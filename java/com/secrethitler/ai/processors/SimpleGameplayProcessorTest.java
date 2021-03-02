package com.secrethitler.ai.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.ParticipantGameNotification;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.Action;
import com.secrethitler.ai.enums.GamePhase;
import com.secrethitler.ai.enums.PartyMembership;
import com.secrethitler.ai.enums.Policy;
import com.secrethitler.ai.enums.SecretRole;
import com.secrethitler.ai.enums.Vote;
import com.secrethitler.ai.utils.RandomUtil;

@RunWith(MockitoJUnitRunner.class)
public class SimpleGameplayProcessorTest {
	@Mock
	protected RandomUtil randomUtil;
	
	protected SimpleGameplayProcessor processor;
	protected ParticipantGameNotification notification;
	protected GameData gameData;
	protected PlayerData myPlayer;
	protected PlayerData previousGovernmentMember;
	protected PlayerData deadPlayer;
	protected PlayerData hitler;
	protected PlayerData fascist;
	protected PlayerData fascist2;
	protected PlayerData liberal;
	protected PlayerData liberal2;
	protected PlayerData unknown;
	protected PlayerData unknown2;
	
	@Before
	public void setUp() {
		processor = new SimpleGameplayProcessor("Robot 1", randomUtil);
		notification = new ParticipantGameNotification();
		gameData = new GameData();
		notification.setGameData(gameData);
		myPlayer = new PlayerData();
		myPlayer.setUsername("Robot 1");
		myPlayer.setAlive(true);
		gameData.setMyPlayer(myPlayer);
		previousGovernmentMember = new PlayerData();
		previousGovernmentMember.setPreviousGovernmentMember(true);
		previousGovernmentMember.setAlive(true);
		deadPlayer = new PlayerData();
		hitler = new PlayerData();
		hitler.setAlive(true);
		hitler.setPartyMembership(PartyMembership.FASCIST);
		hitler.setSecretRole(SecretRole.HITLER);
		fascist = new PlayerData();
		fascist.setUsername("Fascist");
		fascist.setAlive(true);
		fascist.setPartyMembership(PartyMembership.FASCIST);
		fascist.setSecretRole(SecretRole.FASCIST);
		fascist2 = new PlayerData();
		fascist2.setUsername("Fascist2");
		fascist2.setAlive(true);
		fascist2.setPartyMembership(PartyMembership.FASCIST);
		fascist2.setSecretRole(SecretRole.FASCIST);
		liberal = new PlayerData();
		liberal.setUsername("Liberal");
		liberal.setAlive(true);
		liberal.setPartyMembership(PartyMembership.LIBERAL);
		liberal.setSecretRole(SecretRole.LIBERAL);
		liberal2 = new PlayerData();
		liberal2.setUsername("Liberal2");
		liberal2.setAlive(true);
		liberal2.setPartyMembership(PartyMembership.LIBERAL);
		liberal2.setSecretRole(SecretRole.LIBERAL);
		unknown = new PlayerData();
		unknown.setUsername("Unknown");
		unknown.setAlive(true);
		unknown.setPartyMembership(PartyMembership.UNKNOWN);
		unknown.setSecretRole(SecretRole.UNKNOWN);
		unknown2 = new PlayerData();
		unknown2.setUsername("Unknown2");
		unknown2.setAlive(true);
		unknown2.setPartyMembership(PartyMembership.UNKNOWN);
		unknown2.setSecretRole(SecretRole.UNKNOWN);
	}
	
	@Test
	public void testUnmappedPhase() {
		gameData.setPhase(GamePhase.GAME_OVER);
		
		Optional<GameplayAction> result = processor.getActionToTake(notification);
		
		assertEquals(Optional.empty(), result);
	}
	
	@Test
	public void testChooseRunningMate_NotPresident() {
		testChooseRunningMate(Optional.empty());
	}
	
	@Test
	public void testChooseRunningMate_FascistPresidentWithHitlerDangerZone() {
		testChooseRunningMate_FascistPresidentWithHitler(true, 3, hitler);
	}
	
