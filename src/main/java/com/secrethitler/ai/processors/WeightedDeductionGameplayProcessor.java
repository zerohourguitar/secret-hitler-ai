package com.secrethitler.ai.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.math3.util.CombinatoricsUtils;

import com.google.common.collect.ImmutableMap;
import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.Policy;
import com.secrethitler.ai.enums.SuspicionAction;
import com.secrethitler.ai.utils.RandomUtil;

public class WeightedDeductionGameplayProcessor extends AbstractDeductionGameplayProcessor {
	private static final Logger LOGGER = Logger.getLogger(WeightedDeductionGameplayProcessor.class.getName());
	
	private static final int GOVERNMENT_DENIED_FACTOR = 1;
	private static final int FAILED_VETO_FACTOR = 1000;
	private static final int FASCIST_POLICY_CHOSEN = -1000;
	private static final int LIBERAL_POLICY_CHOSEN = 200;
	private static final double PRESIDENT_BLAME_SHARE_FOR_DISCARDING_LIBERAL = 0.75;
	private static final int STARTING_FASCIST_POLICIES = 11;
	private static final int STARTING_LIBERAL_POLICIES = 6;
	private static final double VOTE_CHOICE_UNKNOWN_FACTOR = 0.3;
	private static final double KILL_UNKNOWN_FACTOR = 0.8;
	private static final int CHANCELLOR_VETO_FACTOR = 1000;
	private static final int SUCCESSFUL_VETO_WITH_KNOWN_MEMBERSHIP_FACTOR = 1000;
	private static final double TEAMMATE_CHOSEN_UNKNOWN_FACTOR = 0.5;
	private static final int MAX_TEAMMATE_CHOSEN_SUSPICION = 100;
	private static final int MAX_VOTE_SUSPICION = 100;
	
	private static final Map<PartyMembership, Integer> TEAMMATE_CHOSEN_SUSPICION_MAP = ImmutableMap.<PartyMembership, Integer>builder()
			.put(PartyMembership.FASCIST, (int) Math.round(MAX_TEAMMATE_CHOSEN_SUSPICION / TEAMMATE_CHOSEN_UNKNOWN_FACTOR * -1))
			.put(PartyMembership.LIBERAL, (int) Math.round(MAX_TEAMMATE_CHOSEN_SUSPICION / TEAMMATE_CHOSEN_UNKNOWN_FACTOR))
			.build();
	
	private static final Map<SuspicionAction, Map<PartyMembership, Integer>> ACTION_AND_MEMBERSHIP_TO_SUSPICION_MAP = ImmutableMap.<SuspicionAction, Map<PartyMembership, Integer>>builder()
			.put(SuspicionAction.RUNNING_MATE_CHOSEN, TEAMMATE_CHOSEN_SUSPICION_MAP)
			.put(SuspicionAction.PRESIDENTIAL_CANDIDATE_CHOSEN, TEAMMATE_CHOSEN_SUSPICION_MAP)
			.put(SuspicionAction.SUCCESSFUL_VETO, ImmutableMap.<PartyMembership, Integer>builder()
					.put(PartyMembership.FASCIST, SUCCESSFUL_VETO_WITH_KNOWN_MEMBERSHIP_FACTOR * -1)
					.put(PartyMembership.LIBERAL, SUCCESSFUL_VETO_WITH_KNOWN_MEMBERSHIP_FACTOR)
					.build())
			.build();
	
	private static boolean isFascist(final int suspicion) {
		return suspicion < 0;
	}
	
	private static int getSuspicionForPolicyWithOneFascist(final int suspicion, final boolean isPresident) {
		if (isFascist(suspicion)) {
			if (isPresident) {
				return (int) Math.round(FASCIST_POLICY_CHOSEN * PRESIDENT_BLAME_SHARE_FOR_DISCARDING_LIBERAL);
			}
			return FASCIST_POLICY_CHOSEN;
		}
		return LIBERAL_POLICY_CHOSEN / 2;
	}

	private static int getSuspicionForPolicyWithTwoFascists(final int suspicion) {
		if (isFascist(suspicion)) {
			return FASCIST_POLICY_CHOSEN / 2;
		}
		return LIBERAL_POLICY_CHOSEN;
	}
	
	private static long getTotalPolicyCombinations(int suspicion, int fascistPoliciesRemaining, int liberalPoliciesRemaining) {
		int totalPoliciesRemaining = fascistPoliciesRemaining + liberalPoliciesRemaining;
		long totalCombinations = binomialCoefficientHelper(totalPoliciesRemaining, 3);
		if (isFascist(suspicion)) {
			long combinationsWithNoFascists = binomialCoefficientHelper(liberalPoliciesRemaining, 3);
			
			return totalCombinations - combinationsWithNoFascists;
		}
		long combinationsWithNoLiberals = binomialCoefficientHelper(fascistPoliciesRemaining, 3);
		return totalCombinations - combinationsWithNoLiberals;
	}
	
	private static long binomialCoefficientHelper(int n, int k) {
		if (n < k) {
			return 0;
		}
		return CombinatoricsUtils.binomialCoefficient(n, k);
	}
	
