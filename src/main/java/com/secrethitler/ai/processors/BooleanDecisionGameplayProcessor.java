package com.secrethitler.ai.processors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.ParticipantGameNotification;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.Action;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.SecretRole;

public class BooleanDecisionGameplayProcessor extends SimpleGameplayProcessor {
	private final Map<Action, Consumer<GameData>> actionToDeduceMap;
	
	private Map<String, PartyMembership> suspectedMemberships = new HashMap<>();

	public BooleanDecisionGameplayProcessor(String username) {
		super(username);
		actionToDeduceMap = ImmutableMap.<Action, Consumer<GameData>>builder()
				.put(Action.SHUSH, this::governmentChosenDeducer)
				.put(Action.DENIED, this::governmentDeniedDeducer)
				.put(Action.ANARCHY, this::governmentDeniedDeducer)
				.put(Action.FASCIST_POLICY, this::policyPlayedDeducer)
				.put(Action.LIBERAL_POLICY, this::policyPlayedDeducer)
				.put(Action.KILL_PLAYER, this::playerKilledDeducer)
				.put(Action.CHOOSE_NEXT_PRESIDENTIAL_CANDIDATE, this::specialElectionChosenDeducer)
				.put(Action.PRESIDENT_VETO_YES, this::presidentVetoDeducer)
				.put(Action.PRESIDENT_VETO_NO, this::presidentVetoDeducer)
				.build();
	}
	
	protected PartyMembership getSuspectedMembership(String username) {
		return suspectedMemberships.getOrDefault(username, PartyMembership.UNKNOWN);
	}
	
	protected void setSuspectedMembership(PlayerData player, PartyMembership membership) {
		if (player.getPartyMembership() != PartyMembership.UNKNOWN && membership != player.getPartyMembership()) {
			throw new IllegalArgumentException("Tried to set a suspected membership of a player that is known to be incorrect!");
		}
		suspectedMemberships.put(player.getUsername(), membership);
	}

	@Override
	public Optional<GameplayAction> getActionToTake(final ParticipantGameNotification notification) {
		return super.getActionToTake(notification);
	}
	
	@Override
	protected boolean isVoteJa(Stream<PlayerData> governmentStream, PartyMembership myMembership, SecretRole myRole, int numberOfPlayers) {
		if (SecretRole.LIBERAL == myRole || (SecretRole.HITLER == myRole && numberOfPlayers >= 7)) {
			List<PlayerData> government = governmentStream.collect(Collectors.toList());
			Set<PartyMembership> govtSuspectedMemberships = government.stream()
					.map(player -> getSuspectedMembership(player.getUsername()))
					.filter(membership -> PartyMembership.UNKNOWN != membership)
					.collect(Collectors.toSet());
			PartyMembership suspectedMembership = PartyMembership.UNKNOWN;
			if (govtSuspectedMemberships.size() == 1) {
				suspectedMembership = govtSuspectedMemberships.stream().findAny().get();
				final PartyMembership newSuspectedMembership = suspectedMembership;
				government.forEach(player -> setSuspectedMembership(player, newSuspectedMembership));
			} else if (govtSuspectedMemberships.size() == 2) {
				Set<PartyMembership> knownMemberships = government.stream()
						.map(player -> player.getPartyMembership())
						.filter(membership -> PartyMembership.UNKNOWN != membership)
						.collect(Collectors.toSet());
				if (knownMemberships.size() == 1) {
					PartyMembership knownMembership = knownMemberships.stream().findAny().get();
					suspectedMembership = knownMembership;
					government.forEach(player -> setSuspectedMembership(player, knownMembership));
				} else if (knownMemberships.size() == 2) {
					suspectedMembership = PartyMembership.FASCIST;
				} else {
					government.forEach(player -> setSuspectedMembership(player, PartyMembership.UNKNOWN));
				}
			}
			return Set.of(PartyMembership.UNKNOWN, myMembership).contains(suspectedMembership);
		}
		return governmentStream.anyMatch(player -> PartyMembership.FASCIST == player.getPartyMembership());
	}
	
	protected void governmentChosenDeducer(final GameData gameData) {
		
	}
	
	protected void governmentDeniedDeducer(final GameData gameData) {
		
	}
	
	protected void policyPlayedDeducer(final GameData gameData) {
		
	}
	
	protected void playerKilledDeducer(final GameData gameData) {
		
	}
	
	protected void specialElectionChosenDeducer(final GameData gameData) {
		
	}
	
	protected void presidentVetoDeducer(final GameData gameData) {
		
	}
	
}
