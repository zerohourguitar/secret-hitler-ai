package com.secrethitler.ai.dtos;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class GameDataTest {
	private static final GameData gameData = new GameData();

	@Test
	public void testHashCode() {
		assertNotEquals(0, gameData.hashCode());
	}
	
	@Test
	public void testToString() {
		assertNotNull(gameData.toString());
	}
}
