package com.secrethitler.ai.processors;

import static com.google.common.base.Predicates.not;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.base.Predicates;
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
import com.secrethitler.ai.utils.RandomUtil;

public class SimpleGameplayProcessor implements GameplayProcessor {
	private static final Logger LOGGER = Logger.getLogger(SimpleGameplayProcessor.class.getName());
	private static final Map<PartyMembership, Policy> PREFERRED_POLICY_TO_DISCARD_MAP = ImmutableMap.<PartyMembership, Policy>builder()
			.put(PartyMembership.LIBERAL, Policy.FASCIST)
			.put(PartyMembership.FASCIST, Policy.LIBERAL)
			.build();
	
	private static final Map<PartyMembership, PartyMembership> OPPOSITE_MEMBERSHIP_MAP = ImmutableMap.<PartyMembership, PartyMembership>builder()
			.put(PartyMembership.FASCIST, PartyMembership.LIBERAL)
			.put(PartyMembership.LIBERAL, PartyMembership.FASCIST)
			.put(PartyMembership.UNKNOWN, PartyMembership.UNKNOWN)
			.build();
	
	protected static Policy getPreferredPolicyToDiscard(final PartyMembership membership) {
		return PREFERRED_POLICY_TO_DISCARD_MAP.get(membership);
	}
	
	protected static PartyMembership getOppositeMembership(final PartyMembership membership) {
		return OPPOSITE_MEMBERSHIP_MAP.get(membership);
	}
	
	protected static final Set<SecretRole> KNOWN_NON_HITLER_ROLES = ImmutableSet.of(SecretRole.FASCIST, SecretRole.LIBERAL);
	
	private final RandomUtil randomUtil;
	private final Map<GamePhase, Function<GameData, Optional<GameplayAction>>> phaseToFunctionMap;
	protected final String username;
	protected boolean vetoUsedThisTurn = false;
	protected boolean chancellorHasChoice;
	
	public SimpleGameplayProcessor(final String username, final RandomUtil randomUtil) {
		this.username = username;
		this.randomUtil = randomUtil;
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
		vetoUsedThisTurn = false;
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
		
		return chooseRunningMate(gameData, eligiblePlayers);
	}
	
	protected PlayerData chooseRunningMate(final GameData gameData, List<PlayerData> eligiblePlayers) {
		final PlayerData myPlayer = gameData.getMyPlayer();
		PartyMembership myMembership = myPlayer.getPartyMembership();
		List<PlayerData> preferredPlayers = getPreferredPlayers(myMembership, eligiblePlayers, myPlayer.getSecretRole(), PartyMembership.FASCIST == myMembership && gameData.isFascistDangerZone());
		return randomUtil.getRandomItemFromList(preferredPlayers);
	}
	