	protected static int getNumberOfFascistsFromNumberOfPlayers(int players) {
		return (players - 5) / 2 + 2;
	}
	
	private final Map<SuspicionAction, BiFunction<Integer, GameData, Integer>> suspicionActionToWeightedSuspicionFunctionMap = 
			ImmutableMap.<SuspicionAction, BiFunction<Integer, GameData, Integer>>builder()
			.put(SuspicionAction.GOVERNMENT_DENIED_VOTE, this::governmentDenied)
			.put(SuspicionAction.FAILED_VETO, this::failedVeto)
			.put(SuspicionAction.POLICY_PASSED_PRESIDENT, this::policyPassedPresident)
			.put(SuspicionAction.POLICY_PASSED_CHANCELLOR, this::policyPassedChancellor)
			.put(SuspicionAction.VOTE_CHOICE_RESULT, this::voteChoiceResult)
			.put(SuspicionAction.KILLED_PLAYER, this::killedPlayer)
			.put(SuspicionAction.CHANCELLOR_VETO, this::chancellorVeto)
			.put(SuspicionAction.SUCCESSFUL_VETO, this::succesfulVeto)
			.put(SuspicionAction.RUNNING_MATE_CHOSEN, this::teammateChosen)
			.put(SuspicionAction.PRESIDENTIAL_CANDIDATE_CHOSEN, this::teammateChosen)
			.build();
	
	private final Map<String, Integer> weightedUserSuspicionMap = new HashMap<>();
	protected List<Policy> knownDiscardedPolicies = new ArrayList<>();
	
	public WeightedDeductionGameplayProcessor(String username, RandomUtil randomUtil) {
		super(username, randomUtil);
	}

	@Override
	protected int getMembershipSuspicion(String username) {
		return weightedUserSuspicionMap.getOrDefault(username, 0);
	}

	@Override
	protected void updateSuspectedMembership(PlayerData player, PartyMembership membership,
			SuspicionAction suspicionAction, GameData gameData) {
		final int suspicion = ACTION_AND_MEMBERSHIP_TO_SUSPICION_MAP.getOrDefault(suspicionAction, PARTY_MEMBERSHIP_TO_SUSPICION_MAP).getOrDefault(membership, 0);
		updateSuspectedMembership(player, suspicion, suspicionAction, gameData);
	}

	@Override
	protected void updateSuspectedMembership(PlayerData player, int suspicion, SuspicionAction suspicionAction, GameData gameData) {
		final int startingSuspicion = weightedUserSuspicionMap.getOrDefault(player.getUsername(), 0);
		final int weightedSuspicionChange = suspicionActionToWeightedSuspicionFunctionMap.getOrDefault(suspicionAction, this::defaultWeightedSuspicionFunction).apply(suspicion, gameData);
		weightedUserSuspicionMap.put(player.getUsername(), startingSuspicion + weightedSuspicionChange);
	}

	@Override
	protected void printSuspectedPlayerMatrix(String action) {
		LOGGER.info(() -> String.format("%s's suspected player matrix after %s: %n%s", username, action, weightedUserSuspicionMap.toString()));
	}
	
	@Override
	protected void recordDiscardedPolicy(Policy discardedPolicy) {
		knownDiscardedPolicies.add(discardedPolicy);
		super.recordDiscardedPolicy(discardedPolicy);
	}
	
	@Override
	protected Optional<GameplayAction> pickRunningMate(final GameData gameData) {
		if (gameData.getDeniedPolicies() == 0) {
			knownDiscardedPolicies.clear();
		}
		return super.pickRunningMate(gameData);
	}
	
	@Override
	protected void updateSuspectedMembershipsForGovernment(final List<PlayerData> team,
			final SuspicionAction suspicionAction, final GameData gameData, int govtSuspectedMembership) {
		team.forEach(player -> updateSuspectedMembership(player, govtSuspectedMembership - getMembershipSuspicion(player.getUsername()), suspicionAction, gameData));
	}
	
	@Override
	protected Set<PlayerData> getMostExpectedFascists(GameData gameData) {
		Set<PlayerData> mostSuspectedFascists = gameData.getPlayers().stream()
				.filter(player -> PartyMembership.FASCIST == player.getPartyMembership())
				.collect(Collectors.toSet());
		int totalFascists = getNumberOfFascistsFromNumberOfPlayers(gameData.getPlayers().size());
		List<Set<PlayerData>> playersOrderedBySuspicion = getPlayersOrderedBySuspicion(gameData);
		Iterator<Set<PlayerData>> it = playersOrderedBySuspicion.iterator();
		while(it.hasNext()) {
			Set<PlayerData> players = it.next();
			if (mostSuspectedFascists.size() + players.size() > totalFascists) {
				break;
			}
			mostSuspectedFascists.addAll(players);
		}
		return mostSuspectedFascists;
	}

	protected List<Set<PlayerData>> getPlayersOrderedBySuspicion(GameData gameData) {
		return getPlayersOrderedBySuspicion(gameData.getPlayers());
	}
	
