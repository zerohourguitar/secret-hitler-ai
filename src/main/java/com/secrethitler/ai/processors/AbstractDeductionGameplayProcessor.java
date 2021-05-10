package com.secrethitler.ai.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.ImmutableMap;
import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.ParticipantGameNotification;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.Action;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.Policy;
import com.secrethitler.ai.enums.SecretRole;
import com.secrethitler.ai.enums.SuspicionAction;
import com.secrethitler.ai.enums.Vote;
import com.secrethitler.ai.utils.RandomUtil;

public abstract class AbstractDeductionGameplayProcessor extends SimpleGameplayProcessor {
	private static final Map<Policy, Policy> OPPOSITE_POLICY_MAP = ImmutableMap.<Policy, Policy>builder()
			.put(Policy.FASCIST, Policy.LIBERAL)
			.put(Policy.LIBERAL, Policy.FASCIST)
			.build();
	
	private static final Map<Policy, PartyMembership> POLICY_TO_MEMBERSHIP_MAP = ImmutableMap.<Policy, PartyMembership>builder()
			.put(Policy.FASCIST, PartyMembership.FASCIST)
			.put(Policy.LIBERAL, PartyMembership.LIBERAL)
			.build();
	
	protected static Policy getOppositePolicy(final Policy policy) {
		return OPPOSITE_POLICY_MAP.get(policy);
	}
	
	protected static PartyMembership getSuspectedMembershipFromPolicy(final Policy policy) {
		return POLICY_TO_MEMBERSHIP_MAP.get(policy);
	}
	
	protected static PlayerData getPlayerByUsername(final List<PlayerData> players, final String username) {
		return players.stream()
				.filter(player -> username.equals(player.getUsername()))
				.findAny()
				.orElseThrow(IllegalArgumentException::new);
	}
	
	protected static PartyMembership getPartyMembership(final int suspicion) {
		if (suspicion == 0) {
			return PartyMembership.UNKNOWN;
		} else if (suspicion > 0) {
			return PartyMembership.LIBERAL;
		}
		return PartyMembership.FASCIST;
	}
	
	protected static final Map<PartyMembership, Integer> PARTY_MEMBERSHIP_TO_SUSPICION_MAP = ImmutableMap.<PartyMembership, Integer>builder()
			.put(PartyMembership.FASCIST, -1)
			.put(PartyMembership.UNKNOWN, 0)
			.put(PartyMembership.LIBERAL, 1)
			.build();
	
	private static int symetricalRound(double dVal) {
		int iVal = (int) Math.round(Math.abs(dVal));
		if (dVal < 0) {
			return iVal * -1;
		}
		return iVal;
	}
	
	private static final int MAX_SUSPICION = 1000000;
	
	private final Map<Action, Consumer<ParticipantGameNotification>> actionToDeduceMap;
	
	protected Set<String> provenNonHitlers = new HashSet<>();
	protected List<Policy> policyOptionsForNextGovernment = new ArrayList<>();
	protected Optional<String> vetoRequestor = Optional.empty();
	protected String previousPresident = "";
	protected String previousChancellor = "";

	public AbstractDeductionGameplayProcessor(final String username, final RandomUtil randomUtil) {
		super(username, randomUtil);
		actionToDeduceMap = ImmutableMap.<Action, Consumer<ParticipantGameNotification>>builder()
				.put(Action.SHUSH, this::governmentElectedDeducer)
				.put(Action.DENIED, this::governmentDeniedDeducer)
				.put(Action.ANARCHY, this::anarchyDeducer)
				.put(Action.FASCIST_POLICY, this::fascistPolicyDeducer)
				.put(Action.LIBERAL_POLICY, this::liberalPolicyDeducer)
				.put(Action.KILL_PLAYER, this::playerKilledDeducer)
				.put(Action.CHOOSE_NEXT_PRESIDENTIAL_CANDIDATE, this::specialElectionChosenDeducer)
				.put(Action.CHANCELLOR_VETO, this::chancellorVetoDeducer)
				.put(Action.PRESIDENT_VETO_YES, this::presidentVetoDeducer)
				.build();
	}

	@Override
	public Optional<GameplayAction> getActionToTake(final ParticipantGameNotification notification) {
		GameData gameData = notification.getGameData();
		actionToDeduceMap.getOrDefault(notification.getAction().getAction(), data -> {}).accept(notification);
		return super.getActionToTake(notification);
	}
	
