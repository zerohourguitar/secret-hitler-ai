package com.secrethitler.ai.dtos;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.SecretRole;
import com.secrethitler.ai.enums.Vote;

public class PlayerData {
	private String username;
	private boolean host;
	private boolean connected;
	private boolean alive;
	private PartyMembership partyMembership;
	private SecretRole secretRole;
	private boolean president;
	private boolean chancellor;
	private boolean previousGovernmentMember;
	private Vote vote;
	private boolean voteReady;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public boolean isHost() {
		return host;
	}
	public void setHost(boolean host) {
		this.host = host;
	}
	public boolean isConnected() {
		return connected;
	}
	public void setConnected(boolean connected) {
		this.connected = connected;
	}
	public boolean isAlive() {
		return alive;
	}
	public void setAlive(boolean alive) {
		this.alive = alive;
	}
	public PartyMembership getPartyMembership() {
		return partyMembership;
	}
	public void setPartyMembership(PartyMembership partyMembership) {
		this.partyMembership = partyMembership;
	}
	public SecretRole getSecretRole() {
		return secretRole;
	}
	public void setSecretRole(SecretRole secretRole) {
		this.secretRole = secretRole;
	}
	public boolean isPresident() {
		return president;
	}
	public void setPresident(boolean president) {
		this.president = president;
	}
	public boolean isChancellor() {
		return chancellor;
	}
	public void setChancellor(boolean chancellor) {
		this.chancellor = chancellor;
	}
	public boolean isPreviousGovernmentMember() {
		return previousGovernmentMember;
	}
	public void setPreviousGovernmentMember(boolean previousGovernmentMember) {
		this.previousGovernmentMember = previousGovernmentMember;
	}
	public Vote getVote() {
		return vote;
	}
	public void setVote(Vote vote) {
		this.vote = vote;
	}
	public boolean isVoteReady() {
		return voteReady;
	}
	public void setVoteReady(boolean voteReady) {
		this.voteReady = voteReady;
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
