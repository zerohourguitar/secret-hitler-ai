package com.secrethitler.ai.websockets;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.secrethitler.ai.SecretHitlerAi;
import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.ParticipantGameNotification;
import com.secrethitler.ai.dtos.PlayerData;
import com.secrethitler.ai.enums.Action;
import com.secrethitler.ai.enums.GamePhase;
import com.secrethitler.ai.processors.GameplayProcessor;
import com.secrethitler.ai.processors.GameplayProcessorFactory;
import com.secrethitler.ai.utils.UriWrapper;
import com.secrethitler.ai.websockets.WebsocketClientEndpoint.Builder;

@RunWith(MockitoJUnitRunner.class)
public class GamePlayWebsocketClientEndpointTest extends WebsocketClientEndpointTest {
	@Mock
	private SecretHitlerAi ai;
	
	@Mock
	private Properties prop;
	
	@Mock
	private Function<String, UriWrapper> uriBuilderFunction;
	
	@Mock
	private UriWrapper uri;
	
	@Mock
	private BiConsumer<WebsocketClientEndpoint, UriWrapper> uriConnectionConsumer;
	
	@Mock
	private GameplayProcessorFactory gameplayProcessorFactory;
	
	@Mock
	private GameplayProcessor processor;
	
	@Mock
	private Function<GamePlayWebsocketClientEndpoint.Builder, GamePlayWebsocketClientEndpoint> gamePlayClientBuildFunction;
	
	@Mock
	private ObjectWriter writer;
	
	private GamePlayWebsocketClientEndpoint endpoint;
	
	@Override
	@Before
	public void setUp() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, URISyntaxException, DeploymentException, IOException {
		Logger.getLogger(GamePlayWebsocketClientEndpoint.class.getName()).setLevel(Level.FINE);
		when(gameplayProcessorFactory.getGameplayProcessor(2, "testUsername")).thenReturn(processor);
		when(ai.getBaseUrlString()).thenReturn("test.com");
		when(ai.getProp()).thenReturn(prop);
		when(prop.getProperty(GamePlayWebsocketClientEndpoint.MOVE_DELAY)).thenReturn("500");
		when(prop.getProperty(GamePlayWebsocketClientEndpoint.GAMEPLAY_URL)).thenReturn("/gamePlay");
		when(uriBuilderFunction.apply("ws://test.com/gamePlay?gameId=testGameId&auth=testAccessToken")).thenReturn(uri);
		endpoint = builderHelper().build();
		
		super.setUp();
		
		verify(uriBuilderFunction).apply("ws://test.com/gamePlay?gameId=testGameId&auth=testAccessToken");
		verify(uriConnectionConsumer).accept(endpoint, uri);
	}
	
	@Override
	protected WebsocketClientEndpoint getEndpoint() {
		return endpoint;
	}

	@Override
	protected Builder getBuilder() {
		return builderHelper();
	}
	
	private GamePlayWebsocketClientEndpoint.Builder builderHelper() {
		return GamePlayWebsocketClientEndpoint.builder()
				.withAi(ai)
				.withGameId("testGameId")
				.withAccessToken("testAccessToken")
				.withLevel(2)
				.withUsername("testUsername")
				.withGameplayProcessorFactory(gameplayProcessorFactory)
				.withGamePlayClientBuildFunction(gamePlayClientBuildFunction)
				.withUriBuilderFunction(uriBuilderFunction)
				.withUriConnectionConsumer(uriConnectionConsumer);
	}
	
	@Test
	public void testSecureWs() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, URISyntaxException {
		when(ai.isSecureUrl()).thenReturn(true);
		when(uriBuilderFunction.apply("wss://test.com/gamePlay?gameId=testGameId&auth=testAccessToken")).thenReturn(uri);
		
		endpoint = builderHelper().build();
		
		verify(uriBuilderFunction).apply("wss://test.com/gamePlay?gameId=testGameId&auth=testAccessToken");
		verify(uriConnectionConsumer).accept(endpoint, uri);
	}
	
	@Test
	public void testOnMessage_WithNextGameId() throws IOException {
		endpoint.userSession = userSession;
		ParticipantGameNotification notification = new ParticipantGameNotification();
		GameData gameData = new GameData();
		gameData.setNextGameId("nextGameId");
		notification.setGameData(gameData);
		
		endpoint.onMessage(SecretHitlerAi.getObjectWriter().writeValueAsString(notification));
		
		verify(gamePlayClientBuildFunction).apply(GamePlayWebsocketClientEndpoint.builder()
				.withAi(ai)
				.withGameId("nextGameId")
				.withAccessToken("testAccessToken")
				.withLevel(2)
				.withUsername("testUsername")
				.withGameplayProcessorFactory(gameplayProcessorFactory)
				.withGamePlayClientBuildFunction(gamePlayClientBuildFunction));
		verify(userSession).close();
	}
	
