package com.secrethitler.ai.dtos;

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
}
