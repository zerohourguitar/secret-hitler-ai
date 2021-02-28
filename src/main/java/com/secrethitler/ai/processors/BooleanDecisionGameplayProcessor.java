package com.secrethitler.ai.processors;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.ParticipantGameNotification;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.Action;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.Policy;
import com.secrethitler.ai.enums.SecretRole;
import com.secrethitler.ai.enums.Vote;
import com.secrethitler.ai.utils.RandomUtil;

public class BooleanDecisionGameplayProcessor extends SimpleGameplayProcessor {	
	private static final Logger LOGGER = Logger.getLogger(BooleanDecisionGameplayProcessor.class.getName());
	private static final Map<Policy, Policy> OPPOSITE_POLICY_MAP = ImmutableMap.<Policy, Policy>builder()
			.put(Policy.FASCIST, Policy.LIBERAL)
			.put(Policy.LIBERAL, Policy.FASCIST)
			.build();
	
	private static final Map<Policy, PartyMembership> POLICY_TO_MEMBERSHIP_MAP = ImmutableMap.<Policy, PartyMembership>builder()
			.put(Policy.FASCIST, PartyMembership.FASCIST)
			.put(Policy.LIBERAL, PartyMembership.LIBERAL)
			.build();
	
	private static final Map<PartyMembership, Integer> SUSPECTED_PARTY_MEMBERSHIP_INVESTIGATION_ORDER = ImmutableMap.<PartyMembership, Integer>builder()
			.put(PartyMembership.FASCIST, 1)
			.put(PartyMembership.UNKNOWN, 2)
			.put(PartyMembership.LIBERAL, 3)
			.build();
	
	protected static boolean playerKnowsRoles(SecretRole myRole, int numberOfPlayers) {
		return SecretRole.FASCIST == myRole || (SecretRole.HITLER == myRole && numberOfPlayers < 7);
	}
	
	protected static Policy getOppositePolicy(final Policy policy) {
		return OPPOSITE_POLICY_MAP.get(policy);
	}
	
	protected static PartyMembership getSuspectedMembershipFromPolicy(final Policy policy) {
		return POLICY_TO_MEMBERSHIP_MAP.get(policy);
	}
	
	protected static int getOrderWeightOfSuspectedMembership(PartyMembership suspectedMembership) {
		return SUSPECTED_PARTY_MEMBERSHIP_INVESTIGATION_ORDER.get(suspectedMembership);
	}
	
	protected static PlayerData getPlayerByUsername(final List<PlayerData> players, final String username) {
		return players.stream()
				.filter(player -> username.equals(player.getUsername()))
				.findAny()
				.orElse(null);
	}
	
	private final Map<Action, Consumer<ParticipantGameNotification>> actionToDeduceMap;
	
	private Map<String, PartyMembership> suspectedMemberships = new HashMap<>();
	protected Set<String> provenNonHitlers = new HashSet<>();
	protected boolean policyOptionForNextGovernment = true;
	protected Optional<String> vetoRequestor = Optional.empty();

