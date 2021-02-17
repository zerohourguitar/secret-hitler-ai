package com.secrethitler.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.secrethitler.ai.dtos.LoginRequest;
import com.secrethitler.ai.dtos.LoginResponse;
import com.secrethitler.ai.websockets.GameSetupWebsocketClientEndpoint;

public class SecretHitlerAi {
	private static final Logger LOGGER = Logger.getLogger(SecretHitlerAi.class.getName());
	private static final Scanner SCANNER = new Scanner(System.in);
	private static final Properties PROP = new Properties();
	private static final String PROPERTIES_FILE_NAME = "application.properties";
	private static final String NEW_GAME_COMMAND = "newGame";
	
	private static String baseUrlString;
	
	public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
		final String originalGameId = args[0];
		boolean newGame = NEW_GAME_COMMAND.contentEquals(originalGameId);
		LOGGER.info(() -> getStartupLogMessage(newGame, originalGameId, args.length - 1));
		String gameId = originalGameId;
		
		ClassLoader loader = Thread.currentThread().getContextClassLoader();           
		InputStream stream = loader.getResourceAsStream(PROPERTIES_FILE_NAME);
		PROP.load(stream);
		baseUrlString = PROP.getProperty("secrethitler.url");
		final String robotPassword = PROP.getProperty("secrethitler.login.robotpassword");
		
		for (int idx=1; idx < args.length; idx++) {
			String username = String.format("Robot %d", idx);
			String accessToken = getAuthenticatedAccessToken(username, robotPassword);
			LOGGER.info(() -> String.format("Logged in user %s", username));
			boolean host = idx == 1 && newGame;
			if (host) {
				final String newGameId = createNewGame(accessToken);
				LOGGER.info(() -> String.format("New game created with id: %s", newGameId));
				gameId = newGameId;
			}
			new GameSetupWebsocketClientEndpoint(gameId, accessToken, Integer.valueOf(args[idx]), username, host);
		}
		
		Thread.currentThread().join();
	}
	
	private static String getStartupLogMessage(final boolean newGame, final String gameId, final int totalUsers) {
		if (newGame) {
			return String.format("Starting Secret Hitler AI for %d users creating a new game", totalUsers);
		} 
		return String.format("Starting Secret Hitler AI for %d users with gameId %s.", totalUsers, gameId);
	}

	private static String getAuthenticatedAccessToken(final String username, final String password) throws IOException {
		LoginRequest loginRequest = new LoginRequest(username, password);
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(loginRequest);
		LOGGER.fine(() -> String.format("Logging on with payload: %s", json));
		final String loginUrlString = PROP.getProperty("secrethitler.login.url");
		String response = post(json, loginUrlString, Optional.empty());
		LOGGER.fine(() -> String.format("Received login response: %s", response));
		LoginResponse loginResponse = mapper.readValue(response, LoginResponse.class);
		return loginResponse.getAccessToken();
	}
	
	private static String createNewGame(final String accessToken) throws IOException {
		final String createGameUrlString = PROP.getProperty("secrethitler.creategame.url");
		return post("{}", createGameUrlString, Optional.of(accessToken));
	}
	
	private static String post(final String payload, final String urlString, final Optional<String> authorization) throws IOException {
		URL url = new URL(String.format("https://%s%s", baseUrlString, urlString));
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		authorization.ifPresent(auth -> con.setRequestProperty("authorization", auth));
		con.setDoOutput(true);
		try(OutputStream os = con.getOutputStream()) {
		    byte[] input = payload.getBytes(StandardCharsets.UTF_8);
		    os.write(input, 0, input.length);			
		}
		
		try(BufferedReader br = new BufferedReader(
				new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder response = new StringBuilder();
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			return response.toString();
		}
	}
	
	public static Scanner getScanner() {
		return SCANNER;
	}

	public static Properties getProp() {
		return PROP;
	}

	public static String getBaseUrlString() {
		return baseUrlString;
	}

}
