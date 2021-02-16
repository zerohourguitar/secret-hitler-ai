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
	private static final String PROPERTIES_FILE_NAME = "application.properties";
	private static final String NEW_GAME_COMMAND = "newGame";
	private static final Scanner SCANNER = new Scanner(System.in);
	
	private static Properties prop;
	private static String baseUrlString;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		final String originalGameId = args[0];
		String gameId = originalGameId;
		boolean newGame = NEW_GAME_COMMAND.contentEquals(gameId);
		
		LOGGER.info(() -> getStartupLogMessage(newGame, originalGameId, args.length - 1));
		
		prop = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();           
		InputStream stream = loader.getResourceAsStream(PROPERTIES_FILE_NAME);
		prop.load(stream);
		baseUrlString = prop.getProperty("secrethitler.url");
		
		for (int idx=1; idx < args.length; idx++) {
			try {
				String username = "Robot " + idx;
				String accessToken = getAuthenticatedAccessToken(username, "password");
				LOGGER.info(() -> String.format("Logged in user %s", username));
				boolean host = idx == 1 && newGame;
				if (host) {
					String newGameId = createNewGame(accessToken);
					LOGGER.info(() -> String.format("New game created with id: %s", newGameId));
					gameId = newGameId;
				}
				new GameSetupWebsocketClientEndpoint(gameId, accessToken, Integer.valueOf(args[idx]), username, host);
			} catch (IOException | NumberFormatException | URISyntaxException e) {
				throw new IllegalStateException(e);
			}
		}
		
		Thread.currentThread().join();
	}
	
	private static String getStartupLogMessage(boolean newGame, String gameId, int totalUsers) {
		if (newGame) {
			return String.format("Starting Secret Hitler AI for %d users creating a new game", totalUsers);
		} 
		return String.format("Starting Secret Hitler AI for %d users with gameId %s.", totalUsers, gameId);
	}

	private static String getAuthenticatedAccessToken(final String username, final String password) throws IOException {
		LoginRequest loginRequest = new LoginRequest(username, password);
		final String loginUrlString = prop.getProperty("secrethitler.login.url");
		URL loginUrl = new URL("https://" + baseUrlString + loginUrlString);
		HttpURLConnection con = (HttpURLConnection) loginUrl.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(loginRequest);
		LOGGER.fine(() -> String.format("Logging on with payload: %s", json));
		try(OutputStream os = con.getOutputStream()) {
		    byte[] input = json.getBytes(StandardCharsets.UTF_8);
		    os.write(input, 0, input.length);			
		}
		try(BufferedReader br = new BufferedReader(
				new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder response = new StringBuilder();
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			LOGGER.fine(() -> String.format("Received login response: %s", response.toString()));
			LoginResponse loginResponse = new ObjectMapper().readValue(response.toString(), LoginResponse.class);
			return loginResponse.getAccessToken();
		}
	}
	
	private static String createNewGame(final String accessToken) throws IOException {
		final String createGameUrlString = prop.getProperty("secrethitler.creategame.url");
		URL createGameUrl = new URL("https://" + baseUrlString + createGameUrlString);
		HttpURLConnection con = (HttpURLConnection) createGameUrl.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("authorization", accessToken);
		con.setDoOutput(true);
		try(OutputStream os = con.getOutputStream()) {
		    byte[] input = "{}".getBytes(StandardCharsets.UTF_8);
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

	public static Properties getProp() {
		return prop;
	}

	public static String getBaseUrlString() {
		return baseUrlString;
	}
	
	public static Scanner getScanner() {
		return SCANNER;
	}

}
