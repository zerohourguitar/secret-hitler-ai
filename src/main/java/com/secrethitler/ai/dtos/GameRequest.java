package com.secrethitler.ai.dtos;

import java.util.List;

public class GameRequest {
	private String id;
	private List<GameParticipant> participants;
	private boolean readyToStart;
	private boolean availableForMorePlayers;
	private boolean started;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public List<GameParticipant> getParticipants() {
		return participants;
	}
	public void setParticipants(List<GameParticipant> participants) {
		this.participants = participants;
	}
	public boolean isReadyToStart() {
		return readyToStart;
	}
	public void setReadyToStart(boolean readyToStart) {
		this.readyToStart = readyToStart;
	}
	public boolean isAvailableForMorePlayers() {
		return availableForMorePlayers;
	}
	public void setAvailableForMorePlayers(boolean availableForMorePlayers) {
		this.availableForMorePlayers = availableForMorePlayers;
	}
	public boolean isStarted() {
		return started;
	}
	public void setStarted(boolean started) {
		this.started = started;
	}
}
