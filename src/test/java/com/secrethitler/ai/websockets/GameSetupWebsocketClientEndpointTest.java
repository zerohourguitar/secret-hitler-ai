package com.secrethitler.ai.websockets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Properties;
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
import com.secrethitler.ai.SecretHitlerAi;
import com.secrethitler.ai.dtos.GameRequest;
import com.secrethitler.ai.utils.UriWrapper;
import com.secrethitler.ai.websockets.WebsocketClientEndpoint.Builder;

@RunWith(MockitoJUnitRunner.class)
public class GameSetupWebsocketClientEndpointTest extends WebsocketClientEndpointTest {
	@Mock
	private SecretHitlerAi ai;
	
	@Mock
	private Function<String, UriWrapper> uriBuilderFunction;
	
	@Mock
	private UriWrapper uri;
	
	@Mock
	private Properties prop;
	
	@Mock
	private BiConsumer<WebsocketClientEndpoint, UriWrapper> uriConnectionConsumer;
	
	@Mock
	private Function<GamePlayWebsocketClientEndpoint.Builder, GamePlayWebsocketClientEndpoint> gamePlayClientBuildFunction;
	
	@Mock
	private GamePlayWebsocketClientEndpoint.Builder gamePlayClientBuilder;
	
	private GameSetupWebsocketClientEndpoint endpoint;
	
	@Override
	@Before
	public void setUp() throws URISyntaxException, DeploymentException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Logger.getLogger(GameSetupWebsocketClientEndpoint.class.getName()).setLevel(Level.FINE);
		when(ai.getBaseUrlString()).thenReturn("test.com");
		when(ai.getProp()).thenReturn(prop);
		when(prop.getProperty(GameSetupWebsocketClientEndpoint.GAME_SETUP_URL)).thenReturn("/gameSetup");
		when(uriBuilderFunction.apply("ws://test.com/gameSetup?gameId=testGameId&auth=testAccessToken")).thenReturn(uri);
		
		buildEndpoint(false);
		super.setUp();
		
		verify(uriBuilderFunction).apply("ws://test.com/gameSetup?gameId=testGameId&auth=testAccessToken");
		verify(uriConnectionConsumer).accept(endpoint, uri);
	}

	private void buildEndpoint(final boolean host) throws URISyntaxException {
		endpoint = getBuilder(host).build();
	}
	
	@Override
	protected WebsocketClientEndpoint getEndpoint() {
		return endpoint;
	}
	
	@Override
	protected Builder getBuilder() {
		return getBuilder(false);
	}
	
	protected GameSetupWebsocketClientEndpoint.Builder getBuilder(final boolean host) {
		return GameSetupWebsocketClientEndpoint.builder()
				.withAi(ai)
				.withGameId("testGameId")
				.withAccessToken("testAccessToken")
				.withGameplayLevel(2)
				.withUsername("testUser")
				.withHost(host)
				.withUriBuilderFunction(uriBuilderFunction)
				.withUriConnectionConsumer(uriConnectionConsumer)
				.withGamePlayClientBuildFunction(gamePlayClientBuildFunction);
	}
	
	@Override
	@Test
	public void testOnOpen() {
		super.testOnOpen();
		
		verify(async).sendText("JOIN");
	}
	
	@Test
	public void testSecureWs() throws URISyntaxException {
		when(ai.isSecureUrl()).thenReturn(true);
		when(uriBuilderFunction.apply("wss://test.com/gameSetup?gameId=testGameId&auth=testAccessToken")).thenReturn(uri);
		
		buildEndpoint(false);
		
		verify(uriBuilderFunction).apply("wss://test.com/gameSetup?gameId=testGameId&auth=testAccessToken");
		verify(uriConnectionConsumer).accept(endpoint, uri);
	}
	
	@Test
	public void testOnOpen_Host() throws URISyntaxException, InterruptedException {		
		InputStream in = new ByteArrayInputStream("Blah Blah Blah Test\r\n".getBytes());
		System.setIn(in);
		buildEndpoint(true);
		
		endpoint.onOpen(userSession);
		
		Thread.sleep(500);
		verify(async).sendText("START");
	}

	@Test
	public void testOnMessage_NotStarted() throws IOException {
		endpoint.userSession = userSession;
		GameRequest gameRequest = new GameRequest();
		
		endpoint.onMessage(SecretHitlerAi.getObjectWriter().writeValueAsString(gameRequest));
		
		verify(userSession, never()).close();
	}
	
	@Test
	public void testOnMessage_StartedNoException() throws IOException {
		testOnMessage_Started();
		
		verify(userSession).close();
	}
	
	@Test
	public void testOnMessage_StartedWithException() throws JsonProcessingException {
		when(gamePlayClientBuildFunction.apply(any(GamePlayWebsocketClientEndpoint.Builder.class))).thenThrow(new IllegalArgumentException("Test Exception"));
		
		testOnMessage_Started();
	}
	
	private void testOnMessage_Started() throws JsonProcessingException {
		endpoint.userSession = userSession;
		GameRequest gameRequest = new GameRequest();
		gameRequest.setStarted(true);
		
		endpoint.onMessage(SecretHitlerAi.getObjectWriter().writeValueAsString(gameRequest));
		
		verify(gamePlayClientBuildFunction).apply(GamePlayWebsocketClientEndpoint.builder()
						.withAi(ai)
						.withGameId("testGameId")
						.withAccessToken("testAccessToken")
						.withLevel(2)
						.withUsername("testUser")
						.withGameplayProcessorFactory(SecretHitlerAi.getGameplayProcessorFactory())
						.withGamePlayClientBuildFunction(gamePlayClientBuildFunction));
	}
	
	@Test
	public void testGamePlayWebsocketClientEndpointBuildException() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, URISyntaxException {
		Exception thrownException = new IllegalAccessException("Error");
		when(gamePlayClientBuilder.build()).thenThrow(thrownException);
		try {
			GameSetupWebsocketClientEndpoint.GAME_PLAY_CLIENT_BUILD_FUNCTION.apply(gamePlayClientBuilder);
			fail("Expected an IllegalStateExcpetion to be thrown if the GameplayClientBuildFunction throws an exception");
		} catch(IllegalStateException e) {
			assertEquals(thrownException, e.getCause());
		}
	}
	
	@Override
	@After
	public void tearDown() {
		Logger.getLogger(GameSetupWebsocketClientEndpoint.class.getName()).setLevel(Level.INFO);
		super.tearDown();
	}
}
