package com.secrethitler.ai.dtos;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class GameplayActionTest {
	private static final GameplayAction action = new GameplayAction();
	
	@Test
	public void testHashCode() {
		assertNotEquals(0, action.hashCode());
	}
	
	@Test
	public void testToString() {
		assertNotNull(action.toString());
	}
}