	protected List<Set<PlayerData>> getPlayersOrderedBySuspicion(List<PlayerData> players) {
		return players.stream()
				.collect(Collectors.groupingBy(player -> getMembershipSuspicion(player.getUsername()), Collectors.toSet()))
				.entrySet().stream()
				.sorted((entry1, entry2) -> entry1.getKey().compareTo(entry2.getKey()))
				.map(Entry::getValue)
				.collect(Collectors.toList());
	}
	
	private int defaultWeightedSuspicionFunction(final int suspicion, final GameData gameData) {
		return suspicion;
	}

	private int governmentDenied(final int suspicion, final GameData gameData) {
		return suspicion * GOVERNMENT_DENIED_FACTOR;
	}
	
	private int failedVeto(final int suspicion, final GameData gameData) {
		return suspicion * FAILED_VETO_FACTOR;
	}
	
	private int policyPassedPresident(final int suspicion, final GameData gameData) {
		return policyPassed(suspicion, gameData, true);
	}
	
	private int policyPassedChancellor(final int suspicion, final GameData gameData) {
		// Did the chancellor have a choice?  
		// How many liberal and fascist policies remain
		PlayerData myPlayer = gameData.getMyPlayer();
		if (previousPresident.equals(myPlayer.getUsername())) {
			if (chancellorHasChoice) {
				if (isFascist(suspicion)) {
					return FASCIST_POLICY_CHOSEN;
				}
				return LIBERAL_POLICY_CHOSEN;
			}
			return 0;
		}
		
		return policyPassed(suspicion, gameData, false);
	}

	private int policyPassed(final int suspicion, final GameData gameData, final boolean isPresident) {
		if (CollectionUtils.isNotEmpty(policyOptionsForNextGovernment)) {
			long fascistPolicies = policyOptionsForNextGovernment.stream()
					.filter(policy -> Policy.FASCIST == policy)
					.count();
			if (fascistPolicies == 1) {
				return getSuspicionForPolicyWithOneFascist(suspicion, isPresident);
			}
			return getSuspicionForPolicyWithTwoFascists(suspicion);
		}
		
		int fascistPoliciesDiscarded = (int) knownDiscardedPolicies.stream()
				.filter(policy -> Policy.FASCIST == policy)
				.count();
		int fascistPoliciesUsed = gameData.getFascistPolicies() + fascistPoliciesDiscarded;
		int liberalPoliciesDiscarded = (int) knownDiscardedPolicies.stream()
				.filter(policy -> Policy.LIBERAL == policy)
				.count();
		int liberalPoliciesUsed = gameData.getLiberalPolicies() + liberalPoliciesDiscarded;
		
		int fascistPoliciesRemaining = STARTING_FASCIST_POLICIES - fascistPoliciesUsed;
		int liberalPoliciesRemaining = STARTING_LIBERAL_POLICIES - liberalPoliciesUsed;
		
		long totalCombinations = getTotalPolicyCombinations(suspicion, fascistPoliciesRemaining, liberalPoliciesRemaining);
		
		long combinationsWithOneFascist = binomialCoefficientHelper(fascistPoliciesRemaining, 1) * binomialCoefficientHelper(liberalPoliciesRemaining, 2);
		double chanceOneFascist = ((double) combinationsWithOneFascist) / ((double) totalCombinations);
		long combinationsWithTwoFascists = binomialCoefficientHelper(fascistPoliciesRemaining, 2) * binomialCoefficientHelper(liberalPoliciesRemaining, 1);
		double chanceTwoFascists = ((double) combinationsWithTwoFascists) / ((double) totalCombinations);
		
		int suspicionOfOneFascist = (int) Math.round(getSuspicionForPolicyWithOneFascist(suspicion, isPresident) * chanceOneFascist);
		int suspicionOfTwoFascists = (int) Math.round(getSuspicionForPolicyWithTwoFascists(suspicion) * chanceTwoFascists);
		
		return suspicionOfOneFascist + suspicionOfTwoFascists;
	}

	private int voteChoiceResult(final int suspicion, final GameData gameData) {
		int rawSuspicion = (int) Math.round(suspicion * VOTE_CHOICE_UNKNOWN_FACTOR);
		int changeInSuspicion = Math.min(MAX_VOTE_SUSPICION, Math.abs(rawSuspicion));
		if (rawSuspicion < 0) {
			return changeInSuspicion * -1;
		}
		return changeInSuspicion;
	}
	
	private int killedPlayer(final int suspicion, final GameData gameData) {
		return (int) Math.round(suspicion * KILL_UNKNOWN_FACTOR);
	}
	
	private int chancellorVeto(final int suspicion, final GameData gameData) {
		return suspicion * CHANCELLOR_VETO_FACTOR;
	}
	
	private int succesfulVeto(final int suspicion, final GameData gameData) {
		return suspicion;
	}
	
	private int teammateChosen(final int suspicion, final GameData gameData) {
		int rawSuspicion = (int) Math.round(suspicion * TEAMMATE_CHOSEN_UNKNOWN_FACTOR);
		rawSuspicion = Math.min(MAX_TEAMMATE_CHOSEN_SUSPICION, rawSuspicion);
		return Math.max(MAX_TEAMMATE_CHOSEN_SUSPICION * -1, rawSuspicion);
	}
}
