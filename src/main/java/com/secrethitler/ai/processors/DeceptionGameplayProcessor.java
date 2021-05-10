package com.secrethitler.ai.processors;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.SecretRole;
import com.secrethitler.ai.utils.RandomUtil;

public class DeceptionGameplayProcessor extends WeightedDeductionGameplayProcessor {
	private final static int MAX_LIB_POLICIES = 4;
	
	public DeceptionGameplayProcessor(String username, RandomUtil randomUtil) {
		super(username, randomUtil);
	}
	
	@Override
	protected PlayerData chooseRunningMate(final GameData gameData, List<PlayerData> eligiblePlayers) {
		if(PartyMembership.FASCIST != gameData.getMyPlayer().getPartyMembership()) {
			return super.chooseRunningMate(gameData, eligiblePlayers);
		}
		boolean iAmHitler = SecretRole.HITLER == gameData.getMyPlayer().getSecretRole();
		if (gameData.isFascistDangerZone() && !iAmHitler) {
			PlayerData hitler = gameData.getPlayers().stream()
					.filter(player -> SecretRole.HITLER == player.getSecretRole())
					.findFirst()
					.orElseThrow(IllegalStateException::new);
			boolean selectHitler = !isPlayerSuspectedFascist(gameData, gameData.getMyPlayer()) && !isPlayerSuspectedFascist(gameData, hitler);
			if (selectHitler) {
				return hitler;
			}
		}
		int liberalPolicies = gameData.getLiberalPolicies();
		double chanceOfPickingFascist = liberalPolicies / MAX_LIB_POLICIES;
		if (randomUtil.getRandomNumber() < chanceOfPickingFascist) {
			if (iAmHitler) {
				return getMostSuspectedFascist(eligiblePlayers);
			}
			if (isPlayerSuspectedFascist(gameData, gameData.getMyPlayer())) {
				Optional<PlayerData> mostSuspectedFascist = getPlayersOrderedBySuspicion(eligiblePlayers).stream()
						.map(playerSet -> playerSet.stream()
								.filter(player -> SecretRole.FASCIST == player.getSecretRole())
								.findFirst()
								.orElse(null))
						.filter(player -> player != null)
						.findFirst();
				if (mostSuspectedFascist.isPresent()) {
					return mostSuspectedFascist.get();
				}
				return getMostSuspectedFascist(eligiblePlayers);
			} else {
				List<Set<PlayerData>> playersOrderedBySuspicion = getPlayersOrderedBySuspicion(eligiblePlayers);
				Collections.reverse(playersOrderedBySuspicion);
				Optional<PlayerData> leastSuspectedFascist = playersOrderedBySuspicion.stream()
						.map(playerSet -> playerSet.stream()
								.filter(player -> SecretRole.FASCIST == player.getSecretRole())
								.findFirst()
								.orElse(null))
						.filter(player -> player != null)
						.findFirst();
				if (leastSuspectedFascist.isPresent()) {
					return leastSuspectedFascist.get();
				}
				return getMostSuspectedFascist(eligiblePlayers);
			}
		} else {
			if (iAmHitler) {
				return getMostSuspectedLiberal(eligiblePlayers);
			}
			else if (randomUtil.getRandomNumber() < chanceOfPickingFascist) {
				List<PlayerData> liberalPlayers = getLiberalPlayers(eligiblePlayers);
				for (Set<PlayerData> playerSet : getPlayersOrderedBySuspicion(liberalPlayers)) {
					for (PlayerData player : playerSet) {
						if (!isPlayerSuspectedFascist(gameData, player)) {
							return player;
						}
					}
				}
			} else {
				List<PlayerData> liberalPlayers = getLiberalPlayers(eligiblePlayers);
				return getMostSuspectedLiberal(liberalPlayers);
			}
		}
		//this should never happen
		return null;
	}

	private List<PlayerData> getLiberalPlayers(List<PlayerData> eligiblePlayers) {
		return eligiblePlayers.stream()
				.filter(player -> PartyMembership.LIBERAL == player.getPartyMembership())
				.collect(Collectors.toList());
	}

	private PlayerData getMostSuspectedFascist(List<PlayerData> eligiblePlayers) {
		return getPlayersOrderedBySuspicion(eligiblePlayers).stream()
				.findFirst()
				.orElseThrow(IllegalStateException::new)
				.iterator()
				.next();
	}
	
	private PlayerData getMostSuspectedLiberal(List<PlayerData> eligiblePlayers) {
		List<Set<PlayerData>> playersOrderedBySuspicion = getPlayersOrderedBySuspicion(eligiblePlayers);
		Collections.reverse(playersOrderedBySuspicion);
		return playersOrderedBySuspicion.stream()
				.findFirst()
				.orElseThrow(IllegalStateException::new)
				.iterator()
				.next();
	}
	
	private boolean isPlayerSuspectedFascist(final GameData gameData, PlayerData suspect) {
		int totalFascists = getNumberOfFascistsFromNumberOfPlayers(gameData.getPlayers().size());
		List<Set<PlayerData>> playersOrderedBySuspicion = getPlayersOrderedBySuspicion(gameData);
		for (int i=0; i < totalFascists; i++) {
			Set<PlayerData> playerLevel = playersOrderedBySuspicion.get(i);
			if (playerLevel.contains(suspect)) {
				return true;
			}
		}
		return false;
	}

}
