package com.secrethitler.ai.websockets;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secrethitler.ai.SecretHitlerAi;
import com.secrethitler.ai.dtos.ParticipantGameNotification;
import com.secrethitler.ai.processors.GameplayProcessor;
import com.secrethitler.ai.processors.GameplayProcessorFactory;

@ClientEndpoint
public class GamePlayWebsocketClientEndpoint extends WebsocketClientEndpoint {
	private static final Logger LOGGER = Logger.getLogger(GamePlayWebsocketClientEndpoint.class.getName());
	private static final int MOVE_DELAY = Integer.parseInt(SecretHitlerAi.getProp().getProperty("secrethitler.ai.movedelay"));
	
	private String accessToken;
	private int level;
	private GameplayProcessor processor;
	private boolean makingAMove = false;
	private String username;

	public GamePlayWebsocketClientEndpoint(String gameId, String accessToken, int level, String username) throws InstantiationException, IllegalAccessException, URISyntaxException, InvocationTargetException, NoSuchMethodException {	
		this.accessToken = accessToken;
		this.level = level;
		this.username = username;
		processor = GameplayProcessorFactory.getGameplayProcessor(level, username);
		final String gameSetupUrlString = "wss://" + SecretHitlerAi.getBaseUrlString() + SecretHitlerAi.getProp().getProperty("secrethitler.gameplay.url") +
				"?gameId=" + gameId + "&auth=" + accessToken;
		setupWebsocketClientEndpoint(new URI(gameSetupUrlString));
	}

	@OnMessage
	@Override
	public void onMessage(String message) {
		LOGGER.fine(() -> String.format("Received Gameplay message: %s", message));
		if (makingAMove) {
			return;
		}
		try {
			ParticipantGameNotification participantGameData = new ObjectMapper().readValue(message, ParticipantGameNotification.class);
			String nextGameId = participantGameData.getGameData().getNextGameId();
			if (nextGameId != null) {
				LOGGER.info(() -> String.format("%s is joining the next game with id %s", username, nextGameId));
				new GamePlayWebsocketClientEndpoint(nextGameId, accessToken, level, username);
				userSession.close();
				return;
			}
			processor.getMessageToSend(participantGameData).ifPresent(this::sendDelayedMessage);
		} catch (IOException | InstantiationException | IllegalAccessException | URISyntaxException | InvocationTargetException | NoSuchMethodException e) {
			LOGGER.log(Level.SEVERE, "Exception on a game setup message", e);
		}
	}

	private void sendDelayedMessage(String message) {
		makingAMove = true;
		try {
			Thread.sleep(MOVE_DELAY);
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "Exception when delaying the gameplay response message: %s", e);
			Thread.currentThread().interrupt();
		}
		sendMessage(message);
		makingAMove = false;
	}
}
