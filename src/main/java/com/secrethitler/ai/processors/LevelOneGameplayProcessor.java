package com.secrethitler.ai.processors;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.ParticipantGameNotification;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.Action;
import com.secrethitler.ai.enums.GamePhase;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.Policy;
import com.secrethitler.ai.enums.SecretRole;
import com.secrethitler.ai.enums.Vote;

public class LevelOneGameplayProcessor implements GameplayProcessor {
	private static final Random RANDOM_GENERATOR = new Random();
	protected static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writer().withDefaultPrettyPrinter();
	protected static final Map<PartyMembership, Policy> PREFERRED_POLICY_TO_DISCARD_MAP = ImmutableMap.<PartyMembership, Policy>builder()
			.put(PartyMembership.LIBERAL, Policy.FASCIST)
			.put(PartyMembership.FASCIST, Policy.LIBERAL)
			.build();
	
	private static <T> T getRandomItemFromList(List<T> list) {
		int index = RANDOM_GENERATOR.nextInt(list.size());
		return list.get(index);
	}

	private Map<GamePhase, Function<GameData, Optional<GameplayAction>>> phaseToFunctionMap;
	private boolean hasVoted = false;
	private boolean hasVetoed = false;
	
	public LevelOneGameplayProcessor() {	
		phaseToFunctionMap = ImmutableMap.<GamePhase, Function<GameData, Optional<GameplayAction>>>builder()
				.put(GamePhase.PICKING_RUNNING_MATE, this::pickRunningMate)
				.put(GamePhase.ELECTION, this::vote)
				.put(GamePhase.PRESIDENT_CHOICE, this::makePresidentChoice)
				.put(GamePhase.CHANCELLOR_CHOICE, this::makeChancellorChoice)
				.put(GamePhase.EXAMINE, this::examine)
				.put(GamePhase.KILL, this::kill)
				.put(GamePhase.VETO, this::presidentVeto)
				.build();
	}

