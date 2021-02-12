package com.secrethitler.ai.websockets;

import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secrethitler.ai.SecretHitlerAi;
import com.secrethitler.ai.dtos.ParticipantGameNotification;
import com.secrethitler.ai.processors.GameplayProcessor;
import com.secrethitler.ai.processors.GameplayProcessorFactory;

@ClientEndpoint
public class GamePlayWebsocketClientEndpoint extends WebsocketClientEndpoint {
	private GameplayProcessor processor;

	public GamePlayWebsocketClientEndpoint(String gameId, String accessToken, int level) {		
		try {
			processor = GameplayProcessorFactory.getGameplayProcessor(level);
			final String gameSetupUrlString = "wss://" + SecretHitlerAi.getBaseUrlString() + SecretHitlerAi.getProp().getProperty("secrethitler.gameplay.url") +
					"?gameId=" + gameId + "&auth=" + accessToken;
			setupWebsocketClientEndpoint(new URI(gameSetupUrlString));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@OnMessage
	@Override
	public void onMessage(String message) {
		System.out.println("Received Gameplay message: " + message);
		try {
			ParticipantGameNotification participantGameData = new ObjectMapper().readValue(message, ParticipantGameNotification.class);
			processor.getMessageToSend(participantGameData.getGameData()).ifPresent(this::sendMessage);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

}
