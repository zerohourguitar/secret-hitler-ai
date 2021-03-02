package com.secrethitler.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.secrethitler.ai.dtos.LoginRequest;
import com.secrethitler.ai.dtos.LoginResponse;
import com.secrethitler.ai.utils.UrlWrapper;
import com.secrethitler.ai.websockets.GameSetupWebsocketClientEndpoint;

@RunWith(MockitoJUnitRunner.class)
public class SecretHitlerAiTest {
	private static final String TEST_NON_SECURE_PROPERTIES_FILE_NAME = "test-nonsecure.properties";
	private static final String TEST_SECURE_PROPERTIES_FILE_NAME = "test-secure.properties";
	private static final String AUTH = "TestAuthToken";
	
	@Mock
	private Function<String, UrlWrapper> getUrlFunction;
	
	@Mock
	private Function<GameSetupWebsocketClientEndpoint.Builder, GameSetupWebsocketClientEndpoint> gameSetupClientBuildFunction;
	
	@Mock
	private UrlWrapper loginUrl;
	
	@Mock
	private UrlWrapper createGameUrl;
	
	@Mock
	private HttpURLConnection loginCon;
	
	@Mock
	private HttpURLConnection createGameCon;
	
	private ByteArrayOutputStream loginOs;
	
	@Test
	public void testSecretHitlerAi_ExistingGame_NonSecure() throws Exception {
		SecretHitlerAi ai = testSecretHitlerAi("testGameId", TEST_NON_SECURE_PROPERTIES_FILE_NAME, "http://test.com/login");
	
		verify(gameSetupClientBuildFunction).apply(GameSetupWebsocketClientEndpoint.builder()
				.withAi(ai)
				.withGameId("testGameId")
				.withAccessToken(AUTH)
				.withGameplayLevel(1)
				.withUsername("Robot 1")
				.withHost(false));
		verify(gameSetupClientBuildFunction).apply(GameSetupWebsocketClientEndpoint.builder()
				.withAi(ai)
				.withGameId("testGameId")
				.withAccessToken(AUTH)
				.withGameplayLevel(2)
				.withUsername("Robot 2")
				.withHost(false));
		assertFalse(ai.isSecureUrl());
	}
	
	@Test
	public void testSecretHitlerAi_ExistingGame_Secure() throws Exception {
		SecretHitlerAi ai = testSecretHitlerAi("testGameId", TEST_SECURE_PROPERTIES_FILE_NAME, "https://test.com/login");
	
		verify(gameSetupClientBuildFunction).apply(GameSetupWebsocketClientEndpoint.builder()
				.withAi(ai)
				.withGameId("testGameId")
				.withAccessToken(AUTH)
				.withGameplayLevel(1)
				.withUsername("Robot 1")
				.withHost(false));
		verify(gameSetupClientBuildFunction).apply(GameSetupWebsocketClientEndpoint.builder()
				.withAi(ai)
				.withGameId("testGameId")
				.withAccessToken(AUTH)
				.withGameplayLevel(2)
				.withUsername("Robot 2")
				.withHost(false));
		assertTrue(ai.isSecureUrl());
	}
	
	@Test
	public void testSecretHitlerAi_NewGame_NonSecure() throws Exception {
		when(getUrlFunction.apply("http://test.com/create-game")).thenReturn(createGameUrl);
		when(createGameUrl.openConnection()).thenReturn(createGameCon);
		ByteArrayOutputStream createGameOs = new ByteArrayOutputStream();
		when(createGameCon.getOutputStream()).thenReturn(createGameOs);
		when(createGameCon.getInputStream()).then(invocation -> new ByteArrayInputStream(SecretHitlerAi.getObjectWriter().writeValueAsString("testGameId").getBytes(StandardCharsets.UTF_8)));
		
		SecretHitlerAi ai = testSecretHitlerAi("newGame", TEST_NON_SECURE_PROPERTIES_FILE_NAME, "http://test.com/login");
		
		verify(createGameCon).setRequestMethod("POST");
		verify(createGameCon).setRequestProperty("Content-Type", "application/json; utf-8");
		verify(createGameCon).setRequestProperty("Accept", "application/json");
		verify(createGameCon).setDoOutput(true);
		verify(createGameCon).setRequestProperty("authorization", AUTH);
		final String actualJson = new String(createGameOs.toByteArray(), StandardCharsets.UTF_8);
		assertEquals(SecretHitlerAi.EMPTY_PAYLOAD, actualJson);
		verify(gameSetupClientBuildFunction).apply(GameSetupWebsocketClientEndpoint.builder()
				.withAi(ai)
				.withGameId("\"testGameId\"")
				.withAccessToken(AUTH)
				.withGameplayLevel(1)
				.withUsername("Robot 1")
				.withHost(true));
		verify(gameSetupClientBuildFunction).apply(GameSetupWebsocketClientEndpoint.builder()
				.withAi(ai)
				.withGameId("\"testGameId\"")
				.withAccessToken(AUTH)
				.withGameplayLevel(2)
				.withUsername("Robot 2")
				.withHost(false));
		assertFalse(ai.isSecureUrl());
	}
	
