package com.secrethitler.ai.dtos;

import com.secrethitler.ai.enums.Action;

public class GameplayAction {
	private Action action;
	private String[] args;
	
	public GameplayAction() {
		super();
	}
	
	public GameplayAction(Action action, String[] args) {
		this.action = action;
		this.args = args;
	}
	
	public Action getAction() {
		return action;
	}
	public void setAction(Action action) {
		this.action = action;
	}
	public String[] getArgs() {
		return args;
	}
	public void setArgs(String[] args) {
		this.args = args;
	}
}
