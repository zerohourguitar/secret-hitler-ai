package com.secrethitler.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.secrethitler.ai.dtos.LoginRequest;
import com.secrethitler.ai.dtos.LoginResponse;
import com.secrethitler.ai.utils.UrlWrapper;
import com.secrethitler.ai.websockets.GameSetupWebsocketClientEndpoint;

public class SecretHitlerAi {
	private static final Logger LOGGER = Logger.getLogger(SecretHitlerAi.class.getName());
	private static final Scanner SCANNER = new Scanner(System.in);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer().withDefaultPrettyPrinter();
	private static final String PROPERTIES_FILE_NAME = "application.properties";
	protected static final Function<String, UrlWrapper> GET_URL_FUNCTION = urlString -> {
		try {
			return new UrlWrapper(urlString);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Bad URL", e);
		}
	};
	protected static final Function<GameSetupWebsocketClientEndpoint.Builder, GameSetupWebsocketClientEndpoint> GAME_SETUP_CLIENT_BUILD_FUNCTION = builder -> {
		try {
			return builder.build();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Bad URI", e);
		}
	};
	protected static final String NEW_GAME_COMMAND = "newGame";
	protected static final String EMPTY_PAYLOAD = "{}"; 
	
	public static void main(String[] args) throws Exception {
		final String originalGameId = args[0];
		List<Integer> aiDifficulties = IntStream.range(1, args.length).boxed()
				.map(index -> args[index])
				.map(Integer::parseInt)
				.collect(Collectors.toList());
		
		new SecretHitlerAi(PROPERTIES_FILE_NAME, GET_URL_FUNCTION, GAME_SETUP_CLIENT_BUILD_FUNCTION, originalGameId, aiDifficulties);
		
		Thread.currentThread().join();
	}
	
	private static String getStartupLogMessage(final boolean newGame, final String gameId, final int totalUsers) {
		if (newGame) {
			return String.format("Starting Secret Hitler AI for %d users creating a new game", totalUsers);
		} 
		return String.format("Starting Secret Hitler AI for %d users with gameId %s.", totalUsers, gameId);
	}
	
	public static Scanner getScanner() {
		return SCANNER;
	}
	
	public static ObjectMapper getObjectMapper() {
		return OBJECT_MAPPER;
	}

	public static ObjectWriter getObjectWriter() {
		return OBJECT_WRITER;
	}

	private final Properties prop;
	private final Function<String, UrlWrapper> getUrlFunction;
	private final boolean secureUrl;
	private final String baseUrlString;

	public SecretHitlerAi(final String propertiesFileName, final Function<String, UrlWrapper> getUrlFunction, 
			Function<GameSetupWebsocketClientEndpoint.Builder, GameSetupWebsocketClientEndpoint> gameSetupClientBuildFunction, 
			final String originalGameId, final List<Integer> aiDifficulties) throws Exception {
		ClassLoader loader = Thread.currentThread().getContextClassLoader(); 
		boolean newGame = NEW_GAME_COMMAND.equals(originalGameId);
		LOGGER.info(() -> getStartupLogMessage(newGame, originalGameId, aiDifficulties.size()));
		prop = new Properties();
		prop.load(loader.getResourceAsStream(propertiesFileName));
		secureUrl = Boolean.valueOf(prop.getProperty("secrethitler.secureurl"));
		baseUrlString = prop.getProperty("secrethitler.url");
		final String robotPassword = prop.getProperty("secrethitler.login.robotpassword");
		this.getUrlFunction = getUrlFunction;
		
		String gameId = originalGameId;
		int idx = 1;
		for (int difficulty : aiDifficulties) {
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
			gameSetupClientBuildFunction.apply(
					GameSetupWebsocketClientEndpoint.builder()
							.withAi(this)
							.withGameId(gameId)
							.withAccessToken(accessToken)
							.withGameplayLevel(difficulty)
							.withUsername(username)
							.withHost(host)
			);
			idx++;
		}
	}
	
	private String getAuthenticatedAccessToken(final String username, final String password) throws IOException {
		final String json = OBJECT_WRITER.writeValueAsString(new LoginRequest(username, password));
		LOGGER.fine(() -> String.format("Logging on with payload: %s", json));
		final String loginUrlString = prop.getProperty("secrethitler.login.url");
		final String response = post(json, loginUrlString, Optional.empty());
		LOGGER.fine(() -> String.format("Received login response: %s", response));
		return OBJECT_MAPPER.readValue(response, LoginResponse.class).getAccessToken();
	}
	
	private String createNewGame(final String accessToken) throws IOException {
		final String createGameUrlString = prop.getProperty("secrethitler.creategame.url");
		return post(EMPTY_PAYLOAD, createGameUrlString, Optional.of(accessToken));
	}
	
	private String post(final String payload, final String urlString, final Optional<String> authorization) throws IOException {
		final UrlWrapper url = getUrlFunction.apply(String.format("%s://%s%s", secureUrl ? "https" : "http", baseUrlString, urlString));
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

	public Properties getProp() {
		return prop;
	}

	public boolean isSecureUrl() {
		return secureUrl;
	}

	public String getBaseUrlString() {
		return baseUrlString;
	}
}
