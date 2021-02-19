package com.secrethitler.ai;

import java.io.BufferedReader;
import java.io.IOException;
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
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer().withDefaultPrettyPrinter();
	private static final String PROPERTIES_FILE_NAME = "application.properties";
	private static final String NEW_GAME_COMMAND = "newGame";
	private static final String EMPTY_PAYLOAD = "{}";
	
	private static boolean secureUrl;
	private static String baseUrlString;
	
	public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
		final String originalGameId = args[0];
		boolean newGame = NEW_GAME_COMMAND.equals(originalGameId);
		LOGGER.info(() -> getStartupLogMessage(newGame, originalGameId, args.length - 1));
		
		ClassLoader loader = Thread.currentThread().getContextClassLoader();           
		PROP.load(loader.getResourceAsStream(PROPERTIES_FILE_NAME));
		secureUrl = Boolean.getBoolean(PROP.getProperty("secrethitler.secureurl"));
		baseUrlString = PROP.getProperty("secrethitler.url");
		final String robotPassword = PROP.getProperty("secrethitler.login.robotpassword");
		
		String gameId = originalGameId;
		for (int idx=1; idx < args.length; idx++) {
			String username = String.format("Robot %d", idx);
			String accessToken = getAuthenticatedAccessToken(username, robotPassword);
			LOGGER.info(() -> String.format("Logged in user %s", username));
			boolean host = false;
			if (idx == 1 && newGame) {
				host = true;
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
		final String json = OBJECT_WRITER.writeValueAsString(new LoginRequest(username, password));
		LOGGER.fine(() -> String.format("Logging on with payload: %s", json));
		final String loginUrlString = PROP.getProperty("secrethitler.login.url");
		final String response = post(json, loginUrlString, Optional.empty());
		LOGGER.fine(() -> String.format("Received login response: %s", response));
		return OBJECT_MAPPER.readValue(response, LoginResponse.class).getAccessToken();
	}
	
	private static String createNewGame(final String accessToken) throws IOException {
		final String createGameUrlString = PROP.getProperty("secrethitler.creategame.url");
		return post(EMPTY_PAYLOAD, createGameUrlString, Optional.of(accessToken));
	}
	
	private static String post(final String payload, final String urlString, final Optional<String> authorization) throws IOException {
		final URL url = new URL(String.format("%s://%s%s", secureUrl ? "https" : "http", baseUrlString, urlString));
		final HttpURLConnection con = (HttpURLConnection) url.openConnection();
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
	
	public static boolean isSecureUrl() {
		return secureUrl;
	}

	public static String getBaseUrlString() {
		return baseUrlString;
	}

	private SecretHitlerAi() {
		super();
	}
}
