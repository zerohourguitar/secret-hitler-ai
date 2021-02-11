package com.secrethitler.ai.websockets;

import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secrethitler.ai.SecretHitlerAi;
import com.secrethitler.ai.dtos.ParticipantGameNotification;

@ClientEndpoint
public class GamePlayWebsocketClientEndpoint extends WebsocketClientEndpoint {

	public GamePlayWebsocketClientEndpoint(String gameId, String accessToken) {		
		final String gameSetupUrlString = "wss://" + SecretHitlerAi.getBaseUrlString() + SecretHitlerAi.getProp().getProperty("secrethitler.gameplay.url") +
				"?gameId=" + gameId + "&auth=" + accessToken;
		try {
			setupWebsocketClientEndpoint(new URI(gameSetupUrlString));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@OnMessage
	@Override
	public void onMessage(String message) {
		System.out.println("Received Gameplay message: " + message);
		try {
			ParticipantGameNotification participantGameData = new ObjectMapper().readValue(message, ParticipantGameNotification.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

}
