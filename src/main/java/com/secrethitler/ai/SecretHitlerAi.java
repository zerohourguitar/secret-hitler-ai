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
import java.util.logging.Logger;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.secrethitler.ai.dtos.LoginRequest;
import com.secrethitler.ai.dtos.LoginResponse;
import com.secrethitler.ai.websockets.GameSetupWebsocketClientEndpoint;

public class SecretHitlerAi {
	private static final Logger LOGGER = Logger.getLogger(SecretHitlerAi.class.getName());
	private static final String PROPERTIES_FILE_NAME = "application.properties";
	
	private static Properties prop;
	private static String baseUrlString;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		final String gameId = args[0];
		
		LOGGER.info(() -> String.format("Starting Secret Hitler AI for %d users with gameId %s.", args.length - 1, gameId));
		
		prop = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();           
		InputStream stream = loader.getResourceAsStream(PROPERTIES_FILE_NAME);
		prop.load(stream);
		baseUrlString = prop.getProperty("secrethitler.url");
		
		IntStream.range(1, args.length).forEach(idx -> {
			try {
				String username = "Robot " + idx;
				String accessToken = getAuthenticatedAccessToken(username, "password");
				LOGGER.info(String.format("Logged in user %s", username));
				new GameSetupWebsocketClientEndpoint(gameId, accessToken, Integer.valueOf(args[idx]), username);
			} catch (IOException | NumberFormatException | URISyntaxException e) {
				throw new IllegalStateException(e);
			}
		});
		
		Thread.currentThread().join();
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

	public static Properties getProp() {
		return prop;
	}

	public static String getBaseUrlString() {
		return baseUrlString;
	}

}