	@Override
	protected List<PlayerData> getMostLikelyMatchesHitlerPreference(List<PlayerData> players, boolean hitlerPreferred) {
		List<PlayerData> withHitlerPreference = players.stream()
				.filter(player -> hitlerPreferred != provenNonHitlers.contains(player.getUsername()))
				.collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(withHitlerPreference)) {
			return withHitlerPreference;
		}
		return players;
	}
	
	@Override
	protected List<PlayerData> getMostLikelyPartyMembers(final List<PlayerData> players, final PartyMembership membership) {
		return players.stream()
				.collect(Collectors.groupingBy(player -> getMembershipSuspicion(player.getUsername())))
				.entrySet().stream()
				.sorted((entry1, entry2) -> PartyMembership.FASCIST == membership ?
						entry1.getKey().compareTo(entry2.getKey()) :
						entry2.getKey().compareTo(entry1.getKey()))
				.map(Entry::getValue)
				.findFirst()
				.orElse(Collections.emptyList());
	}
	
	@Override
	protected boolean isVoteJa(PartyMembership myMembership, SecretRole myRole, GameData gameData) {
		PlayerData president = gameData.getPlayers().stream()
				.filter(PlayerData::isPresident)
				.findAny()
				.orElseThrow(IllegalStateException::new);
		PlayerData chancellor = gameData.getPlayers().stream()
				.filter(PlayerData::isChancellor)
				.findAny()
				.orElseThrow(IllegalStateException::new);
		previousPresident = president.getUsername();
		previousChancellor = chancellor.getUsername();
		updateSuspectedMembershipForChosenTeam(president, chancellor, SuspicionAction.RUNNING_MATE_CHOSEN, gameData);
		printSuspectedPlayerMatrix("running mate was chosen");
		Set<PlayerData> mostSuspectedFascists = getMostExpectedFascists(gameData);
		boolean suspectedFascistInGovernment = Stream.of(president, chancellor).anyMatch(mostSuspectedFascists::contains);
		return PartyMembership.FASCIST == myMembership ? suspectedFascistInGovernment : !suspectedFascistInGovernment;
	}

	protected abstract Set<PlayerData> getMostExpectedFascists(GameData gameData);

	private void updateSuspectedMembershipForChosenTeam(final PlayerData president, final PlayerData chancellor, final SuspicionAction suspicionAction, final GameData gameData) {
		if (username.equals(president.getUsername())) {
			return;
		}
		List<PlayerData> team = Arrays.asList(president, chancellor);
		Set<PartyMembership> knownMemberships = team.stream()
				.map(PlayerData::getPartyMembership)
				.filter(membership -> PartyMembership.UNKNOWN != membership)
				.collect(Collectors.toSet());
		if (knownMemberships.size() == 1) {
			PartyMembership knownMembership = knownMemberships.stream().findAny().orElse(null);
			team.forEach(player -> updateSuspectedMembership(player, knownMembership, suspicionAction, gameData));
		} else if (knownMemberships.isEmpty()) {
			int govtSuspectedMembership = symetricalRound(team.stream()
					.mapToInt(player -> getMembershipSuspicion(player.getUsername()))
					.sum() / 2d);
			updateSuspectedMembershipsForGovernment(team, suspicionAction, gameData, govtSuspectedMembership);
		}
	}
	
	protected void governmentElectedDeducer(final ParticipantGameNotification notification) {
		final GameData gameData = notification.getGameData();
		if(gameData.isFascistDangerZone()) {
			gameData.getPlayers().stream()
					.filter(PlayerData::isChancellor)
					.map(PlayerData::getUsername)
					.forEach(provenNonHitlers::add);
		}
	}
	
	protected void governmentDeniedDeducer(final ParticipantGameNotification notification) {
		GameData gameData = notification.getGameData();
		final int mostSuspiciousGovernmentMemberBeforeVote = getMostSuspiciousGovernmentMember(new HashSet<>(Arrays.asList(previousPresident, previousChancellor)), gameData);
		gameData.getPlayers().stream()
				.filter(PlayerData::isAlive)
				.filter(player -> PartyMembership.UNKNOWN == player.getPartyMembership())
				.forEach(player -> {
					int suspicion = Vote.JA == player.getVote() ?
							mostSuspiciousGovernmentMemberBeforeVote : mostSuspiciousGovernmentMemberBeforeVote * -1;
					updateSuspectedMembership(player, suspicion, SuspicionAction.VOTE_CHOICE_RESULT, notification.getGameData());
				});
		printSuspectedPlayerMatrix("government was denied");
	}
	
	protected void anarchyDeducer(final ParticipantGameNotification notification) {
		policyOptionsForNextGovernment.clear();
		governmentDeniedDeducer(notification);
	}
	
	protected void fascistPolicyDeducer(final ParticipantGameNotification notification) {
		policyPlayedDeducer(notification, Policy.FASCIST);
	}
	
	protected void liberalPolicyDeducer(final ParticipantGameNotification notification) {
		policyPlayedDeducer(notification, Policy.LIBERAL);
	}
	
	protected void policyPlayedDeducer(final ParticipantGameNotification notification, Policy policy) {
		GameData gameData = notification.getGameData();
		Set<Policy> uniquePolicies = new HashSet<>(policyOptionsForNextGovernment);
		if (uniquePolicies.size() != 1) {
			final int mostSuspiciousGovernmentMemberBeforeVote = getMostSuspiciousGovernmentMember(new HashSet<>(Arrays.asList(previousPresident, previousChancellor)), gameData);
			gameData.getPlayers().stream()
					.filter(PlayerData::isAlive)
					.filter(player -> PartyMembership.UNKNOWN == player.getPartyMembership())
					.forEach(player -> 
						updateMembershipForPolicyPlayed(notification, policy, mostSuspiciousGovernmentMemberBeforeVote,
								player)
					);
			printSuspectedPlayerMatrix(String.format("%s policy was inacted", policy.name()));
		}
		policyOptionsForNextGovernment.clear();
		vetoRequestor = Optional.empty();
	}

	private void updateMembershipForPolicyPlayed(final ParticipantGameNotification notification, Policy policy,
			final int mostSuspiciousGovernmentMemberBeforeVote, PlayerData player) {
		if (previousChancellor.equals(player.getUsername())) {
			if (vetoRequestor.isPresent()) {
				updateSuspectedMembership(player, getSuspectedMembershipFromPolicy(getOppositePolicy(policy)), SuspicionAction.FAILED_VETO, notification.getGameData());
			} else {
				updateSuspectedMembership(player, getSuspectedMembershipFromPolicy(policy), SuspicionAction.POLICY_PASSED_CHANCELLOR, notification.getGameData());
			}
		} else if (previousPresident.equals(player.getUsername())) {
			if (vetoRequestor.isPresent()) {
				updateSuspectedMembership(player, getSuspectedMembershipFromPolicy(policy), SuspicionAction.FAILED_VETO, notification.getGameData());
			} else {
				updateSuspectedMembership(player, getSuspectedMembershipFromPolicy(policy), SuspicionAction.POLICY_PASSED_PRESIDENT, notification.getGameData());
			}
		} else {
			int suspicion = Vote.JA == player.getVote() ?
					mostSuspiciousGovernmentMemberBeforeVote : mostSuspiciousGovernmentMemberBeforeVote * -1;
			updateSuspectedMembership(player, suspicion, SuspicionAction.VOTE_CHOICE_RESULT, notification.getGameData());
		}
	}
	
	private int getMostSuspiciousGovernmentMember(Set<String> governmentMembers, GameData gameData) {
		Set<PlayerData> mostSuspectedFascists = getMostExpectedFascists(gameData);
		int leastSuspiciousOfTheMost = mostSuspectedFascists.stream()
				.mapToInt(player -> {
					if (PartyMembership.FASCIST == player.getPartyMembership()) {
						return MAX_SUSPICION * -1;
					}
					return getMembershipSuspicion(player.getUsername());
				})
				.max()
				.orElse(0);
		Set<PlayerData> suspiciousGovernmentMembers = gameData.getPlayers().stream()
				.filter(player -> governmentMembers.contains(player.getUsername()))
				.filter(player -> PartyMembership.LIBERAL != player.getPartyMembership())
				.collect(Collectors.toSet());
		if (suspiciousGovernmentMembers.stream()
				.map(PlayerData::getPartyMembership)
				.anyMatch(PartyMembership.FASCIST::equals)) {
			return MAX_SUSPICION * -1;
		}
		return suspiciousGovernmentMembers.stream()
				.map(PlayerData::getUsername)
				.mapToInt(this::getMembershipSuspicion)
				.map(suspicion -> suspicion - leastSuspiciousOfTheMost)
				.min()
				.orElse(MAX_SUSPICION);
	}

	@Override
	protected void examinationHelper(final GameData gameData) {
		policyOptionsForNextGovernment = new ArrayList<>(gameData.getPoliciesToView());
	}
	
	protected void playerKilledDeducer(final ParticipantGameNotification notification) {
		String[] args = notification.getAction().getArgs();
		String killerName = args[0];
		GameData gameData = notification.getGameData();
		if (killerName.equals(gameData.getMyPlayer().getUsername())) {
			return;
		}
		PlayerData killer = gameData.getPlayers().stream()
				.filter(player -> killerName.equals(player.getUsername()))
				.findAny()
				.orElseThrow(() -> new IllegalStateException("Killer not found!"));
		if (PartyMembership.UNKNOWN != killer.getPartyMembership()) {
			return;
		}
		String victimName = args[1];
		int victimSuspicion = getMembershipSuspicion(victimName);
		if (victimSuspicion == 0) {
			return;
		}
		updateSuspectedMembership(killer, -victimSuspicion, SuspicionAction.KILLED_PLAYER, gameData);
		printSuspectedPlayerMatrix(String.format("%s killed %s", killer, victimName));
	}
	
	@Override
	protected boolean presidentVetoHelper(final GameData gameData) {
		boolean concur = super.presidentVetoHelper(gameData);
		Set<Policy> uniquePolicies = new HashSet<>(gameData.getPoliciesToView());
		if (uniquePolicies.size() == 1) {
			PartyMembership membership = gameData.getMyPlayer().getPartyMembership();
			gameData.getPlayers().stream()
					.filter(PlayerData::isChancellor)
					.filter(chancellor -> PartyMembership.UNKNOWN == chancellor.getPartyMembership())
					.forEach(chancellor -> updateSuspectedMembership(chancellor, concur ? 
							membership : getOppositeMembership(membership), SuspicionAction.CHANCELLOR_VETO, gameData));
			printSuspectedPlayerMatrix("chancellor asked to veto the policies");
		}
		return concur;
	}
	
	protected void specialElectionChosenDeducer(final ParticipantGameNotification notification) {
		List<PlayerData> government = Stream.of(notification.getAction().getArgs())
				.map(username -> getPlayerByUsername(notification.getGameData().getPlayers(), username))
				.collect(Collectors.toList());
		updateSuspectedMembershipForChosenTeam(government.get(0), government.get(1),
				SuspicionAction.PRESIDENTIAL_CANDIDATE_CHOSEN, notification.getGameData());
		printSuspectedPlayerMatrix("special election was chosen");
	}
	
	protected void chancellorVetoDeducer(final ParticipantGameNotification notification) {
		vetoRequestor = Optional.of(notification.getAction().getArgs()[0]);
	}
	
	protected void presidentVetoDeducer(final ParticipantGameNotification notification) {
		final String presidentName = notification.getAction().getArgs()[0];
		GameData gameData = notification.getGameData();
		if (presidentName.contentEquals(gameData.getMyPlayer().getUsername())) {
			return;
		}
		List<PlayerData> players = gameData.getPlayers();
		updateSuspectedMembershipForChosenTeam(getPlayerByUsername(players, presidentName),
				getPlayerByUsername(players, vetoRequestor.orElse(null)), SuspicionAction.SUCCESSFUL_VETO, gameData);
		printSuspectedPlayerMatrix("policies were vetoed");
	}
	
	protected abstract int getMembershipSuspicion(String username);
	protected abstract void updateSuspectedMembership(final PlayerData player, final PartyMembership membership, final SuspicionAction suspicionAction, final GameData gameData);
	protected abstract void updateSuspectedMembership(final PlayerData player, final int suspicion, final SuspicionAction suspicionAction, final GameData gameData);
	protected abstract void printSuspectedPlayerMatrix(final String action);
	protected abstract void updateSuspectedMembershipsForGovernment(final List<PlayerData> team,
			final SuspicionAction suspicionAction, final GameData gameData, int govtSuspectedMembership);
}