	protected List<PlayerData> getPreferredPlayers(PartyMembership myMembership, List<PlayerData> eligiblePlayers, SecretRole myRole, boolean hitlerPreferred) {
		List<PlayerData> preferredPlayers = getPreferredPlayers(myMembership, eligiblePlayers);
		if (SecretRole.HITLER == myRole) {
			return preferredPlayers;
		}
		List<PlayerData> withHitlerPreference = preferredPlayers.stream()
				.filter(player -> hitlerPreferred ? SecretRole.HITLER == player.getSecretRole() : KNOWN_NON_HITLER_ROLES.contains(player.getSecretRole()))
				.collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(withHitlerPreference)) {
			return withHitlerPreference;
		}
		return getMostLikelyMatchesHitlerPreference(preferredPlayers, hitlerPreferred);
	}
	
	protected List<PlayerData> getPreferredPlayers(PartyMembership myMembership, List<PlayerData> eligiblePlayers) {
		List<PlayerData> playersOnMyTeam = eligiblePlayers.stream()
				.filter(player -> myMembership == player.getPartyMembership())
				.collect(Collectors.toList());
		
		if (CollectionUtils.isNotEmpty(playersOnMyTeam)) {
			return playersOnMyTeam;
		}
		List<PlayerData> unknownPlayers = eligiblePlayers.stream()
				.filter(player -> PartyMembership.UNKNOWN == player.getPartyMembership())
				.collect(Collectors.toList());
		if (unknownPlayers.isEmpty()) {
			return eligiblePlayers;
		}
		return getMostLikelyPartyMembers(unknownPlayers, myMembership);
	}
	
	protected List<PlayerData> getMostLikelyPartyMembers(final List<PlayerData> players, final PartyMembership myMembership) {
		return players;
	}
	
	protected List<PlayerData> getMostLikelyMatchesHitlerPreference(final List<PlayerData> players, final boolean hitlerPreferred) {
		return players;
	}
	
	protected Optional<GameplayAction> vote(final GameData gameData) {
		final PlayerData myPlayer = gameData.getMyPlayer();
		if (myPlayer.isVoteReady() || !myPlayer.isAlive()) {
			return Optional.empty();
		}
		Vote vote = isVoteJa(myPlayer.getPartyMembership(), myPlayer.getSecretRole(), gameData) ? Vote.JA : Vote.NEIN;
		LOGGER.info(() -> String.format("%s is voting %s", username, vote.name()));
		String[] args = {vote.name()};
		return Optional.of(new GameplayAction(Action.VOTE, args));
	}
	
	protected boolean isVoteJa(PartyMembership myMembership, SecretRole myRole, GameData gameData) {
		Stream<PlayerData> governmentStream = gameData.getPlayers().stream()
				.filter(player -> player.isPresident() || player.isChancellor());
		return PartyMembership.FASCIST == myMembership ? 
				governmentStream.anyMatch(player -> ImmutableSet.of(PartyMembership.FASCIST, PartyMembership.UNKNOWN).contains(player.getPartyMembership())) :
					governmentStream.allMatch(player -> ImmutableSet.of(PartyMembership.LIBERAL, PartyMembership.UNKNOWN).contains(player.getPartyMembership()));
	}
	
	protected Optional<GameplayAction> makePresidentChoice(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		Policy preferredPolicyToDiscard = getPreferredPolicyToDiscard(gameData.getMyPlayer().getPartyMembership());
		List<Policy> policies = gameData.getPoliciesToView();
		int preferredIndex = policies.indexOf(preferredPolicyToDiscard);
		int index = preferredIndex == -1 ? 0 : preferredIndex;
		Policy discardedPolicy = policies.get(index);
		recordDiscardedPolicy(discardedPolicy);
		policies.remove(index);
		chancellorHasChoice = policies.get(0) != policies.get(1);
		String[] args = {String.valueOf(index)};
		return Optional.of(new GameplayAction(Action.PRESIDENT_CHOICE, args));
	}
	
	protected Optional<GameplayAction> makeChancellorChoice(final GameData gameData) {
		if (!gameData.getMyPlayer().isChancellor()) {
			return Optional.empty();
		}
		Policy preferredPolicyToDiscard = getPreferredPolicyToDiscard(gameData.getMyPlayer().getPartyMembership());
		if (!vetoUsedThisTurn && gameData.isVetoUnlocked() && gameData.getPoliciesToView().stream().allMatch(policy -> preferredPolicyToDiscard == policy)) {
			vetoUsedThisTurn = true;
			String[] args = {};
			LOGGER.info(() -> String.format("%s is vetoing the policies", username));
			return Optional.of(new GameplayAction(Action.CHANCELLOR_VETO, args));
		}
		List<Policy> policies = gameData.getPoliciesToView();
		int preferredIndex = policies.indexOf(preferredPolicyToDiscard);
		int index = preferredIndex == -1 ? 0 : preferredIndex;
		Policy discardedPolicy = policies.get(index);
		recordDiscardedPolicy(discardedPolicy);
		String[] args = {String.valueOf(index)};
		return Optional.of(new GameplayAction(Action.CHANCELLOR_CHOICE, args));
	}
	
	protected void recordDiscardedPolicy(Policy discardedPolicy) {
		LOGGER.info(() -> String.format("%s is discarding %s policy", username, discardedPolicy.name()));
	}
	
	protected Optional<GameplayAction> examine(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		examinationHelper(gameData);
		String[] args = {};
		LOGGER.info(() -> String.format("%s is examining the top three policies", username));
		return Optional.of(new GameplayAction(Action.FINISH_EXAMINATION, args));
	}
	
	protected void examinationHelper(final GameData gameData) {
		//do nothing for simple gameplay
	}
	
	protected Optional<GameplayAction> kill(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		final PlayerData myPlayer = gameData.getMyPlayer();
		List<PlayerData> allPlayers = gameData.getPlayers();
		List<PlayerData> eligiblePlayers = allPlayers.stream()
				.filter(Predicates.not(myPlayer::equals))
				.filter(PlayerData::isAlive)
				.collect(Collectors.toList());
		PartyMembership myMembership = myPlayer.getPartyMembership();
		List<PlayerData> preferredPlayers = getPreferredPlayers(getOppositeMembership(myMembership), eligiblePlayers, myPlayer.getSecretRole(), PartyMembership.LIBERAL == myMembership);
		PlayerData playerToKill = randomUtil.getRandomItemFromList(preferredPlayers);
		String[] args = {String.valueOf(playerToKill.getUsername())};
		LOGGER.info(() -> String.format("%s is killing %s", username, playerToKill.getUsername()));
		return Optional.of(new GameplayAction(Action.KILL_PLAYER, args));
	}
	
	protected Optional<GameplayAction> presidentVeto(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		boolean concur = presidentVetoHelper(gameData);
		String[] args = {Boolean.toString(concur)};
		LOGGER.info(() -> String.format("%s is agreeing to the veto", username));
		return Optional.of(new GameplayAction(Action.PRESIDENT_VETO, args));
	}
	
	protected boolean presidentVetoHelper(final GameData gameData) {
		Policy preferredPolicyToDiscard = getPreferredPolicyToDiscard(gameData.getMyPlayer().getPartyMembership());
		return gameData.getPoliciesToView().stream().allMatch(policy -> preferredPolicyToDiscard == policy);
	}
	
	protected Optional<GameplayAction> investigate(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		List<PlayerData> availablePlayers = gameData.getPlayers().stream()
				.filter(player -> !gameData.getMyPlayer().equals(player))
				.filter(PlayerData::isAlive)
				.collect(Collectors.toList());
		List<PlayerData> unknownPlayers = availablePlayers.stream()
				.filter(player -> PartyMembership.UNKNOWN == player.getPartyMembership())
				.collect(Collectors.toList());
		List<PlayerData> preferredPlayers;
		if (unknownPlayers.isEmpty()) {
			preferredPlayers = availablePlayers;
		} else {
			preferredPlayers = getMostLikelyPartyMembers(unknownPlayers, PartyMembership.FASCIST);
		}
		PlayerData playerToInvestigate = randomUtil.getRandomItemFromList(preferredPlayers);
		String[] args = {playerToInvestigate.getUsername()};
		return Optional.of(new GameplayAction(Action.INVESTIGATE_PLAYER, args));
	}
	
	protected Optional<GameplayAction> chooseNextPresidentialCandidate(final GameData gameData) {
		if (!gameData.getMyPlayer().isPresident()) {
			return Optional.empty();
		}
		final PlayerData myPlayer = gameData.getMyPlayer();
		List<PlayerData> allPlayers = gameData.getPlayers();
		List<PlayerData> eligiblePlayers = allPlayers.stream()
				.filter(player -> !myPlayer.equals(player))
				.filter(PlayerData::isAlive)
				.collect(Collectors.toList());
		PartyMembership myMembership = myPlayer.getPartyMembership();
		List<PlayerData> preferredPlayers = getPreferredPlayers(myMembership, eligiblePlayers, myPlayer.getSecretRole(), PartyMembership.LIBERAL == myMembership);
		PlayerData nextCandidate = randomUtil.getRandomItemFromList(preferredPlayers);
		String[] args = {Integer.toString(allPlayers.indexOf(nextCandidate))};
		return Optional.of(new GameplayAction(Action.CHOOSE_NEXT_PRESIDENTIAL_CANDIDATE, args));
	}
}
