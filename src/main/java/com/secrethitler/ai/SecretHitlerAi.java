package com.secrethitler.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.secrethitler.ai.dtos.LoginRequest;
import com.secrethitler.ai.dtos.LoginResponse;
import com.secrethitler.ai.websockets.GameSetupWebsocketClientEndpoint;

public class SecretHitlerAi {
	private static final String PROPERTIES_FILE_NAME = "application.properties";
	
	private static Properties prop;
	private static String baseUrlString;
	
	public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
		final String username = args[0];
		final String password = args[1];
		final String gameId = args[2];
		final int gameplayLevel = Integer.valueOf(args[3]);
		System.out.println("Starting Secret Hitler AI for user " + username + " with gameId " + gameId + "and gameplayLevel " + gameplayLevel);
		
		prop = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();           
		InputStream stream = loader.getResourceAsStream(PROPERTIES_FILE_NAME);
		prop.load(stream);
		baseUrlString = prop.getProperty("secrethitler.url");
		
		final String accessToken = getAuthenticatedAccessToken(username, password);
		new GameSetupWebsocketClientEndpoint(gameId, accessToken, gameplayLevel);
        
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
		System.out.println("Logging on with payload: " + json);
		try(OutputStream os = con.getOutputStream()) {
		    byte[] input = json.getBytes("utf-8");
		    os.write(input, 0, input.length);			
		}
		try(BufferedReader br = new BufferedReader(
				new InputStreamReader(con.getInputStream(), "utf-8"))) {
			StringBuilder response = new StringBuilder();
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			System.out.println("Received login response: " + response.toString());
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
