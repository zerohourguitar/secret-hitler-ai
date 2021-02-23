package com.secrethitler.ai.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.Action;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.SecretRole;
import com.secrethitler.ai.enums.Vote;

public class BooleanDecisionGameplayProcessorTest extends SimpleGameplayProcessorTest {
	private BooleanDecisionGameplayProcessor boolProcessor;
	private PlayerData aj;
	private PlayerData sean;
	
	@Before
	public void setUp() {
		super.setUp();
		processor = new BooleanDecisionGameplayProcessor("Robot 1", randomUtil);
		boolProcessor = (BooleanDecisionGameplayProcessor) processor;
		aj = new PlayerData();
		aj.setUsername("AJ");
		aj.setPartyMembership(PartyMembership.UNKNOWN);
		sean = new PlayerData();
		sean.setUsername("Sean");
		sean.setPartyMembership(PartyMembership.UNKNOWN);
	}
	
	@Override
	@Test
	public void testVote_FascistOneUnknown() {
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		myPlayer.setSecretRole(SecretRole.HITLER);
		liberal.setPresident(true);
		unknown.setChancellor(true);
		String[] args = {Vote.NEIN.name()};
		
		testVote(Optional.of(new GameplayAction(Action.VOTE, args)), Arrays.asList(liberal, liberal2, myPlayer, fascist, fascist2, unknown, unknown2));
	}

	@Test
	public void testIsVoteJa_LiberalBotOneFascist() {
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		
		assertFalse("The AI should vote nein.", boolProcessor.isVoteJa(Stream.of(aj, sean), PartyMembership.LIBERAL, SecretRole.LIBERAL, 7));
		
		assertEquals("AJ should still be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testIsVoteJa_HitlerBotOppositeMemberships() {
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		
		assertTrue("The AI should vote ja.", boolProcessor.isVoteJa(Stream.of(aj, sean), PartyMembership.FASCIST, SecretRole.HITLER, 7));
		
		assertEquals("AJ should now be Unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be Unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testIsVoteJa_LiberalBotOppositeMembershipsKnownFascist() {
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		aj.setPartyMembership(PartyMembership.FASCIST);
		
		assertFalse("The AI should vote nein.", boolProcessor.isVoteJa(Stream.of(aj, sean), PartyMembership.LIBERAL, SecretRole.LIBERAL, 7));
		
		assertEquals("AJ should still be a known Fascist", PartyMembership.FASCIST, aj.getPartyMembership());
		assertEquals("AJ should still be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should still not be a known Fascist", PartyMembership.UNKNOWN, sean.getPartyMembership());
		assertEquals("Sean should now be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testIsVoteJa_LiberalBotOppositeMembershipsTwoKnownOpposites() {
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		aj.setPartyMembership(PartyMembership.FASCIST);
		sean.setPartyMembership(PartyMembership.LIBERAL);
		
		assertFalse("The AI should vote nein.", boolProcessor.isVoteJa(Stream.of(aj, sean), PartyMembership.LIBERAL, SecretRole.LIBERAL, 7));
		
		assertEquals("AJ should still be a known Fascist", PartyMembership.FASCIST, aj.getPartyMembership());
		assertEquals("AJ should still be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should still be a known Liberal", PartyMembership.LIBERAL, sean.getPartyMembership());
		assertEquals("Sean should still be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}

	@Test
	public void testIsVoteJa_LiberalBotAllUnknown() {		
		assertTrue("The AI should vote ja.", boolProcessor.isVoteJa(Stream.of(aj, sean), PartyMembership.LIBERAL, SecretRole.LIBERAL, 5));
		
		assertEquals("AJ should now be Unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be Unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testIsVoteJa_FascistBotTwoLibs() {
		aj.setPartyMembership(PartyMembership.LIBERAL);
		sean.setPartyMembership(PartyMembership.LIBERAL);
		
		assertFalse("The AI should vote nein.", boolProcessor.isVoteJa(Stream.of(aj, sean), PartyMembership.FASCIST, SecretRole.FASCIST, 8));
	}
	
	@Test
	public void testIsVoteJa_FascistBotOppositeMemberships() {
		aj.setPartyMembership(PartyMembership.FASCIST);
		sean.setPartyMembership(PartyMembership.LIBERAL);
		
		assertTrue("The AI should vote ja.", boolProcessor.isVoteJa(Stream.of(aj, sean), PartyMembership.FASCIST, SecretRole.FASCIST, 5));
	}
	
	@Test
	public void testIsVoteJa_HilterBotFewerThanSevenPlayers() {
		aj.setPartyMembership(PartyMembership.FASCIST);
		sean.setPartyMembership(PartyMembership.FASCIST);
		
		assertTrue("The AI should vote ja.", boolProcessor.isVoteJa(Stream.of(aj, sean), PartyMembership.FASCIST, SecretRole.HITLER, 5));
	}
}
