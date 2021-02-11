package com.secrethitler.ai.websockets;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secrethitler.ai.SecretHitlerAi;
import com.secrethitler.ai.dtos.GameRequest;

@ClientEndpoint
public class GameSetupWebsocketClientEndpoint extends WebsocketClientEndpoint {
	private String gameId;
	private String accessToken;
	
	public GameSetupWebsocketClientEndpoint(String gameId, String accessToken) {
		this.gameId = gameId;
		this.accessToken = accessToken;
		
		final String gameSetupUrlString = "wss://" + SecretHitlerAi.getBaseUrlString() + SecretHitlerAi.getProp().getProperty("secrethitler.gamesetup.url") +
				"?gameId=" + gameId + "&auth=" + accessToken;
		try {
			setupWebsocketClientEndpoint(new URI(gameSetupUrlString));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@OnOpen
	@Override
    public void onOpen(Session userSession) {
        this.userSession = userSession;
        this.sendMessage("JOIN");
    }
	
	@OnMessage
	@Override
    public void onMessage(String message) {
		System.out.println("Received game setup message: " + message);
		try {
			GameRequest gameRequest = new ObjectMapper().readValue(message, GameRequest.class);
			if (gameRequest.isStarted()) {
				System.out.println("Starting the game!");
				new GamePlayWebsocketClientEndpoint(gameId, accessToken);
				userSession.close();
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
