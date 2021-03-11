package com.secrethitler.ai.dtos;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ParticipantGameNotification {
	private GameData gameData;
	private GameplayAction action;
	
	public GameData getGameData() {
		return gameData;
	}
	public void setGameData(GameData gameData) {
		this.gameData = gameData;
	}
	public GameplayAction getAction() {
		return action;
	}
	public void setAction(GameplayAction action) {
		this.action = action;
	}
	
	@Override
	public boolean equals(Object o) {
		return EqualsBuilder.reflectionEquals(this, o);
	}
	
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
