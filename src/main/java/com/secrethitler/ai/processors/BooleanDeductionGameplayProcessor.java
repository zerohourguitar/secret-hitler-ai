package com.secrethitler.ai.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.SuspicionAction;
import com.secrethitler.ai.utils.RandomUtil;

public class BooleanDeductionGameplayProcessor extends AbstractDeductionGameplayProcessor {
	private static final Logger LOGGER = Logger.getLogger(BooleanDeductionGameplayProcessor.class.getName());
	
	private Map<String, PartyMembership> suspectedMemberships = new HashMap<>();

	public BooleanDeductionGameplayProcessor(final String username, final RandomUtil randomUtil) {
		super(username, randomUtil);
	}
	
	@Override
	protected int getMembershipSuspicion(String username) {
		PartyMembership suspectedMembership = suspectedMemberships.getOrDefault(username, PartyMembership.UNKNOWN);
		return PARTY_MEMBERSHIP_TO_SUSPICION_MAP.get(suspectedMembership);
	}
	
	@Override
	protected void updateSuspectedMembership(final PlayerData player, final int suspicion, final SuspicionAction suspicionAction, final GameData gameData) {
		if (suspicion == 0) {
			return;
		}
		updateSuspectedMembership(player, getPartyMembership(suspicion), suspicionAction, gameData);
	}
	
	@Override
	protected void updateSuspectedMembership(final PlayerData player, final PartyMembership membership, final SuspicionAction suspicionAction, final GameData gameData) {
		if (player.getPartyMembership() != PartyMembership.UNKNOWN && membership != player.getPartyMembership()) {
			throw new IllegalArgumentException("Tried to set a suspected membership of a player that is known to be incorrect!");
		}
		final String suspectUsername = player.getUsername();
		suspectedMemberships.put(suspectUsername, membership);
	}
	
	@Override
	protected void printSuspectedPlayerMatrix(final String action) {
		LOGGER.info(() -> String.format("%s's suspected player matrix after %s: %n%s", username, action, suspectedMemberships.toString()));
	}
	
}
