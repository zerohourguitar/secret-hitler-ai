package com.secrethitler.ai.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.SuspicionAction;
import com.secrethitler.ai.utils.RandomUtil;

public class WeightedDeductionGameplayProcessor extends AbstractDeductionGameplayProcessor {
	private static final Logger LOGGER = Logger.getLogger(WeightedDeductionGameplayProcessor.class.getName());
	
	private static final int GOVERNMENT_DENIED_FACTOR = 1;
	private static final int FAILED_VETO_FACTOR = 1000;
	
	private final Map<SuspicionAction, BiFunction<Integer, GameData, Integer>> suspicionActionToWeightedSuspicionFunctionMap = 
			ImmutableMap.<SuspicionAction, BiFunction<Integer, GameData, Integer>>builder()
			.put(SuspicionAction.GOVERNMENT_DENIED_VOTE, this::governmentDenied)
			.put(SuspicionAction.FAILED_VETO, this::failedVeto)
			.put(SuspicionAction.VOTE_CHOICE_RESULT, this::voteChoiceResult)
			.put(SuspicionAction.KILLED_PLAYER, this::killedPlayer)
			.put(SuspicionAction.CHANCELLOR_VETO, this::chancellorVeto)
			.put(SuspicionAction.SUCCESSFUL_VETO, this::succesfulVeto)
			.put(SuspicionAction.RUNNING_MATE_CHOSEN, this::runningMateChosen)
			.put(SuspicionAction.PRESIDENTIAL_CANDIDATE_CHOSEN, this::presidentialCandidateChosen)
			.build();
	
	private final Map<String, Integer> weightedUserSuspicionMap = new HashMap<>();
	
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

	private int governmentDenied(final int suspicion, final GameData gameData) {
		return suspicion * GOVERNMENT_DENIED_FACTOR;
	}
	
	private int failedVeto(final int suspicion, final GameData gameData) {
		return suspicion * FAILED_VETO_FACTOR;
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
