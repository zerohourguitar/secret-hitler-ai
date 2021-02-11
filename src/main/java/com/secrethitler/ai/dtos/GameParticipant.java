package com.secrethitler.ai.dtos;

import com.secrethitler.ai.enums.ParticipantRole;

public class GameParticipant {
	private String username;
	private ParticipantRole role;
	private boolean connected;
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public ParticipantRole getRole() {
		return role;
	}
	public void setRole(ParticipantRole role) {
		this.role = role;
	}
	public boolean isConnected() {
		return connected;
	}
	public void setConnected(boolean connected) {
		this.connected = connected;
	}
}