	@Test
	public void testChooseRunningMate_FascistPresidentWithHitlerNotDangerZone() {
		testChooseRunningMate_FascistPresidentWithHitler(false, 5, fascist);
	}
	
	private void testChooseRunningMate_FascistPresidentWithHitler(final boolean dangerZone, final int expectedIndex, final PlayerData expectedChosenPlayer) {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		myPlayer.setSecretRole(SecretRole.FASCIST);
		gameData.setFascistDangerZone(dangerZone);
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, hitler, liberal, fascist));
		String[] args = {Integer.toString(expectedIndex)};
		when(randomUtil.getRandomItemFromList(Arrays.asList(expectedChosenPlayer))).thenReturn(expectedChosenPlayer);
		
		testChooseRunningMate(Optional.of(new GameplayAction(Action.CHOOSE_RUNNING_MATE, args)));
	
		verify(randomUtil).getRandomItemFromList(Arrays.asList(expectedChosenPlayer));
	}
	
	@Test
	public void testChooseRunningMate_FascistPresidentNoHitler() {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		myPlayer.setSecretRole(SecretRole.FASCIST);
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, liberal, fascist, fascist2));
		String[] args = {"4"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(fascist, fascist2))).thenReturn(fascist);
		
		testChooseRunningMate(Optional.of(new GameplayAction(Action.CHOOSE_RUNNING_MATE, args)));
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(fascist, fascist2));
	}
	
	@Test
	public void testChooseRunningMate_LiberalPresidentKnownLiberal() {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, unknown, liberal, fascist, fascist2));
		String[] args = {"4"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(liberal))).thenReturn(liberal);
		
		testChooseRunningMate(Optional.of(new GameplayAction(Action.CHOOSE_RUNNING_MATE, args)));
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(liberal));
	}
	
	@Test
	public void testChooseRunningMate_LiberalPresidentAllFascist() {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, fascist, fascist2));
		String[] args = {"3"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(fascist, fascist2))).thenReturn(fascist);
		
		testChooseRunningMate(Optional.of(new GameplayAction(Action.CHOOSE_RUNNING_MATE, args)));
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(fascist, fascist2));
	}
	
	@Test
	public void testChooseRunningMate_LiberalPresidentUnknowns() {
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, unknown, fascist, unknown2, fascist2));
		when(randomUtil.getRandomItemFromList(Arrays.asList(unknown, unknown2))).thenReturn(unknown2);
		String[] args = {"5"};
		
		testChooseRunningMate_Unknowns(args, PartyMembership.LIBERAL, SecretRole.LIBERAL);
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(unknown, unknown2));
	}
	
	@Test
	public void testChooseRunningMate_HitlerPresidentUnknowns() {
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, unknown, liberal, unknown2, liberal2));
		when(randomUtil.getRandomItemFromList(Arrays.asList(unknown, unknown2))).thenReturn(unknown2);
		String[] args = {"5"};
		
		testChooseRunningMate_Unknowns(args, PartyMembership.FASCIST, SecretRole.HITLER);
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(unknown, unknown2));
	}
	
	protected void testChooseRunningMate_Unknowns(String[] args, PartyMembership membership, SecretRole role) {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(membership);
		myPlayer.setSecretRole(role);
	
		testChooseRunningMate(Optional.of(new GameplayAction(Action.CHOOSE_RUNNING_MATE, args)));
	}
	
	private void testChooseRunningMate(Optional<GameplayAction> expectedResult) {
		processor.vetoUsedThisTurn = true;
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		
		Optional<GameplayAction> result = processor.getActionToTake(notification);
		
		assertEquals(expectedResult, result);
		assertFalse("The vetoUsedThisTurn flag should be reset when a new running mate is chosen", processor.vetoUsedThisTurn);
	}
	
	@Test
	public void testVote_AlreadyVoted() {
		myPlayer.setVoteReady(true);
		
		testVote(Optional.empty());
	}
	
	@Test
	public void testVote_Dead() {
		myPlayer.setAlive(false);
		
		testVote(Optional.empty());
	}
	
	@Test
	public void testVote_FascistTwoLiberals() {
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		liberal.setPresident(true);
		liberal2.setChancellor(true);
		String[] args = {Vote.NEIN.name()};
		
		testVote(Optional.of(new GameplayAction(Action.VOTE, args)));
	}
	
	@Test
	public void testVote_FascistOneUnknown() {
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		myPlayer.setSecretRole(SecretRole.HITLER);
		liberal.setPresident(true);
		unknown.setChancellor(true);
		String[] args = {Vote.JA.name()};
		
		testVote(Optional.of(new GameplayAction(Action.VOTE, args)), Arrays.asList(liberal, liberal2, myPlayer, fascist, fascist2, unknown, unknown2));
	}
	
	@Test
	public void testVote_FascistOneFascists() {
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		liberal.setPresident(true);
		fascist.setChancellor(true);
		String[] args = {Vote.JA.name()};
		
		testVote(Optional.of(new GameplayAction(Action.VOTE, args)));
	}
	
	@Test
	public void testVote_LiberalOneFascist() {
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		liberal.setPresident(true);
		fascist.setChancellor(true);
		String[] args = {Vote.NEIN.name()};
		
		testVote(Optional.of(new GameplayAction(Action.VOTE, args)));
	}
	
	@Test
	public void testVote_LiberalNoFascists() {
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		liberal.setPresident(true);
		unknown.setChancellor(true);
		String[] args = {Vote.JA.name()};
		
		testVote(Optional.of(new GameplayAction(Action.VOTE, args)));
	}
	
	protected void testVote(Optional<GameplayAction> expectedResult) {
		testVote(expectedResult, Arrays.asList(liberal, liberal2, myPlayer, fascist, fascist2, unknown, unknown2));
	}
	
	protected void testVote(Optional<GameplayAction> expectedResult, List<PlayerData> players) {
		gameData.setPhase(GamePhase.ELECTION);
		gameData.setPlayers(players);
		
		Optional<GameplayAction> result = processor.getActionToTake(notification);
		
		assertEquals(expectedResult, result);
	}
	
	@Test
	public void testMakePresidentChoice_NotPresident() {
		testMakePresidentChoice(Optional.empty());
	}
	
	@Test
	public void testMakePresidentChoice_LiberalWithFascistPolicy() {		
		testMakePresidentChoice_President(PartyMembership.LIBERAL, Arrays.asList(Policy.LIBERAL, Policy.FASCIST, Policy.FASCIST), 1);
	}
	
	@Test
	public void testMakePresidentChoice_LiberalWithAllLiberals() {		
		testMakePresidentChoice_President(PartyMembership.LIBERAL, Arrays.asList(Policy.LIBERAL, Policy.LIBERAL, Policy.LIBERAL), 0);
	}
	
	@Test
	public void testMakePresidentChoice_FascistWithLiberalPolicy() {		
		testMakePresidentChoice_President(PartyMembership.FASCIST, Arrays.asList(Policy.FASCIST, Policy.FASCIST, Policy.LIBERAL), 2);
	}
	
	@Test
	public void testMakePresidentChoice_LiberalWithAllFascist() {		
		testMakePresidentChoice_President(PartyMembership.FASCIST, Arrays.asList(Policy.FASCIST, Policy.FASCIST, Policy.FASCIST), 0);
	}
	
	private void testMakePresidentChoice_President(PartyMembership membership, List<Policy> policies, int expectedIndex) {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(membership);
		gameData.setPoliciesToView(policies);
		String[] args = {Integer.toString(expectedIndex)};
		
		testMakePresidentChoice(Optional.of(new GameplayAction(Action.PRESIDENT_CHOICE, args)));
	}
	
	private void testMakePresidentChoice(Optional<GameplayAction> expectedResult) {
		gameData.setPhase(GamePhase.PRESIDENT_CHOICE);
		
		Optional<GameplayAction> result = processor.getActionToTake(notification);
		
		assertEquals(expectedResult, result);
	}
	
	@Test
	public void testMakeChancellorChoice_NotChancellor() {
		testMakeChancellorChoice(Optional.empty());
	}
	
	@Test
	public void testMakeChancellorChoice_LiberalVeto() {
		gameData.setVetoUnlocked(true);
		String[] args = {};
		
		testMakeChancellorChoice_Chancellor(PartyMembership.LIBERAL, Arrays.asList(Policy.FASCIST, Policy.FASCIST), new GameplayAction(Action.CHANCELLOR_VETO, args));
		
		assertTrue("The processor should remember that a veto has already been used this turn", processor.vetoUsedThisTurn);
	}
	
	@Test
	public void testMakeChancellorChoice_FascistlVeto() {
		gameData.setVetoUnlocked(true);
		String[] args = {};
		
		testMakeChancellorChoice_Chancellor(PartyMembership.FASCIST, Arrays.asList(Policy.LIBERAL, Policy.LIBERAL), new GameplayAction(Action.CHANCELLOR_VETO, args));
		
		assertTrue("The processor should remember that a veto has already been used this turn", processor.vetoUsedThisTurn);
	}
	
	@Test
	public void testMakeChancellorChoice_LiberalWithLiberalPolicy() {
		gameData.setVetoUnlocked(true);
		
		testMakeChancellorChoice_NoVeto(PartyMembership.LIBERAL, Arrays.asList(Policy.LIBERAL, Policy.FASCIST), 1);
	}
	
	@Test
	public void testMakeChancellorChoice_VetoNotUnlocked() {
		testMakeChancellorChoice_NoVeto(PartyMembership.FASCIST, Arrays.asList(Policy.LIBERAL, Policy.LIBERAL), 0);
	}
	
	@Test
	public void testMakeChancellorChoice_VetoAlreadyUsed() {
		processor.vetoUsedThisTurn = true;
		gameData.setVetoUnlocked(true);
		String[] args = {"0"};
		
		testMakeChancellorChoice_Chancellor(PartyMembership.LIBERAL, Arrays.asList(Policy.FASCIST, Policy.FASCIST), new GameplayAction(Action.CHANCELLOR_CHOICE, args));
	}
	
	@Test
	public void testMakeChancellorChoice_LiberalAllLiberals() {
		testMakeChancellorChoice_NoVeto(PartyMembership.LIBERAL, Arrays.asList(Policy.LIBERAL, Policy.LIBERAL), 0);
	}
	
	private void testMakeChancellorChoice_NoVeto(PartyMembership membership, List<Policy> policies, int expectedIndex) {
		String[] args = {Integer.toString(expectedIndex)};
		
		testMakeChancellorChoice_Chancellor(membership, policies, new GameplayAction(Action.CHANCELLOR_CHOICE, args));
		
		assertFalse("The processor should know that a veto has not been used this turn", processor.vetoUsedThisTurn);
	}
	
	private void testMakeChancellorChoice_Chancellor(PartyMembership membership, List<Policy> policies, GameplayAction gameplayAction) {
		myPlayer.setChancellor(true);
		myPlayer.setPartyMembership(membership);
		gameData.setPoliciesToView(policies);
		
		testMakeChancellorChoice(Optional.of(gameplayAction));
	}
	
	private void testMakeChancellorChoice(Optional<GameplayAction> expectedResult) {
		gameData.setPhase(GamePhase.CHANCELLOR_CHOICE);
		
		Optional<GameplayAction> result = processor.getActionToTake(notification);
		
		assertEquals(expectedResult, result);
	}
	
	@Test
	public void testExamine_NotPresident() {
		testExamine(Optional.empty());
	}
	
	@Test
	public void testExamine_President() {
		testExamine_President(Arrays.asList(Policy.FASCIST, Policy.LIBERAL, Policy.FASCIST));
	}
	
	protected void testExamine_President(List<Policy> policies) {
		gameData.setPoliciesToView(policies);
		myPlayer.setPresident(true);
		String[] args = {};
		
		testExamine(Optional.of(new GameplayAction(Action.FINISH_EXAMINATION, args)));
	}
	
	private void testExamine(Optional<GameplayAction> expectedResult) {
		gameData.setPhase(GamePhase.EXAMINE);
		
		Optional<GameplayAction> result = processor.getActionToTake(notification);
		
		assertEquals(expectedResult, result);
	}
	
	@Test
	public void testKill_NotPresident() {
		testKill(Optional.empty());
	}
	
	@Test
	public void testKill_LiberalWithFascists() {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		gameData.setPlayers(Arrays.asList(myPlayer, fascist, liberal, unknown, liberal2, unknown2, fascist2, deadPlayer));
		String[] args = {"Fascist2"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(fascist, fascist2))).thenReturn(fascist2);
		
		testKill(Optional.of(new GameplayAction(Action.KILL_PLAYER, args)));
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(fascist, fascist2));
	}
	
	@Test
	public void testKill_FascistWithLiberals() {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(PartyMembership.FASCIST);
		myPlayer.setSecretRole(SecretRole.FASCIST);
		gameData.setPlayers(Arrays.asList(myPlayer, fascist, liberal, unknown, liberal2, unknown2, fascist2, deadPlayer));
		String[] args = {"Liberal"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(liberal, liberal2))).thenReturn(liberal);
		
		testKill(Optional.of(new GameplayAction(Action.KILL_PLAYER, args)));
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(liberal, liberal2));
	}
	
	@Test
	public void testKill_Unknown() {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		gameData.setPlayers(Arrays.asList(myPlayer, unknown, unknown2, deadPlayer));
		String[] args = {"Unknown"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(unknown, unknown2))).thenReturn(unknown);
		
		testKill(Optional.of(new GameplayAction(Action.KILL_PLAYER, args)));
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(unknown, unknown2));
	}
	
	protected void testKill(Optional<GameplayAction> expectedResult) {
		gameData.setPhase(GamePhase.KILL);
		
		Optional<GameplayAction> result = processor.getActionToTake(notification);
		
		assertEquals(expectedResult, result);
	}
	
	@Test
	public void testPresidentVeto_NotPresident() {
		testPresidentVeto(Optional.empty());
	}
	
	@Test
	public void testPresidentVeto_LiberalAllFascist() {
		testPresidentVeto_President(PartyMembership.LIBERAL, Arrays.asList(Policy.FASCIST, Policy.FASCIST), true);
	}
	
	@Test
	public void testPresidentVeto_LiberalAllLiberal() {
		testPresidentVeto_President(PartyMembership.LIBERAL, Arrays.asList(Policy.LIBERAL, Policy.LIBERAL), false);
	}
	
	@Test
	public void testPresidentVeto_FascistAllFascist() {
		testPresidentVeto_President(PartyMembership.FASCIST, Arrays.asList(Policy.FASCIST, Policy.FASCIST), false);
	}
	
	@Test
	public void testPresidentVeto_FascistAllLiberal() {
		testPresidentVeto_President(PartyMembership.FASCIST, Arrays.asList(Policy.LIBERAL, Policy.LIBERAL), true);
	}
	
	@Test
	public void testPresidentVeto_Mixed() {
		testPresidentVeto_President(PartyMembership.FASCIST, Arrays.asList(Policy.LIBERAL, Policy.FASCIST), false);
	}
	
	private void testPresidentVeto_President(PartyMembership membership, List<Policy> policies, boolean concur) {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(membership);
		gameData.setPoliciesToView(policies);
		String[] args = {Boolean.toString(concur)};
		
		testPresidentVeto(Optional.of(new GameplayAction(Action.PRESIDENT_VETO, args)));
	}
	
	protected void testPresidentVeto(Optional<GameplayAction> expectedResult) {
		gameData.setPhase(GamePhase.VETO);
		
		Optional<GameplayAction> result = processor.getActionToTake(notification);
		
		assertEquals(expectedResult, result);
	}
	
	@Test
	public void testInvestigate_NotPresident() {
		testInvestigate(Optional.empty());
	}
	
	@Test
	public void testInvestigate_WithUnknown() {
		when(randomUtil.getRandomItemFromList(Arrays.asList(unknown, unknown2))).thenReturn(unknown2);
		
		testInvestigate_President(PartyMembership.FASCIST, Arrays.asList(liberal, unknown, myPlayer, fascist, deadPlayer, fascist2, liberal2, unknown2), "Unknown2");
	
		verify(randomUtil).getRandomItemFromList(Arrays.asList(unknown, unknown2));
	}
	
	@Test
	public void testInvestigate_AllKNown() {
		when(randomUtil.getRandomItemFromList(Arrays.asList(liberal, fascist, fascist2, liberal2))).thenReturn(fascist);
		
		testInvestigate_President(PartyMembership.FASCIST, Arrays.asList(liberal, myPlayer, fascist, deadPlayer, fascist2, liberal2), "Fascist");
	
		verify(randomUtil).getRandomItemFromList(Arrays.asList(liberal, fascist, fascist2, liberal2));
	}
	
	protected void testInvestigate_President(PartyMembership membership, List<PlayerData> players, String expectedPlayerForInvestigation) {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(membership);
		gameData.setPlayers(players);
		String[] args = {expectedPlayerForInvestigation};
		
		testInvestigate(Optional.of(new GameplayAction(Action.INVESTIGATE_PLAYER, args)));
	}
	
	private void testInvestigate(Optional<GameplayAction> expectedResult) {
		gameData.setPhase(GamePhase.INVESTIGATE);
		
		Optional<GameplayAction> result = processor.getActionToTake(notification);
		
		assertEquals(expectedResult, result);
	}
	
	@Test
	public void testChooseNextPresidentialCandidate_NotPresident() {
		testChooseNextPresidentialCandidate(Optional.empty());
	}
	
	@Test
	public void testChooseNextPresidentialCandidate_LiberalWithLiberals() {
		when(randomUtil.getRandomItemFromList(Arrays.asList(liberal, liberal2))).thenReturn(liberal2);
		
		testChooseNextPresidentialCandidate_President(PartyMembership.LIBERAL, Arrays.asList(fascist, unknown, deadPlayer, myPlayer, fascist2, liberal, unknown2, liberal2), 7);
	
		verify(randomUtil).getRandomItemFromList(Arrays.asList(liberal, liberal2));
	}
	
	@Test
	public void testChooseNextPresidentialCandidate_FascistWithFascists() {
		when(randomUtil.getRandomItemFromList(Arrays.asList(fascist, fascist2))).thenReturn(fascist);
		
		testChooseNextPresidentialCandidate_President(PartyMembership.FASCIST, Arrays.asList(fascist, unknown, deadPlayer, myPlayer, fascist2, liberal, unknown2, liberal2), 0);
	
		verify(randomUtil).getRandomItemFromList(Arrays.asList(fascist, fascist2));
	}
	
	@Test
	public void testChooseNextPresidentialCandidate_Unknowns() {
		when(randomUtil.getRandomItemFromList(Arrays.asList(unknown, unknown2))).thenReturn(unknown2);
		
		testChooseNextPresidentialCandidate_President(PartyMembership.LIBERAL, Arrays.asList(unknown, deadPlayer, myPlayer, unknown2), 3);
	
		verify(randomUtil).getRandomItemFromList(Arrays.asList(unknown, unknown2));
	}
	
	protected void testChooseNextPresidentialCandidate_President(PartyMembership membership, List<PlayerData> players, int expectedIndex) {
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(membership);
		gameData.setPlayers(players);
		String[] args = {Integer.toString(expectedIndex)};
		
		testChooseNextPresidentialCandidate(Optional.of(new GameplayAction(Action.CHOOSE_NEXT_PRESIDENTIAL_CANDIDATE, args)));
	}
	
	protected void testChooseNextPresidentialCandidate(Optional<GameplayAction> expectedResult) {
		gameData.setPhase(GamePhase.SPECIAL_ELECTION);
		
		Optional<GameplayAction> result = processor.getActionToTake(notification);
		
		assertEquals(expectedResult, result);
	}
}
