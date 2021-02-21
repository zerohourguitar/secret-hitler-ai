package com.secrethitler.ai.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.SecretRole;

public class BooleanDecisionGameplayProcessorTest {
	private BooleanDecisionGameplayProcessor processor;
	private PlayerData aj;
	private PlayerData sean;
	
	@Before
	public void setUp() {
		processor = new BooleanDecisionGameplayProcessor("Robot 1");
		aj = new PlayerData();
		aj.setUsername("AJ");
		aj.setPartyMembership(PartyMembership.UNKNOWN);
		sean = new PlayerData();
		sean.setUsername("Sean");
		sean.setPartyMembership(PartyMembership.UNKNOWN);
	}

	@Test
	public void testIsVoteJa_LiberalBotOneFascist() {
		processor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		
		assertFalse("The AI should vote nein.", processor.isVoteJa(Stream.of(aj, sean), PartyMembership.LIBERAL, SecretRole.LIBERAL, 7));
		
		assertEquals("AJ should still be a suspected Fascist", PartyMembership.FASCIST, processor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be a suspected Fascist", PartyMembership.FASCIST, processor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testIsVoteJa_HitlerBotOppositeMemberships() {
		processor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		processor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		
		assertTrue("The AI should vote ja.", processor.isVoteJa(Stream.of(aj, sean), PartyMembership.FASCIST, SecretRole.HITLER, 7));
		
		assertEquals("AJ should now be Unknown", PartyMembership.UNKNOWN, processor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be Unknown", PartyMembership.UNKNOWN, processor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testIsVoteJa_LiberalBotOppositeMembershipsKnownFascist() {
		processor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		processor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		aj.setPartyMembership(PartyMembership.FASCIST);
		
		assertFalse("The AI should vote nein.", processor.isVoteJa(Stream.of(aj, sean), PartyMembership.LIBERAL, SecretRole.LIBERAL, 7));
		
		assertEquals("AJ should still be a known Fascist", PartyMembership.FASCIST, aj.getPartyMembership());
		assertEquals("AJ should still be a suspected Fascist", PartyMembership.FASCIST, processor.getSuspectedMembership("AJ"));
		assertEquals("Sean should still not be a known Fascist", PartyMembership.UNKNOWN, sean.getPartyMembership());
		assertEquals("Sean should now be a suspected Fascist", PartyMembership.FASCIST, processor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testIsVoteJa_LiberalBotOppositeMembershipsTwoKnownOpposites() {
		processor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		processor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		aj.setPartyMembership(PartyMembership.FASCIST);
		sean.setPartyMembership(PartyMembership.LIBERAL);
		
		assertFalse("The AI should vote nein.", processor.isVoteJa(Stream.of(aj, sean), PartyMembership.LIBERAL, SecretRole.LIBERAL, 7));
		
		assertEquals("AJ should still be a known Fascist", PartyMembership.FASCIST, aj.getPartyMembership());
		assertEquals("AJ should still be a suspected Fascist", PartyMembership.FASCIST, processor.getSuspectedMembership("AJ"));
		assertEquals("Sean should still be a known Liberal", PartyMembership.LIBERAL, sean.getPartyMembership());
		assertEquals("Sean should still be a suspected Liberal", PartyMembership.LIBERAL, processor.getSuspectedMembership("Sean"));
	}

	@Test
	public void testIsVoteJa_LiberalBotAllUnknown() {		
		assertTrue("The AI should vote ja.", processor.isVoteJa(Stream.of(aj, sean), PartyMembership.LIBERAL, SecretRole.LIBERAL, 5));
		
		assertEquals("AJ should now be Unknown", PartyMembership.UNKNOWN, processor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be Unknown", PartyMembership.UNKNOWN, processor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testIsVoteJa_FascistBotTwoLibs() {
		aj.setPartyMembership(PartyMembership.LIBERAL);
		sean.setPartyMembership(PartyMembership.LIBERAL);
		
		assertFalse("The AI should vote nein.", processor.isVoteJa(Stream.of(aj, sean), PartyMembership.FASCIST, SecretRole.FASCIST, 8));
	}
	
	@Test
	public void testIsVoteJa_FascistBotOppositeMemberships() {
		aj.setPartyMembership(PartyMembership.FASCIST);
		sean.setPartyMembership(PartyMembership.LIBERAL);
		
		assertTrue("The AI should vote ja.", processor.isVoteJa(Stream.of(aj, sean), PartyMembership.FASCIST, SecretRole.FASCIST, 5));
	}
	
	@Test
	public void testIsVoteJa_HilterBotFewerThanSevenPlayers() {
		aj.setPartyMembership(PartyMembership.FASCIST);
		sean.setPartyMembership(PartyMembership.FASCIST);
		
		assertTrue("The AI should vote ja.", processor.isVoteJa(Stream.of(aj, sean), PartyMembership.FASCIST, SecretRole.HITLER, 5));
	}
}