	@Test
	public void testSecretHitlerAi_NewGame_Secure() throws Exception {
		when(getUrlFunction.apply("https://test.com/create-game")).thenReturn(createGameUrl);
		when(createGameUrl.openConnection()).thenReturn(createGameCon);
		ByteArrayOutputStream createGameOs = new ByteArrayOutputStream();
		when(createGameCon.getOutputStream()).thenReturn(createGameOs);
		when(createGameCon.getInputStream()).then(invocation -> new ByteArrayInputStream(SecretHitlerAi.getObjectWriter().writeValueAsString("testGameId").getBytes(StandardCharsets.UTF_8)));
		
		SecretHitlerAi ai = testSecretHitlerAi("newGame", TEST_SECURE_PROPERTIES_FILE_NAME, "https://test.com/login");
		
		verify(createGameCon).setRequestMethod("POST");
		verify(createGameCon).setRequestProperty("Content-Type", "application/json; utf-8");
		verify(createGameCon).setRequestProperty("Accept", "application/json");
		verify(createGameCon).setDoOutput(true);
		verify(createGameCon).setRequestProperty("authorization", AUTH);
		final String actualJson = new String(createGameOs.toByteArray(), StandardCharsets.UTF_8);
		assertEquals(SecretHitlerAi.EMPTY_PAYLOAD, actualJson);
		verify(gameSetupClientBuildFunction).apply(GameSetupWebsocketClientEndpoint.builder()
				.withAi(ai)
				.withGameId("\"testGameId\"")
				.withAccessToken(AUTH)
				.withGameplayLevel(1)
				.withUsername("Robot 1")
				.withHost(true));
		verify(gameSetupClientBuildFunction).apply(GameSetupWebsocketClientEndpoint.builder()
				.withAi(ai)
				.withGameId("\"testGameId\"")
				.withAccessToken(AUTH)
				.withGameplayLevel(2)
				.withUsername("Robot 2")
				.withHost(false));
		assertTrue(ai.isSecureUrl());
	}
	
	private SecretHitlerAi testSecretHitlerAi(final String gameId, final String propertiesFileName, String expectedLoginUrl) throws Exception {		
		when(getUrlFunction.apply(expectedLoginUrl)).thenReturn(loginUrl);
		when(loginUrl.openConnection()).thenReturn(loginCon);
		loginOs = new ByteArrayOutputStream();
		when(loginCon.getOutputStream()).thenReturn(loginOs);
		LoginResponse response = new LoginResponse();
		response.setAccessToken(AUTH);
		when(loginCon.getInputStream()).then(invocation -> new ByteArrayInputStream(SecretHitlerAi.getObjectWriter().writeValueAsString(response).getBytes(StandardCharsets.UTF_8)));
		
		SecretHitlerAi ai = new SecretHitlerAi(propertiesFileName, getUrlFunction, gameSetupClientBuildFunction, gameId, Arrays.asList(1, 2));
		
		verify(loginCon, times(2)).setRequestMethod("POST");
		verify(loginCon, times(2)).setRequestProperty("Content-Type", "application/json; utf-8");
		verify(loginCon, times(2)).setRequestProperty("Accept", "application/json");
		verify(loginCon, times(2)).setDoOutput(true);
		final String expectedJson = SecretHitlerAi.getObjectWriter().writeValueAsString(new LoginRequest("Robot 1", "password")) +
				SecretHitlerAi.getObjectWriter().writeValueAsString(new LoginRequest("Robot 2", "password"));
		final String actualJson = new String(loginOs.toByteArray(), StandardCharsets.UTF_8);
		assertEquals(expectedJson, actualJson);
		assertNotNull(ai.getProp());
		assertEquals("test.com", ai.getBaseUrlString());
		
		return ai;
	}

	@Test
	public void testGetScanner() {
		assertNotNull(SecretHitlerAi.getScanner());
	}
	
	@Test
	public void testGetObjectMapper() {
		assertNotNull(SecretHitlerAi.getObjectMapper());
	}
}
