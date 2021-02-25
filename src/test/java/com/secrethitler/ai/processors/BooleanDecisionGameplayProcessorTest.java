package com.secrethitler.ai.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.Action;
import com.secrethitler.ai.enums.GamePhase;
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
		aj.setAlive(true);
		aj.setUsername("AJ");
		aj.setPartyMembership(PartyMembership.UNKNOWN);
		sean = new PlayerData();
		sean.setAlive(true);
		sean.setUsername("Sean");
		sean.setPartyMembership(PartyMembership.UNKNOWN);
		gameData.setPlayers(Collections.emptyList());
		notification.setAction(new GameplayAction(Action.CONNECTED, null));
	}
	
	@Test
	public void testSetSuspectedMembership_BadSuspect() {
		PlayerData player = new PlayerData();
		player.setPartyMembership(PartyMembership.LIBERAL);
		
		try {
			boolProcessor.setSuspectedMembership(player, PartyMembership.FASCIST);
			fail("Expected an IllegalArgumentException to be thrown if we suspect a player to be membership that we know to be false");
		} catch (IllegalArgumentException e) {
			assertEquals("Tried to set a suspected membership of a player that is known to be incorrect!", e.getMessage());
		}
	}
	
	@Test
	public void testChooseRunningMate_LiberalSuspectedMembers() {
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, unknown, fascist, unknown2, fascist2));
		boolProcessor.setSuspectedMembership(unknown, PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(unknown2, PartyMembership.FASCIST);
		String[] args = {"3"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(unknown))).thenReturn(unknown);
		
		testChooseRunningMate_Unknowns(args, PartyMembership.LIBERAL, SecretRole.LIBERAL);
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(unknown));
	}
	
	@Test
	public void testChooseRunningMate_HitlerSuspectedMembers() {
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, unknown, liberal, unknown2, liberal2));
		boolProcessor.setSuspectedMembership(unknown, PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(unknown2, PartyMembership.FASCIST);
		String[] args = {"5"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(unknown2))).thenReturn(unknown2);
		
		testChooseRunningMate_Unknowns(args, PartyMembership.FASCIST, SecretRole.HITLER);
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(unknown2));
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
	public void testVote_LiberalBotOneSuspectedFascist() {
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		
		testVoteHelper(Vote.NEIN);
		
		assertEquals("AJ should still be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testVote_HitlerBotOppositeSuspectedMemberships() {
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		myPlayer.setSecretRole(SecretRole.HITLER);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		
		testVoteHelper(Vote.JA, Arrays.asList(aj, sean, unknown, unknown, unknown, unknown, unknown));
		
		assertEquals("AJ should now be Unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be Unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testVote_LiberalBotSuspectedOppositeMembershipsKnownFascist() {
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		aj.setPartyMembership(PartyMembership.FASCIST);
		
		testVoteHelper(Vote.NEIN);
		
		assertEquals("AJ should still be a known Fascist", PartyMembership.FASCIST, aj.getPartyMembership());
		assertEquals("AJ should still be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should still not be a known Fascist", PartyMembership.UNKNOWN, sean.getPartyMembership());
		assertEquals("Sean should now be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testVote_LiberalBotTwoKnownOpposites() {
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		aj.setPartyMembership(PartyMembership.FASCIST);
		sean.setPartyMembership(PartyMembership.LIBERAL);
		
		testVoteHelper(Vote.NEIN);
		
		assertEquals("AJ should still be a known Fascist", PartyMembership.FASCIST, aj.getPartyMembership());
		assertEquals("AJ should still be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should still be a known Liberal", PartyMembership.LIBERAL, sean.getPartyMembership());
		assertEquals("Sean should still be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}

	@Test
	public void testVote_LiberalBotAllUnknown() {	
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		testVoteHelper(Vote.JA);
		
		assertEquals("AJ should now be Unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be Unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testVote_FascistBotTwoLibs() {
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		myPlayer.setSecretRole(SecretRole.FASCIST);
		aj.setPartyMembership(PartyMembership.LIBERAL);
		sean.setPartyMembership(PartyMembership.LIBERAL);
		
		testVoteHelper(Vote.NEIN);
	}
	
	@Test
	public void testVote_FascistBotOppositeMemberships() {
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		myPlayer.setSecretRole(SecretRole.FASCIST);
		aj.setPartyMembership(PartyMembership.FASCIST);
		sean.setPartyMembership(PartyMembership.LIBERAL);
		
		testVoteHelper(Vote.JA);
	}
	
	@Test
	public void testIsVoteJa_HilterBotFewerThanSevenPlayers() {
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		myPlayer.setSecretRole(SecretRole.HITLER);
		aj.setPartyMembership(PartyMembership.FASCIST);
		sean.setPartyMembership(PartyMembership.FASCIST);
		
		assertTrue("The AI should vote ja.", boolProcessor.isVoteJa(Stream.of(aj, sean), PartyMembership.FASCIST, SecretRole.HITLER, 5));
	}
	
	private void testVoteHelper(Vote vote) {
		testVoteHelper(vote, Arrays.asList(aj, sean));
	}
	
	private void testVoteHelper(Vote vote, List<PlayerData> players) {
		aj.setPresident(true);
		sean.setChancellor(true);
		String[] args = {vote.name()};
		
		testVote(Optional.of(new GameplayAction(Action.VOTE, args)), players);
	}
	
	@Test
	public void testGovernmentDenied_LiberalJa() {
		testGovernmentDenied(PartyMembership.LIBERAL, SecretRole.LIBERAL, Vote.JA);
		
		assertEquals("AJ should be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testGovernmentDenied_LiberalNein() {
		testGovernmentDenied(PartyMembership.LIBERAL, SecretRole.LIBERAL, Vote.NEIN);
		
		assertEquals("AJ should be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testGovernmentDenied_HitlerJa() {
		testGovernmentDenied(PartyMembership.FASCIST, SecretRole.HITLER, Vote.JA);
		
		assertEquals("AJ should be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testGovernmentDenied_HitlerNein() {
		testGovernmentDenied(PartyMembership.FASCIST, SecretRole.HITLER, Vote.NEIN);
		
		assertEquals("AJ should be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	private void testGovernmentDenied(PartyMembership myMembership, SecretRole myRole, Vote myVote) {
		String[] args = {};
		notification.setAction(new GameplayAction(Action.DENIED, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		myPlayer.setPartyMembership(myMembership);
		myPlayer.setSecretRole(myRole);
		myPlayer.setVote(myVote);
		aj.setVote(Vote.JA);
		sean.setVote(Vote.NEIN);
		gameData.setPlayers(Arrays.asList(aj, sean, fascist, fascist2, liberal, liberal2, myPlayer));
		
		processor.getActionToTake(notification);
	}
	
	@Test
	public void testLiberalPolicyPlayed() {
		String[] args = {};
		notification.setAction(new GameplayAction(Action.LIBERAL_POLICY, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		aj.setVote(Vote.JA);
		sean.setVote(Vote.NEIN);
		gameData.setPlayers(Arrays.asList(aj, sean, deadPlayer, fascist, liberal));
		
		processor.getActionToTake(notification);
		
		assertEquals("AJ should be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testFascistPolicyPlayed() {
		String[] args = {};
		notification.setAction(new GameplayAction(Action.FASCIST_POLICY, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		aj.setVote(Vote.JA);
		sean.setVote(Vote.NEIN);
		gameData.setPlayers(Arrays.asList(aj, sean, deadPlayer, fascist, liberal));
		
		processor.getActionToTake(notification);
		
		assertEquals("AJ should be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}
}
