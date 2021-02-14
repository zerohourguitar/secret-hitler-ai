package com.secrethitler.ai.websockets;

import java.io.IOException;
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
	private static final int MOVE_DELAY = Integer.valueOf(SecretHitlerAi.getProp().getProperty("secrethitler.ai.movedelay"));
	private String accessToken;
	private int level;
	private GameplayProcessor processor;
	private boolean makingAMove = false;

	public GamePlayWebsocketClientEndpoint(String gameId, String accessToken, int level) {	
		this.accessToken = accessToken;
		this.level = level;
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
		if (makingAMove) {
			return;
		}
		try {
			ParticipantGameNotification participantGameData = new ObjectMapper().readValue(message, ParticipantGameNotification.class);
			String nextGameId = participantGameData.getGameData().getNextGameId();
			if (nextGameId != null) {
				new GamePlayWebsocketClientEndpoint(nextGameId, accessToken, level);
				userSession.close();
				return;
			}
			processor.getMessageToSend(participantGameData).ifPresent(this::sendDelayedMessage);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendDelayedMessage(String message) {
		makingAMove = true;
		try {
			Thread.sleep(MOVE_DELAY);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		sendMessage(message);
		makingAMove = false;
	}
}
