package com.secrethitler.ai.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Logger;

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
		long totalCombinations = CombinatoricsUtils.binomialCoefficient(totalPoliciesRemaining, 3);
		if (isFascist(suspicion)) {
			long combinationsWithNoFascists = CombinatoricsUtils.binomialCoefficient(liberalPoliciesRemaining, 3);
			
			return totalCombinations - combinationsWithNoFascists;
		}
		long combinationsWithNoLiberals = CombinatoricsUtils.binomialCoefficient(fascistPoliciesRemaining, 3);
		return totalCombinations - combinationsWithNoLiberals;
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
		final int suspicion = PARTY_MEMBERSHIP_TO_SUSPICION_MAP.get(membership);
		updateSuspectedMembership(player, suspicion, suspicionAction, gameData);
	}

	@Override
	protected void updateSuspectedMembership(PlayerData player, int suspicion, SuspicionAction suspicionAction, GameData gameData) {
		final int startingSuspicion = weightedUserSuspicionMap.getOrDefault(username, 0);
		final int weightedSuspicionChange = suspicionActionToWeightedSuspicionFunctionMap.get(suspicionAction).apply(suspicion, gameData);
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
		
		long combinationsWithOneFascist = CombinatoricsUtils.binomialCoefficient(fascistPoliciesRemaining, 1) * CombinatoricsUtils.binomialCoefficient(liberalPoliciesRemaining, 2);
		double chanceOneFascist = combinationsWithOneFascist / totalCombinations;
		long combinationsWithTwoFascists = CombinatoricsUtils.binomialCoefficient(fascistPoliciesRemaining, 2) * CombinatoricsUtils.binomialCoefficient(liberalPoliciesRemaining, 1);
		double chanceTwoFascists = combinationsWithTwoFascists / totalCombinations;
		
		int suspicionOfOneFascist = (int) Math.round(getSuspicionForPolicyWithOneFascist(suspicion, isPresident) * chanceOneFascist);
		int suspicionOfTwoFascists = (int) Math.round(getSuspicionForPolicyWithTwoFascists(suspicion) * chanceTwoFascists);
		
		return suspicionOfOneFascist + suspicionOfTwoFascists;
	}

	private int voteChoiceResult(final int suspicion, final GameData gameData) {
		return (int) Math.round(suspicion * VOTE_CHOICE_UNKNOWN_FACTOR);
	}
	
	private int killedPlayer(final int suspicion, final GameData gameData) {
		return (int) Math.round(suspicion * KILL_UNKNOWN_FACTOR);
	}
	
	private int chancellorVeto(final int suspicion, final GameData gameData) {
		return suspicion * CHANCELLOR_VETO_FACTOR;
	}
	
	private int succesfulVeto(final int suspicion, final GameData gameData) {
		Optional<PartyMembership> knownMembership = gameData.getPlayers().stream()
				.filter(player -> previousPresident.equals(player.getUsername()) || previousChancellor.equals(player.getUsername()))
				.map(PlayerData::getPartyMembership)
				.filter(membership -> PartyMembership.UNKNOWN != membership)
				.findAny();
		if (knownMembership.isPresent()) {
			return PARTY_MEMBERSHIP_TO_SUSPICION_MAP.get(knownMembership.get()) * SUCCESSFUL_VETO_WITH_KNOWN_MEMBERSHIP_FACTOR;
		}
		return suspicion;
	}
	
	private int teammateChosen(final int suspicion, final GameData gameData) {
		return (int) Math.round(suspicion * TEAMMATE_CHOSEN_UNKNOWN_FACTOR);
	}
}
