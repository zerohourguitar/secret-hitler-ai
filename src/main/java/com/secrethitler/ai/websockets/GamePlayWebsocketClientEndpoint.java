package com.secrethitler.ai.websockets;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secrethitler.ai.SecretHitlerAi;
import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.ParticipantGameNotification;
import com.secrethitler.ai.enums.Action;
import com.secrethitler.ai.enums.GamePhase;
import com.secrethitler.ai.processors.GameplayProcessor;
import com.secrethitler.ai.processors.GameplayProcessorFactory;

@ClientEndpoint
public class GamePlayWebsocketClientEndpoint extends WebsocketClientEndpoint {
	private static final Logger LOGGER = Logger.getLogger(GamePlayWebsocketClientEndpoint.class.getName());
	private static final int MOVE_DELAY = Integer.parseInt(SecretHitlerAi.getProp().getProperty("secrethitler.ai.movedelay"));
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	private final String accessToken;
	private final int level;
	private final GameplayProcessor processor;
	private final String username;
	private GamePhase previousPhase = null;

	public GamePlayWebsocketClientEndpoint(final String gameId, final String accessToken, final int level, final String username) throws InstantiationException, IllegalAccessException, URISyntaxException, InvocationTargetException, NoSuchMethodException {	
		this.accessToken = accessToken;
		this.level = level;
		this.username = username;
		processor = GameplayProcessorFactory.getGameplayProcessor(level, username);
		final String gameSetupUrlString = String.format("wss://%s%s?gameId=%s&auth=%s", 
				SecretHitlerAi.getBaseUrlString(), SecretHitlerAi.getProp().getProperty("secrethitler.gameplay.url"), 
				gameId, accessToken);
		setupWebsocketClientEndpoint(new URI(gameSetupUrlString));
	}

	@OnMessage
	@Override
	public void onMessage(String message) {
		LOGGER.fine(() -> String.format("Received Gameplay message: %s", message));
		try {
			ParticipantGameNotification gameNotification = OBJECT_MAPPER.readValue(message, ParticipantGameNotification.class);
			GameData gameData = gameNotification.getGameData();
			String nextGameId = gameData.getNextGameId();
			if (nextGameId != null) {
				LOGGER.info(() -> String.format("%s is joining the next game with id %s", username, nextGameId));
				new GamePlayWebsocketClientEndpoint(nextGameId, accessToken, level, username);
				userSession.close();
				return;
			}
			GamePhase currentPhase = gameData.getPhase();
			if (previousPhase == currentPhase) {
				return;
			}
			previousPhase = currentPhase;
			if (GamePhase.GAME_OVER == currentPhase && gameData.getMyPlayer().isHost()) {
				Thread t = new Thread(() -> {
		        	LOGGER.info("Press enter when you are ready to start a new game");
		        	SecretHitlerAi.getScanner().nextLine();
		        	String[] args = {};
		        	GameplayAction newGame = new GameplayAction(Action.NEW_GAME, args);
		            sendMessage(gameplayActionToString(newGame));
		            LOGGER.info("Game has been initiated by the host");
	        	});
	        	t.start();
			}
			processor.getActionToTake(gameNotification).ifPresent(this::sendDelayedMessage);
		} catch (IOException | InstantiationException | IllegalAccessException | URISyntaxException | InvocationTargetException | NoSuchMethodException e) {
			LOGGER.log(Level.SEVERE, "Exception on a game setup message", e);
		}
	}

	private void sendDelayedMessage(GameplayAction gameplayAction) {
		try {
			Thread.sleep(MOVE_DELAY);
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "Exception when delaying the gameplay response message: %s", e);
			Thread.currentThread().interrupt();
		}
		String message = gameplayActionToString(gameplayAction);
		sendMessage(message);
	}
	
	protected String gameplayActionToString(GameplayAction gameplayAction) {
		try {
			return OBJECT_MAPPER.writer().withDefaultPrettyPrinter().writeValueAsString(gameplayAction);
		} catch (JsonProcessingException e) {
			LOGGER.log(Level.SEVERE, "Error parsing gameplay response message", e);
		}
		return null;
	}
}
