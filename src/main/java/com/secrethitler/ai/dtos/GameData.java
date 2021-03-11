package com.secrethitler.ai.dtos;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.secrethitler.ai.enums.GamePhase;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.Policy;

public class GameData {
	private PlayerData myPlayer;
	private List<PlayerData> players;
	private Set<String> watchers;
	private GamePhase phase;
	private int policyDocketSize;
	private int deniedPolicies;
	private int liberalPolicies;
	private int fascistPolicies;
	private int unsuccessfulGovernments;
	private boolean fascistDangerZone;
	private boolean vetoUnlocked;
	private List<String> history;
	private List<Policy> policiesToView;
	private String nextPresident;
	private PartyMembership winners;
	private String nextGameId;
	
	public PlayerData getMyPlayer() {
		return myPlayer;
	}
	public void setMyPlayer(PlayerData myPlayer) {
		this.myPlayer = myPlayer;
	}
	public List<PlayerData> getPlayers() {
		return players;
	}
	public void setPlayers(List<PlayerData> players) {
		this.players = players;
	}
	public Set<String> getWatchers() {
		return watchers;
	}
	public void setWatchers(Set<String> watchers) {
		this.watchers = watchers;
	}
	public GamePhase getPhase() {
		return phase;
	}
	public void setPhase(GamePhase phase) {
		this.phase = phase;
	}
	public int getPolicyDocketSize() {
		return policyDocketSize;
	}
	public void setPolicyDocketSize(int policyDocketSize) {
		this.policyDocketSize = policyDocketSize;
	}
	public int getDeniedPolicies() {
		return deniedPolicies;
	}
	public void setDeniedPolicies(int deniedPolicies) {
		this.deniedPolicies = deniedPolicies;
	}
	public int getLiberalPolicies() {
		return liberalPolicies;
	}
	public void setLiberalPolicies(int liberalPolicies) {
		this.liberalPolicies = liberalPolicies;
	}
	public int getFascistPolicies() {
		return fascistPolicies;
	}
	public void setFascistPolicies(int fascistPolicies) {
		this.fascistPolicies = fascistPolicies;
	}
	public int getUnsuccessfulGovernments() {
		return unsuccessfulGovernments;
	}
	public void setUnsuccessfulGovernments(int unsuccessfulGovernments) {
		this.unsuccessfulGovernments = unsuccessfulGovernments;
	}
	public boolean isFascistDangerZone() {
		return fascistDangerZone;
	}
	public void setFascistDangerZone(boolean fascistDangerZone) {
		this.fascistDangerZone = fascistDangerZone;
	}
	public boolean isVetoUnlocked() {
		return vetoUnlocked;
	}
	public void setVetoUnlocked(boolean vetoUnlocked) {
		this.vetoUnlocked = vetoUnlocked;
	}
	public List<String> getHistory() {
		return history;
	}
	public void setHistory(List<String> history) {
		this.history = history;
	}
	public List<Policy> getPoliciesToView() {
		return policiesToView;
	}
	public void setPoliciesToView(List<Policy> policiesToView) {
		this.policiesToView = policiesToView;
	}
	public String getNextPresident() {
		return nextPresident;
	}
	public void setNextPresident(String nextPresident) {
		this.nextPresident = nextPresident;
	}
	public PartyMembership getWinners() {
		return winners;
	}
	public void setWinners(PartyMembership winners) {
		this.winners = winners;
	}
	public String getNextGameId() {
		return nextGameId;
	}
	public void setNextGameId(String nextGameId) {
		this.nextGameId = nextGameId;
	}
	
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
