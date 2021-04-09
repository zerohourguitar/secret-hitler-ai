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
			.put(SuspicionAction.RUNNING_MATE_CHOSEN, this::runningMateChosen)
			.put(SuspicionAction.PRESIDENTIAL_CANDIDATE_CHOSEN, this::presidentialCandidateChosen)
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
		if (CollectionUtils.isNotEmpty(policyOptionsForNextGovernment)) {
			long fascistPolicies = policyOptionsForNextGovernment.stream()
					.filter(policy -> Policy.FASCIST == policy)
					.count();
			if (fascistPolicies == 1) {
				if (suspicion == -1) {
					return (int) Math.round(FASCIST_POLICY_CHOSEN * 0.75);
				} else {
					return LIBERAL_POLICY_CHOSEN / 2;
				}
			} else {
				if (suspicion == -1) {
					return FASCIST_POLICY_CHOSEN / 2;
				} else {
					return LIBERAL_POLICY_CHOSEN;
				}
			}
		}
		return policyPassed(suspicion, gameData);
	}
	
	private int policyPassedChancellor(final int suspicion, final GameData gameData) {
		// Did the chancellor have a choice?  
		// How many liberal and fascist policies remain
		PlayerData myPlayer = gameData.getMyPlayer();
		if (previousPresident.equals(myPlayer.getUsername())) {
			if (chancellorHasChoice) {
				if (suspicion == -1) {
					return FASCIST_POLICY_CHOSEN;
				} else {
					return LIBERAL_POLICY_CHOSEN;
				}
			} else {
				return 0;
			}
		}
		if (CollectionUtils.isNotEmpty(policyOptionsForNextGovernment)) {
			long fascistPolicies = policyOptionsForNextGovernment.stream()
					.filter(policy -> Policy.FASCIST == policy)
					.count();
			if (fascistPolicies == 1) {
				if (suspicion == -1) {
					return FASCIST_POLICY_CHOSEN;
				} else {
					return LIBERAL_POLICY_CHOSEN / 2;
				}
			} else {
				if (suspicion == -1) {
					return FASCIST_POLICY_CHOSEN / 2;
				} else {
					return LIBERAL_POLICY_CHOSEN;
				}
			}
		}
		return policyPassed(suspicion, gameData);
	}

	private int policyPassed(final int suspicion, final GameData gameData) {
		int fascistPoliciesDiscarded = (int) knownDiscardedPolicies.stream()
				.filter(policy -> Policy.FASCIST == policy)
				.count();
		int fascistPoliciesUsed = gameData.getFascistPolicies() + fascistPoliciesDiscarded;
		int liberalPoliciesDiscarded = (int) knownDiscardedPolicies.stream()
				.filter(policy -> Policy.LIBERAL == policy)
				.count();
		int liberalPoliciesUsed = gameData.getLiberalPolicies() + liberalPoliciesDiscarded;
		int fascistPoliciesRemaining = 11 - fascistPoliciesUsed;
		int liberalPoliciesRemaining = 6 - liberalPoliciesUsed;
		int totalPoliciesRemaining = fascistPoliciesRemaining + liberalPoliciesRemaining;
		long totalCombinations = CombinatoricsUtils.binomialCoefficient(totalPoliciesRemaining, 3);
		long combinationsWithNoLiberals = CombinatoricsUtils.binomialCoefficient(fascistPoliciesRemaining, 3);
		double chanceNoLiberalChoice = combinationsWithNoLiberals / totalCombinations;
		double chanceLiberalExists = 1 - chanceNoLiberalChoice;
		long combinationsWithNoFascists = CombinatoricsUtils.binomialCoefficient(liberalPoliciesRemaining, 3);
		double chanceNoFascistChoice = combinationsWithNoFascists / totalCombinations;
		double chanceFascistExists = 1 - chanceNoFascistChoice;
		if (suspicion == -1) {
			return (int) Math.round(FASCIST_POLICY_CHOSEN * chanceLiberalExists / 2);
		} else {
			return (int) Math.round(LIBERAL_POLICY_CHOSEN * chanceFascistExists / 2);
		}
	}
	
	private int voteChoiceResult(final int suspicion, final GameData gameData) {
		return suspicion;
	}
	
	private int killedPlayer(final int suspicion, final GameData gameData) {
		return suspicion;
	}
	
	private int chancellorVeto(final int suspicion, final GameData gameData) {
		return suspicion;
	}
	
	private int succesfulVeto(final int suspicion, final GameData gameData) {
		return suspicion;
	}
	
	private int runningMateChosen(final int suspicion, final GameData gameData) {
		return suspicion;
	}
	
	private int presidentialCandidateChosen(final int suspicion, final GameData gameData) {
		return suspicion;
	}
}
