package com.secrethitler.ai.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.Action;
import com.secrethitler.ai.enums.GamePhase;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.Policy;
import com.secrethitler.ai.enums.SecretRole;
import com.secrethitler.ai.enums.Vote;

public abstract class AbstractDeductionGameplayProcessorTest extends SimpleGameplayProcessorTest {
	protected static enum Scenario {
		VOTE_LIBERAL_BOT_ONE_SUSPECTED_FASCIST,
		VOTE_HITLER_BOT_OPPOSITE_SUSPECTED_MEMBERSHIPS,
		VOTE_LIBERAL_BOT_SUSPECTED_OPPOSITE_MEMBERSHIPS_KNOWN_FASCIST,
		VOTE_LIBERAL_BOT_TWO_KNOWN_OPPOSITES,
		VOTE_LIBERAL_BOT_ALL_UNKNOWN,
		GOVERNMENT_DENIED_LIBERAL_JA,
		GOVERNMENT_DENIED_LIBERAL_NEIN,
		GOVERNMENT_DENIED_HITLER_JA,
		GOVERNMENT_DENIED_HITLER_NEIN,
		ANARCHY,
		POLICY_PLAYED_NO_POLICY_OPTION,
		POLICY_PLAYED_WITH_NEIN_VOTER,
		POLICY_PLAYED_VETO_REQUESTOR,
		POLICY_PLAYED_CHANCELLOR_NO_VETO,
		POLICY_PLAYED_PRESIDENT_FAILED_VETO,
		POLICY_PLAYED_PRESIDENT_NO_VETO,
		SPECIAL_ELECTION_CHOSEN_SUSPECTED_LIBERAL,
		SPECIAL_ELECTION_CHOSEN_SUSPECTED_FASCIST,
		PRESIDENT_VETOED_KNOWN_LIBERAL,
		PRESIDENT_VETOED_KNOWN_FASCIST,
		PRESIDENT_VETOED_SUSPECTED_LIBERAL,
		PRESIDENT_VETOED_SUSPECTED_FASCIST,
		PRESIDENT_VETOED_OPPOSITE_KNOWN_MEMBERSHIPS,
		PRESIDENT_VETOED_OPPOSITE_SUSPECTED_MEMBERSHIPS,
		PLAYER_KILLED_SUSPECTED_FASCIST,
		PLAYER_KILLED_SUSPECTED_LIBERAL,
		PLAYER_KILLED_UNKNOWN_KILLER_UNKNOWN_KILLED,
		PLAYER_KILLED_SUSPECTED_LIBERAL_KILLER_UNKNOWN_KILLED,
		PLAYER_KILLED_KNOWN_LIBERAL_KILLER,
		PRESIDENT_VETO_LIBERAL_ALL_FASCIST,
		PRESIDENT_VETO_LIBERAL_ALL_LIBERAL,
		PRESIDENT_VETO_FASCIST_ALL_FASCIST,
		PRESIDENT_VETO_FASCIST_ALL_LIBERAL,
		PRESIDENT_VETO_MIXED,
		PRESIDENT_VETO_KNOWN_LIBERAL_MIXED,
		PRESIDENT_VETO_KNOWN_FASCIST_ALL_FASCIST,
		PLAYER_KILLED_PRESIDENT,
		PRESIDENT_VETOED_PRESIDENT,
	}
	
	private AbstractDeductionGameplayProcessor deductionProcessor;
	private PlayerData aj;
	private PlayerData sean;
	
	@Before
	public void setUp() {
		super.setUp();
		deductionProcessor = getDeductionProcessor();
		processor = deductionProcessor;
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
	public void testChooseRunningMate_LiberalSuspectedMembers() {
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, unknown, fascist, unknown2, fascist2));
		deductionProcessor.updateSuspectedMembership(unknown, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(unknown2, PartyMembership.FASCIST, null, null);
		String[] args = {"3"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(unknown))).thenReturn(unknown);
		
