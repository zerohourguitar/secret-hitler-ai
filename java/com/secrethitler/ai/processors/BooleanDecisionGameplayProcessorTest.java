package com.secrethitler.ai.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
import com.secrethitler.ai.enums.Policy;
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
		assertTrue("policyOptionForNextGovernment flag should remain true", boolProcessor.policyOptionForNextGovernment);
	}
	
	@Test
	public void testGovernmentDenied_HitlerNein() {
		boolProcessor.policyOptionForNextGovernment = false;
		testGovernmentDenied(PartyMembership.FASCIST, SecretRole.HITLER, Vote.NEIN);
		
		assertEquals("AJ should be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
		assertFalse("policyOptionForNextGovernment flag should remain false", boolProcessor.policyOptionForNextGovernment);
	}
	
	@Test
	public void testAnarchy() {
		boolProcessor.policyOptionForNextGovernment = false;
		
		testGovernmentDenied(PartyMembership.LIBERAL, SecretRole.LIBERAL, Vote.JA, Action.ANARCHY);
		
		assertEquals("AJ should be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));	
		assertTrue("policyOptionForNextGovernment flag should be reset", boolProcessor.policyOptionForNextGovernment);
	}
	
	private void testGovernmentDenied(PartyMembership myMembership, SecretRole myRole, Vote myVote) {
		testGovernmentDenied(myMembership, myRole, myVote, Action.DENIED);
	}
	
	private void testGovernmentDenied(PartyMembership myMembership, SecretRole myRole, Vote myVote, Action action) {
		String[] args = {};
		notification.setAction(new GameplayAction(action, args));
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
	public void testPolicyPlayed_NoPolicyOption() {
		boolProcessor.policyOptionForNextGovernment = false;
		String[] args = {};
		notification.setAction(new GameplayAction(Action.LIBERAL_POLICY, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		aj.setVote(Vote.JA);
		sean.setVote(Vote.NEIN);
		gameData.setPlayers(Arrays.asList(aj, sean, deadPlayer, fascist, liberal));
		
		processor.getActionToTake(notification);
		
		assertEquals("AJ should still be unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should still be unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("Sean"));
		assertTrue("policyOptionForNextGovernment flag should be reset", boolProcessor.policyOptionForNextGovernment);
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
		assertTrue("policyOptionForNextGovernment flag should remain true", boolProcessor.policyOptionForNextGovernment);
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
		assertTrue("policyOptionForNextGovernment flag should remain true", boolProcessor.policyOptionForNextGovernment);
	}
	
	@Test
	public void testPolicyPlayed_VetoRequestor() {
		boolProcessor.vetoRequestor = Optional.of("Sean");
		String[] args = {};
		notification.setAction(new GameplayAction(Action.FASCIST_POLICY, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		aj.setVote(Vote.JA);
		sean.setVote(Vote.JA);
		gameData.setPlayers(Arrays.asList(aj, sean, deadPlayer, fascist, liberal));
		
		processor.getActionToTake(notification);
		
		assertEquals("AJ should be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
		assertTrue("policyOptionForNextGovernment flag should remain true", boolProcessor.policyOptionForNextGovernment);
		assertEquals("The vetoRequestor should be reset", Optional.empty(), boolProcessor.vetoRequestor);
	}
	
	@Override
	@Test
	public void testExamine_President() {
		super.testExamine_President();
		
		assertTrue("The next government has a choice for their policy", boolProcessor.policyOptionForNextGovernment);
	}
	
	@Test
	public void testExamine_AllFascist() {
		testExamine_President(Arrays.asList(Policy.FASCIST, Policy.FASCIST, Policy.FASCIST));
		
		assertFalse("The next government does not have a choice for their policy", boolProcessor.policyOptionForNextGovernment);
	}
	
	@Test
	public void testExamine_AllLiberal() {		
		testExamine_President(Arrays.asList(Policy.LIBERAL, Policy.LIBERAL, Policy.LIBERAL));
		
		assertFalse("The next government does not have a choice for their policy", boolProcessor.policyOptionForNextGovernment);
	}
	
	@Test
	public void testKill_LiberalSuspectedFascist() {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		gameData.setPlayers(Arrays.asList(myPlayer, deadPlayer, aj, unknown, unknown2, sean, unknown));
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		String[] args = {"AJ"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(aj))).thenReturn(aj);
		
		testKill(Optional.of(new GameplayAction(Action.KILL_PLAYER, args)));
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(aj));
	}
	
	@Test
	public void testKill_HitlerSuspectedLiberal() {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		myPlayer.setSecretRole(SecretRole.HITLER);
		gameData.setPlayers(Arrays.asList(myPlayer, deadPlayer, aj, unknown, unknown2, sean, unknown));
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		String[] args = {"Sean"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(sean))).thenReturn(sean);
		
		testKill(Optional.of(new GameplayAction(Action.KILL_PLAYER, args)));
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(sean));
	}
	
	@Test
	public void testPlayerKilled_President() {
		gameData.setMyPlayer(sean);
		
		testPlayerKilled();
		
		assertEquals("Sean should not guess about his own party membership", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testPlayerKilled_PlayerNotFound() {
		try { 
			testPlayerKilled(Arrays.asList(aj, unknown));
			fail("Expected an IllegalStateException to be thrown if the killer's name is not found in the game");
		} catch (IllegalStateException e) {
			assertEquals("Killer not found!", e.getMessage());
		}
	}
	
	@Test
	public void testPlayerKilled_SuspectedFascist() {
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		
		testPlayerKilled();
		
		assertEquals("Sean should be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testPlayerKilled_SuspectedLiberal() {
		boolProcessor.setSuspectedMembership(aj, PartyMembership.LIBERAL);
		
		testPlayerKilled();
		
		assertEquals("Sean should be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testPlayerKilled_UnknownKillerUnknownKilled() {
		testPlayerKilled();
		
		assertEquals("Sean should remain unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testPlayerKilled_SuspectedLiberalKillerUnknownKilled() {
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		
		testPlayerKilled();
		
		assertEquals("Sean should remain a suspected liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testPlayerKilled_KnownLiberalKiller() {
		sean.setPartyMembership(PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.LIBERAL);
		
		testPlayerKilled();
		
		assertEquals("Sean should remain a known liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	private void testPlayerKilled() {
		testPlayerKilled(Arrays.asList(sean, aj));
	}
	
	private void testPlayerKilled(List<PlayerData> players) {
		String[] args = {"Sean", "AJ"};
		notification.setAction(new GameplayAction(Action.KILL_PLAYER, args));
		gameData.setPlayers(players);
		
		processor.getActionToTake(notification);
	}
	
	@Override
	@Test
	public void testPresidentVeto_LiberalAllFascist() {
		super.testPresidentVeto_LiberalAllFascist();
		
		assertEquals("AJ should now be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("AJ"));
	}
	
	@Override
	@Test
	public void testPresidentVeto_LiberalAllLiberal() {
		super.testPresidentVeto_LiberalAllLiberal();
		
		assertEquals("AJ should now be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
	}
	
	@Override
	@Test
	public void testPresidentVeto_FascistAllFascist() {
		super.testPresidentVeto_FascistAllFascist();
		
		assertEquals("AJ should now be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("AJ"));
	}
	
	@Override
	@Test
	public void testPresidentVeto_FascistAllLiberal() {
		super.testPresidentVeto_FascistAllLiberal();
		
		assertEquals("AJ should now be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
	}
	
	@Override
	@Test
	public void testPresidentVeto_Mixed() {
		super.testPresidentVeto_Mixed();
		
		assertEquals("AJ should remain unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("AJ"));
	}
	
	@Test
	public void testPresidentVeto_KnownLiberalMixed() {
		aj.setPartyMembership(PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.LIBERAL);
		
		super.testPresidentVeto_Mixed();
		
		assertEquals("AJ should remain a known liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("AJ"));
	}
	
	@Test
	public void testPresidentVeto_KnownFascistAllFascist() {
		aj.setPartyMembership(PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		
		super.testPresidentVeto_LiberalAllFascist();
		
		assertEquals("AJ should remain a known fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
	}
	
	@Override
	protected void testPresidentVeto(Optional<GameplayAction> expectedResult) {
		aj.setChancellor(true);
		gameData.setPlayers(Arrays.asList(sean, aj));
		
		super.testPresidentVeto(expectedResult);
	}
	
	@Test
	public void testInvestigate_SuspectedFascsist() {
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		when(randomUtil.getRandomItemFromList(Arrays.asList(aj))).thenReturn(aj);
		
		testInvestigate_President(PartyMembership.LIBERAL, Arrays.asList(sean, aj), "AJ");
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(aj));
	}
	
	@Test
	public void testInvestigate_SuspectedLiberalAndUnknown() {
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.UNKNOWN);
		when(randomUtil.getRandomItemFromList(Arrays.asList(aj))).thenReturn(aj);
		
		testInvestigate_President(PartyMembership.LIBERAL, Arrays.asList(sean, aj), "AJ");
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(aj));
	}
	
	@Test
	public void testInvestigate_AllSuspectedLiberals() {
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.LIBERAL);
		when(randomUtil.getRandomItemFromList(Arrays.asList(sean, aj))).thenReturn(sean);
		
		testInvestigate_President(PartyMembership.LIBERAL, Arrays.asList(sean, aj), "Sean");
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(sean, aj));
	}
	
	@Test
	public void testSpecialElectionChosen_SuspectedLiberal() {
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		
		testSpecialElectionChosen();
		
		assertEquals("AJ should now be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should still be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testSpecialElectionChosen_SuspectedFascist() {
		boolProcessor.setSuspectedMembership(sean, PartyMembership.FASCIST);
		
		testSpecialElectionChosen();
		
		assertEquals("AJ should now be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should still be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	private void testSpecialElectionChosen() {
		String[] args = {"Sean", "AJ"};
		notification.setAction(new GameplayAction(Action.CHOOSE_NEXT_PRESIDENTIAL_CANDIDATE, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		gameData.setPlayers(Arrays.asList(sean, aj));
		
		processor.getActionToTake(notification);
	}
	
	@Test
	public void testChooseNextPresidentialCandidate_LiberalSuspectedLiberal() {
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		when(randomUtil.getRandomItemFromList(Arrays.asList(sean))).thenReturn(sean);
		
		testChooseNextPresidentialCandidate_President(PartyMembership.LIBERAL, Arrays.asList(aj, sean), 1);
	
		verify(randomUtil).getRandomItemFromList(Arrays.asList(sean));
	}
	
	@Test
	public void testChooseNextPresidentialCandidate_FascistSuspectedFascist() {
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		when(randomUtil.getRandomItemFromList(Arrays.asList(aj))).thenReturn(aj);
		
		testChooseNextPresidentialCandidate_President(PartyMembership.FASCIST, Arrays.asList(aj, sean), 0);
	
		verify(randomUtil).getRandomItemFromList(Arrays.asList(aj));
	}
	
	@Test
	public void testChooseNextPresidentialCandidate_FascistWithHitler() {
		aj.setPartyMembership(PartyMembership.FASCIST);
		aj.setSecretRole(SecretRole.HITLER);
		sean.setPartyMembership(PartyMembership.FASCIST);
		sean.setSecretRole(SecretRole.FASCIST);
		when(randomUtil.getRandomItemFromList(Arrays.asList(sean))).thenReturn(sean);
		
		testChooseNextPresidentialCandidate_President(PartyMembership.FASCIST, Arrays.asList(aj, sean), 1);
	
		verify(randomUtil).getRandomItemFromList(Arrays.asList(sean));
	}
	
	@Test
	public void testChancellorVetoed() {
		String[] args = {"Sean"};
		notification.setAction(new GameplayAction(Action.CHANCELLOR_VETO, args));
		
		processor.getActionToTake(notification);
		
		assertEquals(Optional.of("Sean"), boolProcessor.vetoRequestor);
	}
	
	@Test
	public void testPresidentVetoed_President() {
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		gameData.setMyPlayer(aj);
		
		testPresidentVetoed();
		
		assertEquals("The president should not make any guess about himself", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("AJ"));
	}
	
	@Test
	public void testPresidentVetoed_KnownLiberal() {
		sean.setPartyMembership(PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		
		testPresidentVetoed();
		
		assertEquals("AJ should now be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should remain be a known Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testPresidentVetoed_KnownFascist() {
		aj.setPartyMembership(PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		
		testPresidentVetoed();
		
		assertEquals("AJ should remain a known Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testPresidentVetoed_SuspectedLiberal() {
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		
		testPresidentVetoed();
		
		assertEquals("AJ should now be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should remain be a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testPresidentVetoed_SuspectedFascist() {
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		
		testPresidentVetoed();
		
		assertEquals("AJ should remain a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testPresidentVetoed_OppositeKnownMemberships() {
		sean.setPartyMembership(PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		aj.setPartyMembership(PartyMembership.FASCIST);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		
		testPresidentVetoed();
		
		assertEquals("AJ should remain a suspected Fascist", PartyMembership.FASCIST, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should remain a suspected Liberal", PartyMembership.LIBERAL, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	@Test
	public void testPresidentVetoed_OppositeSuspectedMemberships() {
		boolProcessor.setSuspectedMembership(sean, PartyMembership.LIBERAL);
		boolProcessor.setSuspectedMembership(aj, PartyMembership.FASCIST);
		
		testPresidentVetoed();
		
		assertEquals("AJ should now be unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("AJ"));
		assertEquals("Sean should now be unknown", PartyMembership.UNKNOWN, boolProcessor.getSuspectedMembership("Sean"));
	}
	
	private void testPresidentVetoed() {
		boolProcessor.vetoRequestor = Optional.of("Sean");
		String[] args = {"AJ"};
		notification.setAction(new GameplayAction(Action.PRESIDENT_VETO_YES, args));
		gameData.setPlayers(Arrays.asList(aj, sean));
		
		processor.getActionToTake(notification);
	}
	
	@Test
	public void testGovernmentElected_NonDangerZone() {
		testGovernmentElected();
		
		assertEquals("There are no proven non-hitlers", Collections.emptySet(), boolProcessor.provenNonHitlers);
	}
	
	@Test
	public void testGovernmentElected_DangerZone() {
		gameData.setFascistDangerZone(true);
		testGovernmentElected();
		
		assertEquals("Sean is a proven non-hitler", new HashSet<>(Arrays.asList(sean.getUsername())), boolProcessor.provenNonHitlers);
	}
	
	private void testGovernmentElected() {
		sean.setChancellor(true);
		gameData.setPlayers(Arrays.asList(sean, aj));
		String[] args = {};
		notification.setAction(new GameplayAction(Action.SHUSH, args));
		
		processor.getActionToTake(notification);
	}
}
