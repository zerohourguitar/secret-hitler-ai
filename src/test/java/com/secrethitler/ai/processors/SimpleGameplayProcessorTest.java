package com.secrethitler.ai.processors;

import static org.junit.Assert.assertEquals;
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
		myPlayer.setAlive(true);
		gameData.setMyPlayer(myPlayer);
		previousGovernmentMember = new PlayerData();
		previousGovernmentMember.setPreviousGovernmentMember(true);
		previousGovernmentMember.setAlive(true);
		deadPlayer = new PlayerData();
		hitler = new PlayerData();
		hitler.setAlive(true);
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
	public void testChooseRunningMate_FascistPresidentWithHitler() {
		myPlayer.setPresident(true);
		myPlayer.setSecretRole(SecretRole.FASCIST);
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, hitler, liberal, fascist));
		String[] args = {"3"};
		
		testChooseRunningMate(Optional.of(new GameplayAction(Action.CHOOSE_RUNNING_MATE, args)));
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
		myPlayer.setPresident(true);
		myPlayer.setPartyMembership(PartyMembership.LIBERAL);
		myPlayer.setSecretRole(SecretRole.LIBERAL);
		gameData.setPlayers(Arrays.asList(previousGovernmentMember, deadPlayer, myPlayer, unknown, fascist, unknown2, fascist2));
		String[] args = {"5"};
		when(randomUtil.getRandomItemFromList(Arrays.asList(unknown, unknown2))).thenReturn(unknown2);
		
		testChooseRunningMate(Optional.of(new GameplayAction(Action.CHOOSE_RUNNING_MATE, args)));
		
		verify(randomUtil).getRandomItemFromList(Arrays.asList(unknown, unknown2));
	}
	
	private void testChooseRunningMate(Optional<GameplayAction> expectedResult) {
		gameData.setPhase(GamePhase.PICKING_RUNNING_MATE);
		
		Optional<GameplayAction> result = processor.getActionToTake(notification);
		
		assertEquals(expectedResult, result);
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
}