	public BooleanDecisionGameplayProcessor(final String username, final RandomUtil randomUtil) {
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
	
	protected PartyMembership getSuspectedMembership(String username) {
		return suspectedMemberships.getOrDefault(username, PartyMembership.UNKNOWN);
	}
	
	protected void setSuspectedMembership(final PlayerData player, final PartyMembership membership) {
		if (player.getPartyMembership() != PartyMembership.UNKNOWN && membership != player.getPartyMembership()) {
			throw new IllegalArgumentException("Tried to set a suspected membership of a player that is known to be incorrect!");
		}
		final String suspectUsername = player.getUsername();
		suspectedMemberships.put(suspectUsername, membership);
	}
	
	private void printSuspectedPlayerMatrix(final String action) {
		LOGGER.info(() -> String.format("%s's suspected player matrix after %s: %n%s", username, action, suspectedMemberships.toString()));
	}

	@Override
	public Optional<GameplayAction> getActionToTake(final ParticipantGameNotification notification) {
		GameData gameData = notification.getGameData();
		if (!playerKnowsRoles(gameData.getMyPlayer().getSecretRole(), gameData.getPlayers().size())) {
			actionToDeduceMap.getOrDefault(notification.getAction().getAction(), data -> {}).accept(notification);
		}
		return super.getActionToTake(notification);
	}
	
	@Override
	protected List<PlayerData> getMostLikelyHitlerPreference(List<PlayerData> players, boolean hitlerPreferred) {
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
		List<PlayerData> mostLikelyPlayers = players.stream()
				.filter(player -> membership == getSuspectedMembership(player.getUsername()))
				.collect(Collectors.toList());
		if (mostLikelyPlayers.isEmpty()) {
			return players;
		}
		return mostLikelyPlayers;
	}
	
	@Override
	protected boolean isVoteJa(Stream<PlayerData> governmentStream, PartyMembership myMembership, SecretRole myRole, int numberOfPlayers) {
		if (!playerKnowsRoles(myRole, numberOfPlayers)) {
			List<PlayerData> government = governmentStream.collect(Collectors.toList());
			PartyMembership suspectedMembership = updateSuspectedMembershipForChosenTeam(government);
			printSuspectedPlayerMatrix("running mate was chosen");
			
			return ImmutableSet.of(PartyMembership.UNKNOWN, myMembership).contains(suspectedMembership);
		}
		return governmentStream.anyMatch(player -> PartyMembership.FASCIST == player.getPartyMembership());
	}

	private PartyMembership updateSuspectedMembershipForChosenTeam(List<PlayerData> team) {
		Set<PartyMembership> knownMemberships = team.stream()
				.map(PlayerData::getPartyMembership)
				.filter(membership -> PartyMembership.UNKNOWN != membership)
				.collect(Collectors.toSet());
		PartyMembership suspectedMembership = PartyMembership.UNKNOWN;
		if (knownMemberships.size() == 1) {
			PartyMembership knownMembership = knownMemberships.stream().findAny().orElse(null);
			suspectedMembership = knownMembership;
			team.forEach(player -> setSuspectedMembership(player, knownMembership));
		} else if (knownMemberships.size() == 2) {
			suspectedMembership = PartyMembership.FASCIST;
		} else {
			Set<PartyMembership> govtSuspectedMemberships = team.stream()
					.map(player -> getSuspectedMembership(player.getUsername()))
					.filter(membership -> PartyMembership.UNKNOWN != membership)
					.collect(Collectors.toSet());
			if (govtSuspectedMemberships.size() == 1) {
				suspectedMembership = govtSuspectedMemberships.stream().findAny().orElse(null);
				final PartyMembership newSuspectedMembership = suspectedMembership;
				team.forEach(player -> setSuspectedMembership(player, newSuspectedMembership));
			} else if (govtSuspectedMemberships.size() == 2) {
				team.forEach(player -> setSuspectedMembership(player, PartyMembership.UNKNOWN));
			}
		}
		return suspectedMembership;
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
		PlayerData myPlayer = gameData.getMyPlayer();
		PartyMembership myMembership = myPlayer.getPartyMembership();
		gameData.getPlayers().stream()
				.filter(PlayerData::isAlive)
				.filter(player -> PartyMembership.UNKNOWN == player.getPartyMembership())
				.forEach(player -> {
					PartyMembership suspectedMembership = myPlayer.getVote() == player.getVote() ?
							myMembership : getOppositeMembership(myMembership);
					setSuspectedMembership(player, suspectedMembership);
				});
		printSuspectedPlayerMatrix("government was denied");
	}
	
	protected void anarchyDeducer(final ParticipantGameNotification notification) {
		policyOptionForNextGovernment = true;
		governmentDeniedDeducer(notification);
	}
	
	protected void fascistPolicyDeducer(final ParticipantGameNotification notification) {
		policyPlayedDeducer(notification, Policy.FASCIST);
	}
	
	protected void liberalPolicyDeducer(final ParticipantGameNotification notification) {
		policyPlayedDeducer(notification, Policy.LIBERAL);
	}
	
	protected void policyPlayedDeducer(final ParticipantGameNotification notification, Policy policy) {
		if (policyOptionForNextGovernment) {
			notification.getGameData().getPlayers().stream()
					.filter(PlayerData::isAlive)
					.filter(player -> PartyMembership.UNKNOWN == player.getPartyMembership())
					.forEach(player -> {
						if (vetoRequestor.isPresent() && player.getUsername().equals(vetoRequestor.get())) {
							setSuspectedMembership(player, getSuspectedMembershipFromPolicy(getOppositePolicy(policy)));
						} else {
							Policy policyVotedFor = Vote.JA == player.getVote() ?
									policy : getOppositePolicy(policy);
							setSuspectedMembership(player, getSuspectedMembershipFromPolicy(policyVotedFor));
						}
					});
			printSuspectedPlayerMatrix(String.format("%s policy was inacted", policy.name()));
		}
		policyOptionForNextGovernment = true;
		vetoRequestor = Optional.empty();
	}
	
	@Override
	protected void examinationHelper(final GameData gameData) {
		Set<Policy> uniquePolicies = new HashSet<>(gameData.getPoliciesToView());
		policyOptionForNextGovernment = uniquePolicies.size() != 1;
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
		PartyMembership suspectedVictimParty = getSuspectedMembership(victimName);
		if (PartyMembership.UNKNOWN == suspectedVictimParty) {
			return;
		}
		setSuspectedMembership(killer, getOppositeMembership(suspectedVictimParty));
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
					.forEach(chancellor -> setSuspectedMembership(chancellor, concur ? 
							membership : getOppositeMembership(membership)));
			printSuspectedPlayerMatrix("chancellor asked to veto the policies");
		}
		return concur;
	}
	
	@Override
	protected List<PlayerData> getPreferredPlayersToInvestigate(List<PlayerData> unknownPlayers) {
		return unknownPlayers.stream()
				.collect(Collectors.groupingBy(player -> 
						getOrderWeightOfSuspectedMembership(
								getSuspectedMembership(player.getUsername())))).entrySet().stream()
				.sorted((entry1, entry2) -> entry1.getKey().compareTo(entry2.getKey()))
				.findFirst()
				.map(Entry::getValue)
				.orElse(Collections.emptyList());
	}
	
	protected void specialElectionChosenDeducer(final ParticipantGameNotification notification) {
		updateSuspectedMembershipForChosenTeam(
				Stream.of(notification.getAction().getArgs())
						.map(username -> getPlayerByUsername(notification.getGameData().getPlayers(), username))
						.collect(Collectors.toList()));
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
		updateSuspectedMembershipForChosenTeam(Arrays.asList(
				getPlayerByUsername(players, presidentName), 
				getPlayerByUsername(players, vetoRequestor.orElse(null))));
		printSuspectedPlayerMatrix("policies were vetoed");
	}
	
}