	@Override
	public Optional<String> getMessageToSend(ParticipantGameNotification notification) {
		GameData gameData = notification.getGameData();
		Function<GameData, Optional<GameplayAction>> function = phaseToFunctionMap.getOrDefault(gameData.getPhase(), data -> Optional.empty());
		return function.apply(gameData).map(action -> {
			try {
				return OBJECT_WRITER.writeValueAsString(action);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return null;
		});
	}

	protected Optional<GameplayAction> pickRunningMate(GameData gameData) {
		hasVoted = false;
		hasVetoed = false;
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		int runningMateIndex = chooseRunningMateIndex(gameData);
		String[] args = {String.valueOf(runningMateIndex)};
		return Optional.of(new GameplayAction(Action.CHOOSE_RUNNING_MATE, args));		
	}

	protected int chooseRunningMateIndex(GameData gameData) {
		List<PlayerData> allPlayers = gameData.getPlayers();
		PlayerData myPlayer = gameData.getMyPlayer();
		List<PlayerData> eligiblePlayers = allPlayers.stream()
				.filter(player -> !myPlayer.equals(player))
				.filter(player -> !player.isPreviousGovernmentMember())
				.filter(PlayerData::isAlive)
				.collect(Collectors.toList());
		
		if (SecretRole.FASCIST == myPlayer.getSecretRole()) {
			Optional<PlayerData> hitler = eligiblePlayers.stream()
					.filter(player -> SecretRole.HITLER == player.getSecretRole())
					.findAny();
			if (hitler.isPresent()) {
				return allPlayers.indexOf(hitler.get());
			}
		}
		
		List<PlayerData> preferredPlayers = eligiblePlayers.stream()
				.filter(player -> myPlayer.getPartyMembership() == player.getPartyMembership())
				.collect(Collectors.toList());
		
		if (preferredPlayers.isEmpty()) {
			preferredPlayers = eligiblePlayers;
		}
		PlayerData player = getRandomItemFromList(preferredPlayers);
		return allPlayers.indexOf(player);
	}
	
	protected Optional<GameplayAction> vote(GameData gameData) {
		if (hasVoted) {
			return Optional.empty();
		}
		PlayerData myPlayer = gameData.getMyPlayer();
		if (myPlayer.isVoteReady() || !myPlayer.isAlive()) {
			return Optional.empty();
		}
		Stream<PlayerData> governmentStream = gameData.getPlayers().stream()
				.filter(player -> player.isPresident() || player.isChancellor());
		Vote vote = isVoteJa(governmentStream, myPlayer.getPartyMembership()) ? Vote.JA : Vote.NEIN;
		String[] args = {vote.name()};
		hasVoted = true;
		return Optional.of(new GameplayAction(Action.VOTE, args));
	}
	
	protected boolean isVoteJa(Stream<PlayerData> governmentStream, PartyMembership myMembership) {
		return PartyMembership.FASCIST == myMembership ? 
				governmentStream.anyMatch(player -> ImmutableSet.of(PartyMembership.FASCIST, PartyMembership.UNKNOWN).contains(player.getPartyMembership())) :
					governmentStream.allMatch(player -> ImmutableSet.of(PartyMembership.LIBERAL, PartyMembership.UNKNOWN).contains(player.getPartyMembership()));
	}
	
	protected Optional<GameplayAction> makePresidentChoice(GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		Policy preferredPolicyToDiscard = PREFERRED_POLICY_TO_DISCARD_MAP.get(gameData.getMyPlayer().getPartyMembership());
		int index = gameData.getPoliciesToView().indexOf(preferredPolicyToDiscard);
		String args[] = {String.valueOf(index == -1 ? 0 : index)};
		return Optional.of(new GameplayAction(Action.PRESIDENT_CHOICE, args));
	}
	
	protected Optional<GameplayAction> makeChancellorChoice(GameData gameData) {
		if (!gameData.getMyPlayer().isChancellor()) {
			return Optional.empty();
		}
		Policy preferredPolicyToDiscard = PREFERRED_POLICY_TO_DISCARD_MAP.get(gameData.getMyPlayer().getPartyMembership());
		if (!hasVetoed && gameData.isVetoUnlocked() && gameData.getPoliciesToView().stream().allMatch(policy -> preferredPolicyToDiscard == policy)) {
			hasVetoed = true;
			String[] args = {};
			return Optional.of(new GameplayAction(Action.CHANCELLOR_VETO, args));
		}
		int index = gameData.getPoliciesToView().indexOf(preferredPolicyToDiscard);
		String args[] = {String.valueOf(index == -1 ? 0 : index)};
		return Optional.of(new GameplayAction(Action.CHANCELLOR_CHOICE, args));
	}
	
	protected Optional<GameplayAction> examine(GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		String[] args = {};
		return Optional.of(new GameplayAction(Action.FINISH_EXAMINATION, args));
	}
	
	protected Optional<GameplayAction> kill(GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		PlayerData myPlayer = gameData.getMyPlayer();
		List<PlayerData> allPlayers = gameData.getPlayers();
		List<PlayerData> eligiblePlayers = allPlayers.stream()
				.filter(player -> !myPlayer.equals(player))
				.filter(PlayerData::isAlive)
				.collect(Collectors.toList());
		List<PlayerData> preferredPlayers = eligiblePlayers.stream()
				.filter(player -> PartyMembership.UNKNOWN != player.getPartyMembership() && myPlayer.getPartyMembership() != player.getPartyMembership())
				.collect(Collectors.toList());
		if (preferredPlayers.isEmpty()) {
			preferredPlayers = eligiblePlayers;
		}
		PlayerData player = getRandomItemFromList(preferredPlayers);
		String[] args = {String.valueOf(player.getUsername())};
		return Optional.of(new GameplayAction(Action.KILL_PLAYER, args));
	}
	
	protected Optional<GameplayAction> presidentVeto(GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		Policy preferredPolicyToDiscard = PREFERRED_POLICY_TO_DISCARD_MAP.get(gameData.getMyPlayer().getPartyMembership());
		boolean concur = gameData.getPoliciesToView().stream().allMatch(policy -> preferredPolicyToDiscard == policy);
		String[] args = {Boolean.toString(concur)};
		return Optional.of(new GameplayAction(Action.PRESIDENT_VETO, args));
	}
}
