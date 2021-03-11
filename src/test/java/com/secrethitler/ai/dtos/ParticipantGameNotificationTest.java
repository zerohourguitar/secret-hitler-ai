package com.secrethitler.ai.dtos;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ParticipantGameNotificationTest {
	private static final ParticipantGameNotification notification = new ParticipantGameNotification();
	
	@Test
	public void testHashCode() {
		assertNotEquals(0, notification.hashCode());
	}
	
	@Test
	public void testToString() {
		assertNotNull(notification.toString());
	}
}
