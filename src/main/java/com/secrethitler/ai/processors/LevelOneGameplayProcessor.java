package com.secrethitler.ai.processors;

import static com.google.common.base.Predicates.not;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private static final Logger LOGGER = Logger.getLogger(LevelOneGameplayProcessor.class.getName());
	private static final Random RANDOM_GENERATOR = new Random();
	protected static final Map<PartyMembership, Policy> PREFERRED_POLICY_TO_DISCARD_MAP = ImmutableMap.<PartyMembership, Policy>builder()
			.put(PartyMembership.LIBERAL, Policy.FASCIST)
			.put(PartyMembership.FASCIST, Policy.LIBERAL)
			.build();
	
	private static <T> T getRandomItemFromList(List<T> list) {
		int index = RANDOM_GENERATOR.nextInt(list.size());
		return list.get(index);
	}
	
	private final Map<GamePhase, Function<GameData, Optional<GameplayAction>>> phaseToFunctionMap;
	private final String username;
	private boolean hasVetoed = false;
	
	public LevelOneGameplayProcessor(final String username) {
		this.username = username;
		phaseToFunctionMap = ImmutableMap.<GamePhase, Function<GameData, Optional<GameplayAction>>>builder()
				.put(GamePhase.PICKING_RUNNING_MATE, this::pickRunningMate)
				.put(GamePhase.ELECTION, this::vote)
				.put(GamePhase.PRESIDENT_CHOICE, this::makePresidentChoice)
				.put(GamePhase.CHANCELLOR_CHOICE, this::makeChancellorChoice)
				.put(GamePhase.EXAMINE, this::examine)
				.put(GamePhase.KILL, this::kill)
				.put(GamePhase.VETO, this::presidentVeto)
				.put(GamePhase.INVESTIGATE, this::investigate)
				.put(GamePhase.SPECIAL_ELECTION, this::chooseNextPresidentialCandidate)
				.build();
	}

	@Override
	public Optional<GameplayAction> getActionToTake(final ParticipantGameNotification notification) {
		final GameData gameData = notification.getGameData();
		return phaseToFunctionMap.getOrDefault(gameData.getPhase(), data -> Optional.empty()).apply(gameData);
	}

	protected Optional<GameplayAction> pickRunningMate(final GameData gameData) {
		hasVetoed = false;
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		PlayerData runningMate = chooseRunningMate(gameData);
		LOGGER.info(() -> String.format("%s is picking %s as running mate.", username, runningMate.getUsername()));
		final int runningMateIndex = gameData.getPlayers().indexOf(runningMate);
		String[] args = {String.valueOf(runningMateIndex)};
		return Optional.of(new GameplayAction(Action.CHOOSE_RUNNING_MATE, args));		
	}

	protected PlayerData chooseRunningMate(final GameData gameData) {
		final PlayerData myPlayer = gameData.getMyPlayer();
		List<PlayerData> eligiblePlayers = gameData.getPlayers().stream()
				.filter(not(myPlayer::equals))
				.filter(not(PlayerData::isPreviousGovernmentMember))
				.filter(PlayerData::isAlive)
				.collect(Collectors.toList());
		
		if (SecretRole.FASCIST == myPlayer.getSecretRole()) {
			Optional<PlayerData> hitler = eligiblePlayers.stream()
					.filter(player -> SecretRole.HITLER == player.getSecretRole())
					.findAny();
			if (hitler.isPresent()) {
				return hitler.get();
			}
		}
		
		List<PlayerData> preferredPlayers = eligiblePlayers.stream()
				.filter(player -> myPlayer.getPartyMembership() == player.getPartyMembership())
				.collect(Collectors.toList());
		
		if (preferredPlayers.isEmpty()) {
			preferredPlayers = eligiblePlayers;
		}
		return getRandomItemFromList(preferredPlayers);
	}
	
	protected Optional<GameplayAction> vote(final GameData gameData) {
		final PlayerData myPlayer = gameData.getMyPlayer();
		if (myPlayer.isVoteReady() || !myPlayer.isAlive()) {
			return Optional.empty();
		}
		Stream<PlayerData> governmentStream = gameData.getPlayers().stream()
				.filter(player -> player.isPresident() || player.isChancellor());
		Vote vote = isVoteJa(governmentStream, myPlayer.getPartyMembership()) ? Vote.JA : Vote.NEIN;
		LOGGER.info(() -> String.format("%s is voting %s", username, vote.name()));
		String[] args = {vote.name()};
		return Optional.of(new GameplayAction(Action.VOTE, args));
	}
	
	protected boolean isVoteJa(Stream<PlayerData> governmentStream, PartyMembership myMembership) {
		return PartyMembership.FASCIST == myMembership ? 
				governmentStream.anyMatch(player -> ImmutableSet.of(PartyMembership.FASCIST, PartyMembership.UNKNOWN).contains(player.getPartyMembership())) :
					governmentStream.allMatch(player -> ImmutableSet.of(PartyMembership.LIBERAL, PartyMembership.UNKNOWN).contains(player.getPartyMembership()));
	}
	
	protected Optional<GameplayAction> makePresidentChoice(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		Policy preferredPolicyToDiscard = PREFERRED_POLICY_TO_DISCARD_MAP.get(gameData.getMyPlayer().getPartyMembership());
		List<Policy> policies = gameData.getPoliciesToView();
		int preferredIndex = policies.indexOf(preferredPolicyToDiscard);
		int index = preferredIndex == -1 ? 0 : preferredIndex;
		LOGGER.info(() -> String.format("%s is discarding %s policy", username, policies.get(index).name()));
		String[] args = {String.valueOf(index)};
		return Optional.of(new GameplayAction(Action.PRESIDENT_CHOICE, args));
	}
	
	protected Optional<GameplayAction> makeChancellorChoice(final GameData gameData) {
		if (!gameData.getMyPlayer().isChancellor()) {
			return Optional.empty();
		}
		Policy preferredPolicyToDiscard = PREFERRED_POLICY_TO_DISCARD_MAP.get(gameData.getMyPlayer().getPartyMembership());
		if (!hasVetoed && gameData.isVetoUnlocked() && gameData.getPoliciesToView().stream().allMatch(policy -> preferredPolicyToDiscard == policy)) {
			hasVetoed = true;
			String[] args = {};
			LOGGER.info(() -> String.format("%s is vetoing the policies", username));
			return Optional.of(new GameplayAction(Action.CHANCELLOR_VETO, args));
		}
		List<Policy> policies = gameData.getPoliciesToView();
		int preferredIndex = policies.indexOf(preferredPolicyToDiscard);
		int index = preferredIndex == -1 ? 0 : preferredIndex;
		LOGGER.info(() -> String.format("%s is discarding %s policy", username, policies.get(index).name()));
		String[] args = {String.valueOf(index)};
		return Optional.of(new GameplayAction(Action.CHANCELLOR_CHOICE, args));
	}
	
	protected Optional<GameplayAction> examine(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		String[] args = {};
		LOGGER.info(() -> String.format("%s is examining the top three policies", username));
		return Optional.of(new GameplayAction(Action.FINISH_EXAMINATION, args));
	}
	
	protected Optional<GameplayAction> kill(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		final PlayerData myPlayer = gameData.getMyPlayer();
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
		PlayerData playerToKill = getRandomItemFromList(preferredPlayers);
		String[] args = {String.valueOf(playerToKill.getUsername())};
		LOGGER.info(() -> String.format("%s is killing %s", username, playerToKill.getUsername()));
		return Optional.of(new GameplayAction(Action.KILL_PLAYER, args));
	}
	
	protected Optional<GameplayAction> presidentVeto(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		Policy preferredPolicyToDiscard = PREFERRED_POLICY_TO_DISCARD_MAP.get(gameData.getMyPlayer().getPartyMembership());
		boolean concur = gameData.getPoliciesToView().stream().allMatch(policy -> preferredPolicyToDiscard == policy);
		String[] args = {Boolean.toString(concur)};
		LOGGER.info(() -> String.format("%s is agreeing to the veto", username));
		return Optional.of(new GameplayAction(Action.PRESIDENT_VETO, args));
	}
	
	protected Optional<GameplayAction> investigate(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		List<PlayerData> availablePlayers = gameData.getPlayers().stream()
				.filter(player -> !gameData.getMyPlayer().equals(player))
				.filter(PlayerData::isAlive)
				.collect(Collectors.toList());
		List<PlayerData> preferredPlayers = availablePlayers.stream()
				.filter(player -> PartyMembership.UNKNOWN != player.getPartyMembership())
				.collect(Collectors.toList());
		if (preferredPlayers.isEmpty()) {
			preferredPlayers = availablePlayers;
		}
		PlayerData playerToInvestigate = getRandomItemFromList(preferredPlayers);
		String[] args = {playerToInvestigate.getUsername()};
		return Optional.of(new GameplayAction(Action.INVESTIGATE_PLAYER, args));
	}
	
	protected Optional<GameplayAction> chooseNextPresidentialCandidate(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		final PlayerData myPlayer = gameData.getMyPlayer();
		List<PlayerData> allPlayers = gameData.getPlayers();
		List<PlayerData> availablePlayers = allPlayers.stream()
				.filter(player -> !myPlayer.equals(player))
				.filter(PlayerData::isAlive)
				.collect(Collectors.toList());
		List<PlayerData> preferredPlayers = availablePlayers.stream()
				.filter(player -> myPlayer.getPartyMembership() == player.getPartyMembership())
				.collect(Collectors.toList());
		if (preferredPlayers.isEmpty()) {
			preferredPlayers = availablePlayers;
		}
		PlayerData nextCandidate = getRandomItemFromList(preferredPlayers);
		String[] args = {Integer.toString(allPlayers.indexOf(nextCandidate))};
		return Optional.of(new GameplayAction(Action.CHOOSE_NEXT_PRESIDENTIAL_CANDIDATE, args));
	}
}