		testChooseRunningMate_Unknowns(args, PartyMembership.LIBERAL, SecretRole.LIBERAL);
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(unknown));
	}
	
	@Test
	public void testChooseRunningMate_HitlerSuspectedMembers() {
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, unknown, liberal, unknown2, liberal2));
		deductionProcessor.updateSuspectedMembership(unknown, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(unknown2, PartyMembership.FASCIST, null, null);
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
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		
		testVoteHelper(Vote.NEIN);
		
		verifyExpectedSuspicions(Scenario.VOTE_LIBERAL_BOT_ONE_SUSPECTED_FASCIST);
	}

	@Test
	public void testVote_HitlerBotOppositeSuspectedMemberships() {
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		myPlayer.setSecretRole(SecretRole.HITLER);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		
		testVoteHelper(Vote.JA, Arrays.asList(aj, sean, unknown, unknown, unknown, unknown, unknown));
		
		verifyExpectedSuspicions(Scenario.VOTE_HITLER_BOT_OPPOSITE_SUSPECTED_MEMBERSHIPS);
	}
	
	@Test
	public void testVote_LiberalBotSuspectedOppositeMembershipsKnownFascist() {
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		aj.setPartyMembership(PartyMembership.FASCIST);
		
		testVoteHelper(Vote.NEIN);
		
		assertEquals("AJ should still be a known Fascist", PartyMembership.FASCIST, aj.getPartyMembership());
		assertEquals("Sean should still not be a known Fascist", PartyMembership.UNKNOWN, sean.getPartyMembership());
		verifyExpectedSuspicions(Scenario.VOTE_LIBERAL_BOT_SUSPECTED_OPPOSITE_MEMBERSHIPS_KNOWN_FASCIST);
	}
	
	@Test
	public void testVote_LiberalBotTwoKnownOpposites() {
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		aj.setPartyMembership(PartyMembership.FASCIST);
		sean.setPartyMembership(PartyMembership.LIBERAL);
		
		testVoteHelper(Vote.NEIN);
		
		assertEquals("AJ should still be a known Fascist", PartyMembership.FASCIST, aj.getPartyMembership());
		assertEquals("Sean should still be a known Liberal", PartyMembership.LIBERAL, sean.getPartyMembership());
		verifyExpectedSuspicions(Scenario.VOTE_LIBERAL_BOT_TWO_KNOWN_OPPOSITES);
	}

	@Test
	public void testVote_LiberalBotAllUnknown() {	
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		testVoteHelper(Vote.JA);
		
		verifyExpectedSuspicions(Scenario.VOTE_LIBERAL_BOT_ALL_UNKNOWN);
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
		aj.setPresident(true);
		sean.setPartyMembership(PartyMembership.FASCIST);
		sean.setChancellor(true);
		GameData gameData = new GameData();
		gameData.setPlayers(Arrays.asList(new PlayerData(), new PlayerData(), new PlayerData(), new PlayerData(), new PlayerData()));
		
		assertTrue("The AI should vote ja.", deductionProcessor.isVoteJa(PartyMembership.FASCIST, SecretRole.HITLER, gameData));
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
		
		verifyExpectedSuspicions(Scenario.GOVERNMENT_DENIED_LIBERAL_JA);
	}
	
	@Test
	public void testGovernmentDenied_LiberalNein() {
		testGovernmentDenied(PartyMembership.LIBERAL, SecretRole.LIBERAL, Vote.NEIN);
		
		verifyExpectedSuspicions(Scenario.GOVERNMENT_DENIED_LIBERAL_NEIN);
	}
	
	@Test
	public void testGovernmentDenied_HitlerJa() {
		testGovernmentDenied(PartyMembership.FASCIST, SecretRole.HITLER, Vote.JA);
		
		verifyExpectedSuspicions(Scenario.GOVERNMENT_DENIED_HITLER_JA);
		assertTrue("policyOptionsForNextGovernment should remain empty", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
	}
	
	@Test
	public void testGovernmentDenied_HitlerNein() {
		deductionProcessor.policyOptionsForNextGovernment = Arrays.asList(Policy.FASCIST, Policy.FASCIST, Policy.FASCIST);
		testGovernmentDenied(PartyMembership.FASCIST, SecretRole.HITLER, Vote.NEIN);
		
		verifyExpectedSuspicions(Scenario.GOVERNMENT_DENIED_HITLER_NEIN);
		assertFalse("policyOptionsForNextGovernment should remain", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
	}
	
	@Test
	public void testAnarchy() {
		deductionProcessor.policyOptionsForNextGovernment = new ArrayList<>(Arrays.asList(Policy.FASCIST, Policy.FASCIST, Policy.FASCIST));
		
		testGovernmentDenied(PartyMembership.LIBERAL, SecretRole.LIBERAL, Vote.JA, Action.ANARCHY);
		
		verifyExpectedSuspicions(Scenario.ANARCHY);
		assertTrue("policyOptionsForNextGovernment should be reset", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
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
		deductionProcessor.policyOptionsForNextGovernment = new ArrayList<>(Arrays.asList(Policy.FASCIST, Policy.FASCIST, Policy.FASCIST));
		String[] args = {};
		notification.setAction(new GameplayAction(Action.LIBERAL_POLICY, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		aj.setVote(Vote.JA);
		sean.setVote(Vote.NEIN);
		gameData.setPlayers(Arrays.asList(aj, sean, deadPlayer, fascist, liberal));
		
		processor.getActionToTake(notification);
		
		verifyExpectedSuspicions(Scenario.POLICY_PLAYED_NO_POLICY_OPTION);
		assertTrue("policyOptionForNextGovernment flag should be reset", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
	}
	
	@Test
	public void testPolicyPlayed_VetoRequestor() {
		deductionProcessor.vetoRequestor = Optional.of("Sean");
		deductionProcessor.previousChancellor = "Sean";
		String[] args = {};
		notification.setAction(new GameplayAction(Action.FASCIST_POLICY, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		aj.setVote(Vote.JA);
		sean.setVote(Vote.JA);
		gameData.setPlayers(Arrays.asList(aj, sean, deadPlayer, fascist, liberal));
		deductionProcessor.previousPresident = "Fascist";
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		
		processor.getActionToTake(notification);
		
		verifyExpectedSuspicions(Scenario.POLICY_PLAYED_VETO_REQUESTOR);
		assertTrue("policyOptionsForNextGovernment should remain empty", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
		assertEquals("The vetoRequestor should be reset", Optional.empty(), deductionProcessor.vetoRequestor);
	}
	
	@Test
	public void testPolicyPlayed_ChancellorNoVeto() {
		deductionProcessor.previousChancellor = "Sean";
		String[] args = {};
		notification.setAction(new GameplayAction(Action.FASCIST_POLICY, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		aj.setVote(Vote.JA);
		sean.setVote(Vote.JA);
		gameData.setPlayers(Arrays.asList(aj, sean, deadPlayer, fascist, liberal));
		deductionProcessor.previousPresident = "Fascist";
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		
		processor.getActionToTake(notification);
		
		verifyExpectedSuspicions(Scenario.POLICY_PLAYED_CHANCELLOR_NO_VETO);
		assertTrue("policyOptionsForNextGovernment should remain empty", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
		assertEquals("The vetoRequestor should be reset", Optional.empty(), deductionProcessor.vetoRequestor);
	}
	
	@Test
	public void testPolicyPlayed_PresidentFailedVeto() {
		deductionProcessor.previousPresident = "Sean";
		deductionProcessor.vetoRequestor = Optional.of("Fascist");
		String[] args = {};
		notification.setAction(new GameplayAction(Action.FASCIST_POLICY, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		aj.setVote(Vote.JA);
		sean.setVote(Vote.JA);
		gameData.setPlayers(Arrays.asList(aj, sean, deadPlayer, fascist, liberal));
		deductionProcessor.previousChancellor = "Fascist";
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		
		processor.getActionToTake(notification);
		
		verifyExpectedSuspicions(Scenario.POLICY_PLAYED_PRESIDENT_FAILED_VETO);
		assertTrue("policyOptionsForNextGovernment should remain empty", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
		assertEquals("The vetoRequestor should be reset", Optional.empty(), deductionProcessor.vetoRequestor);
	}
	
	@Test
	public void testPolicyPlayed_PresidentNoVeto() {
		deductionProcessor.previousPresident = "Sean";
		String[] args = {};
		notification.setAction(new GameplayAction(Action.FASCIST_POLICY, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		aj.setVote(Vote.JA);
		sean.setVote(Vote.JA);
		gameData.setPlayers(Arrays.asList(aj, sean, deadPlayer, fascist, liberal));
		deductionProcessor.previousChancellor = "Fascist";
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(fascist, PartyMembership.FASCIST, null, null);
		
		processor.getActionToTake(notification);
		
		verifyExpectedSuspicions(Scenario.POLICY_PLAYED_PRESIDENT_NO_VETO);
		assertTrue("policyOptionsForNextGovernment should remain empty", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
		assertEquals("The vetoRequestor should be reset", Optional.empty(), deductionProcessor.vetoRequestor);
	}
	
	@Test
	public void testPolicyPlayedWithNeinVoter() {
		String[] args = {};
		notification.setAction(new GameplayAction(Action.LIBERAL_POLICY, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		deductionProcessor.previousPresident = "Liberal";
		deductionProcessor.updateSuspectedMembership(liberal, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(liberal, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(liberal, PartyMembership.LIBERAL, null, null);
		deductionProcessor.previousChancellor = "Liberal2";
		deductionProcessor.updateSuspectedMembership(liberal2, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(liberal2, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(liberal2, PartyMembership.LIBERAL, null, null);
		
		aj.setVote(Vote.JA);
		sean.setVote(Vote.NEIN);
		gameData.setPlayers(Arrays.asList(aj, sean, deadPlayer, fascist, liberal));
		
		processor.getActionToTake(notification);
		
		verifyExpectedSuspicions(Scenario.POLICY_PLAYED_WITH_NEIN_VOTER);
		assertTrue("policyOptionForNextGovernment flag should remain empty", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
	}
	
	@Override
	@Test
	public void testExamine_President() {
		super.testExamine_President();
		
		assertFalse("The next government has a choice for their policy", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
	}
	
	@Test
	public void testExamine_AllFascist() {
		testExamine_President(Arrays.asList(Policy.FASCIST, Policy.FASCIST, Policy.FASCIST));
		
		assertFalse("The next government does not have a choice for their policy", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
	}
	
	@Test
	public void testExamine_AllLiberal() {		
		testExamine_President(Arrays.asList(Policy.LIBERAL, Policy.LIBERAL, Policy.LIBERAL));
		
		assertFalse("The next government does not have a choice for their policy", deductionProcessor.policyOptionsForNextGovernment.isEmpty());
	}
	
	@Test
	public void testKill_LiberalSuspectedFascist() {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		gameData.setPlayers(Arrays.asList(myPlayer, deadPlayer, aj, unknown, unknown2, sean, unknown));
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
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
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		String[] args = {"Sean"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(sean))).thenReturn(sean);
		
		testKill(Optional.of(new GameplayAction(Action.KILL_PLAYER, args)));
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(sean));
	}
	
	@Test
	public void testPlayerKilled_President() {
		gameData.setMyPlayer(sean);
		
		testPlayerKilled();
		
		verifyExpectedSuspicions(Scenario.PLAYER_KILLED_PRESIDENT);
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
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		
		testPlayerKilled();
		
		verifyExpectedSuspicions(Scenario.PLAYER_KILLED_SUSPECTED_FASCIST);
	}
	
	@Test
	public void testPlayerKilled_SuspectedLiberal() {
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.LIBERAL, null, null);
		
		testPlayerKilled();
		
		verifyExpectedSuspicions(Scenario.PLAYER_KILLED_SUSPECTED_LIBERAL);
	}
	
	@Test
	public void testPlayerKilled_UnknownKillerUnknownKilled() {
		testPlayerKilled();
		
		verifyExpectedSuspicions(Scenario.PLAYER_KILLED_UNKNOWN_KILLER_UNKNOWN_KILLED);
	}
	
	@Test
	public void testPlayerKilled_SuspectedLiberalKillerUnknownKilled() {
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		
		testPlayerKilled();
		
		verifyExpectedSuspicions(Scenario.PLAYER_KILLED_SUSPECTED_LIBERAL_KILLER_UNKNOWN_KILLED);
	}
	
	@Test
	public void testPlayerKilled_KnownLiberalKiller() {
		sean.setPartyMembership(PartyMembership.LIBERAL);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.LIBERAL, null, null);
		
		testPlayerKilled();
		
		verifyExpectedSuspicions(Scenario.PLAYER_KILLED_KNOWN_LIBERAL_KILLER);
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
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETO_LIBERAL_ALL_FASCIST);
	}
	
	@Override
	@Test
	public void testPresidentVeto_LiberalAllLiberal() {
		super.testPresidentVeto_LiberalAllLiberal();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETO_LIBERAL_ALL_LIBERAL);
	}
	
	@Override
	@Test
	public void testPresidentVeto_FascistAllFascist() {
		super.testPresidentVeto_FascistAllFascist();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETO_FASCIST_ALL_FASCIST);
	}
	
	@Override
	@Test
	public void testPresidentVeto_FascistAllLiberal() {
		super.testPresidentVeto_FascistAllLiberal();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETO_FASCIST_ALL_LIBERAL);
	}
	
	@Override
	@Test
	public void testPresidentVeto_Mixed() {
		super.testPresidentVeto_Mixed();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETO_MIXED);
	}
	
	@Test
	public void testPresidentVeto_KnownLiberalMixed() {
		aj.setPartyMembership(PartyMembership.LIBERAL);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.LIBERAL, null, null);
		
		super.testPresidentVeto_Mixed();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETO_KNOWN_LIBERAL_MIXED);
	}
	
	@Test
	public void testPresidentVeto_KnownFascistAllFascist() {
		aj.setPartyMembership(PartyMembership.FASCIST);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		
		super.testPresidentVeto_LiberalAllFascist();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETO_KNOWN_FASCIST_ALL_FASCIST);
	}
	
	@Override
	protected void testPresidentVeto(Optional<GameplayAction> expectedResult) {
		aj.setChancellor(true);
		gameData.setPlayers(Arrays.asList(sean, aj));
		
		super.testPresidentVeto(expectedResult);
	}
	
	@Test
	public void testInvestigate_SuspectedFascsist() {
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		when(randomUtil.getRandomItemFromList(Arrays.asList(aj))).thenReturn(aj);
		
		testInvestigate_President(PartyMembership.LIBERAL, Arrays.asList(sean, aj), "AJ");
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(aj));
	}
	
	@Test
	public void testInvestigate_SuspectedLiberalAndUnknown() {
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.UNKNOWN, null, null);
		when(randomUtil.getRandomItemFromList(Arrays.asList(aj))).thenReturn(aj);
		
		testInvestigate_President(PartyMembership.LIBERAL, Arrays.asList(sean, aj), "AJ");
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(aj));
	}
	
	@Test
	public void testInvestigate_AllSuspectedLiberals() {
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.LIBERAL, null, null);
		when(randomUtil.getRandomItemFromList(Arrays.asList(sean, aj))).thenReturn(sean);
		
		testInvestigate_President(PartyMembership.LIBERAL, Arrays.asList(sean, aj), "Sean");
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(sean, aj));
	}
	
	@Test
	public void testSpecialElectionChosen_SuspectedLiberal() {
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		
		testSpecialElectionChosen();
		
		verifyExpectedSuspicions(Scenario.SPECIAL_ELECTION_CHOSEN_SUSPECTED_LIBERAL);
	}
	
	@Test
	public void testSpecialElectionChosen_SuspectedFascist() {
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.FASCIST, null, null);
		
		testSpecialElectionChosen();
		
		verifyExpectedSuspicions(Scenario.SPECIAL_ELECTION_CHOSEN_SUSPECTED_FASCIST);
	}
	
	protected void testSpecialElectionChosen() {
		String[] args = {"Sean", "AJ"};
		notification.setAction(new GameplayAction(Action.CHOOSE_NEXT_PRESIDENTIAL_CANDIDATE, args));
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		gameData.setPlayers(Arrays.asList(sean, aj));
		
		processor.getActionToTake(notification);
	}
	
	@Test
	public void testChooseNextPresidentialCandidate_LiberalSuspectedLiberal() {
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		when(randomUtil.getRandomItemFromList(Arrays.asList(sean))).thenReturn(sean);
		
		testChooseNextPresidentialCandidate_President(PartyMembership.LIBERAL, Arrays.asList(aj, sean), 1);
	
		verify(randomUtil).getRandomItemFromList(Arrays.asList(sean));
	}
	
	@Test
	public void testChooseNextPresidentialCandidate_FascistSuspectedFascist() {
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
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
		
		assertEquals(Optional.of("Sean"), deductionProcessor.vetoRequestor);
	}
	
	@Test
	public void testPresidentVetoed_President() {
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		gameData.setMyPlayer(aj);
		
		testPresidentVetoed();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETOED_PRESIDENT);
	}
	
	@Test
	public void testPresidentVetoed_KnownLiberal() {
		sean.setPartyMembership(PartyMembership.LIBERAL);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		
		testPresidentVetoed();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETOED_KNOWN_LIBERAL);
	}
	
	@Test
	public void testPresidentVetoed_KnownFascist() {
		aj.setPartyMembership(PartyMembership.FASCIST);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		
		testPresidentVetoed();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETOED_KNOWN_FASCIST);
	}
	
	@Test
	public void testPresidentVetoed_SuspectedLiberal() {
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		
		testPresidentVetoed();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETOED_SUSPECTED_LIBERAL);
	}
	
	@Test
	public void testPresidentVetoed_SuspectedFascist() {
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		
		testPresidentVetoed();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETOED_SUSPECTED_FASCIST);
	}
	
	@Test
	public void testPresidentVetoed_OppositeKnownMemberships() {
		sean.setPartyMembership(PartyMembership.LIBERAL);
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		aj.setPartyMembership(PartyMembership.FASCIST);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		
		testPresidentVetoed();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETOED_OPPOSITE_KNOWN_MEMBERSHIPS);
	}
	
	@Test
	public void testPresidentVetoed_OppositeSuspectedMemberships() {
		deductionProcessor.updateSuspectedMembership(sean, PartyMembership.LIBERAL, null, null);
		deductionProcessor.updateSuspectedMembership(aj, PartyMembership.FASCIST, null, null);
		
		testPresidentVetoed();
		
		verifyExpectedSuspicions(Scenario.PRESIDENT_VETOED_OPPOSITE_SUSPECTED_MEMBERSHIPS);
	}
	
	private void testPresidentVetoed() {
		deductionProcessor.previousPresident = "Sean";
		deductionProcessor.previousChancellor = "AJ";
		deductionProcessor.vetoRequestor = Optional.of("Sean");
		String[] args = {"AJ"};
		notification.setAction(new GameplayAction(Action.PRESIDENT_VETO_YES, args));
		gameData.setPlayers(Arrays.asList(aj, sean));
		
		processor.getActionToTake(notification);
	}
	
	@Test
	public void testGovernmentElected_NonDangerZone() {
		testGovernmentElected();
		
		assertEquals("There are no proven non-hitlers", Collections.emptySet(), deductionProcessor.provenNonHitlers);
	}
	
	@Test
	public void testGovernmentElected_DangerZone() {
		gameData.setFascistDangerZone(true);
		testGovernmentElected();
		
		assertEquals("Sean is a proven non-hitler", new HashSet<>(Arrays.asList(sean.getUsername())), deductionProcessor.provenNonHitlers);
	}
	
	private void testGovernmentElected() {
		sean.setChancellor(true);
		gameData.setPlayers(Arrays.asList(sean, aj));
		String[] args = {};
		notification.setAction(new GameplayAction(Action.SHUSH, args));
		
		processor.getActionToTake(notification);
	}
	
	private void verifyExpectedSuspicions(Scenario scenario) {
		Pair<Integer, Integer> expectedSuspicions = getExpectedSuspicions(scenario);
		assertEquals(expectedSuspicions.getLeft().intValue(), deductionProcessor.getMembershipSuspicion("AJ"));
		assertEquals(expectedSuspicions.getRight().intValue(), deductionProcessor.getMembershipSuspicion("Sean"));
	}
	
	protected abstract Pair<Integer, Integer> getExpectedSuspicions(Scenario scenario);
	
	protected abstract AbstractDeductionGameplayProcessor getDeductionProcessor();
}