	@Test
	public void testOnMessage_NoPhaseChange() throws JsonProcessingException {
		endpoint.previousPhase = GamePhase.ELECTION;
		ParticipantGameNotification notification = new ParticipantGameNotification();
		GameData gameData = new GameData();
		gameData.setPhase(GamePhase.ELECTION);
		notification.setGameData(gameData);
		
		endpoint.onMessage(SecretHitlerAi.getObjectWriter().writeValueAsString(notification));
		
		verify(processor, never()).getActionToTake(notification);
	}
	
	@Test
	public void testOnMessage_GameOverHost() throws JsonProcessingException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, URISyntaxException, InterruptedException {
		InputStream in = new ByteArrayInputStream("Blah Blah Blah Test\r\n".getBytes());
		System.setIn(in);
		getBuilder().build();
		endpoint.userSession = userSession;
		endpoint.previousPhase = GamePhase.ELECTION;
		ParticipantGameNotification notification = new ParticipantGameNotification();
		GameData gameData = new GameData();
		gameData.setPhase(GamePhase.GAME_OVER);
		PlayerData myPlayer = new PlayerData();
		myPlayer.setHost(true);
		gameData.setMyPlayer(myPlayer);
		notification.setGameData(gameData);
		
		endpoint.onMessage(SecretHitlerAi.getObjectWriter().writeValueAsString(notification));
		
		Thread.sleep(500);
		String[] args = {};
		final String expectedAction = SecretHitlerAi.getObjectWriter().writeValueAsString(new GameplayAction(Action.NEW_GAME, args));
		verify(async).sendText(expectedAction);
		verify(processor, never()).getActionToTake(notification);
	}
	
	@Test
	public void testOnMessage_GameOverNotHost() throws JsonProcessingException {
		endpoint.userSession = userSession;
		endpoint.previousPhase = GamePhase.ELECTION;
		ParticipantGameNotification notification = new ParticipantGameNotification();
		GameData gameData = new GameData();
		gameData.setPhase(GamePhase.GAME_OVER);
		PlayerData myPlayer = new PlayerData();
		gameData.setMyPlayer(myPlayer);
		notification.setGameData(gameData);
		when(processor.getActionToTake(notification)).thenReturn(Optional.empty());
		
		endpoint.onMessage(SecretHitlerAi.getObjectWriter().writeValueAsString(notification));
		
		verify(processor).getActionToTake(notification);
	}
	
	@Test
	public void testOnMessage_WithAction() throws JsonProcessingException {
		endpoint.userSession = userSession;
		endpoint.previousPhase = GamePhase.ELECTION;
		ParticipantGameNotification notification = new ParticipantGameNotification();
		GameData gameData = new GameData();
		gameData.setPhase(GamePhase.PRESIDENT_CHOICE);
		PlayerData myPlayer = new PlayerData();
		myPlayer.setHost(true);
		gameData.setMyPlayer(myPlayer);
		notification.setGameData(gameData);
		String[] args = {"1"};
		GameplayAction action = new GameplayAction(Action.PRESIDENT_CHOICE, args);
		when(processor.getActionToTake(notification)).thenReturn(Optional.of(action));
		
		endpoint.onMessage(SecretHitlerAi.getObjectWriter().writeValueAsString(notification));
		
		verify(processor).getActionToTake(notification);
		verify(async).sendText(SecretHitlerAi.getObjectWriter().writeValueAsString(action));
	}

	@Test
	public void testOnMessage_InterruptedSleep() throws InterruptedException, JsonProcessingException {
		endpoint.userSession = userSession;
		endpoint.previousPhase = GamePhase.ELECTION;
		ParticipantGameNotification notification = new ParticipantGameNotification();
		GameData gameData = new GameData();
		gameData.setPhase(GamePhase.PRESIDENT_CHOICE);
		PlayerData myPlayer = new PlayerData();
		myPlayer.setHost(true);
		gameData.setMyPlayer(myPlayer);
		notification.setGameData(gameData);
		String[] args = {"1"};
		GameplayAction action = new GameplayAction(Action.PRESIDENT_CHOICE, args);
		when(processor.getActionToTake(notification)).thenReturn(Optional.of(action));
		
		Thread t = new Thread(() -> {
			try {
				endpoint.onMessage(SecretHitlerAi.getObjectWriter().writeValueAsString(notification));
			} catch (JsonProcessingException e) {
				throw new IllegalStateException(e);
			}
		});
		t.start();
		t.interrupt();
		t.join();
		verify(processor).getActionToTake(notification);
		verify(async).sendText(SecretHitlerAi.getObjectWriter().writeValueAsString(action));
	}
	
	@Test
	public void testOnMessage_BadMessage() {
		endpoint.onMessage("This message is bad");
		
		verify(processor, never()).getActionToTake(any(ParticipantGameNotification.class));
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testGameplayActionToString_BadWriter() throws JsonProcessingException {
		GameplayAction action = new GameplayAction();
		when(writer.writeValueAsString(action)).thenThrow(new JsonProcessingException("Error"){});
		
		assertNull(endpoint.gameplayActionToString(action, writer));
	}
	
	@Override
	@After
	public void tearDown() {
		Logger.getLogger(GamePlayWebsocketClientEndpoint.class.getName()).setLevel(Level.INFO);
		super.tearDown();
	}
}
