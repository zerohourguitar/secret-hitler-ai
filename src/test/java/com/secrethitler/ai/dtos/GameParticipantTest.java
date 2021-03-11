package com.secrethitler.ai.dtos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.secrethitler.ai.enums.ParticipantRole;

public class GameParticipantTest {
	@Test
	public void testGettersSetters() {
		GameParticipant participant = new GameParticipant();
		participant.setConnected(true);
		participant.setRole(ParticipantRole.PLAYING);
		participant.setUsername("testUsername");
		assertTrue(participant.isConnected());
		assertEquals(ParticipantRole.PLAYING, participant.getRole());
		assertEquals("testUsername", participant.getUsername());
	}
}
