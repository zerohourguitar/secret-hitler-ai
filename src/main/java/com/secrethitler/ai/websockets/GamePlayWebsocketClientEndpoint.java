package com.secrethitler.ai.websockets;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.secrethitler.ai.SecretHitlerAi;
import com.secrethitler.ai.dtos.GameData;
import com.secrethitler.ai.dtos.GameplayAction;
import com.secrethitler.ai.dtos.ParticipantGameNotification;
import com.secrethitler.ai.enums.Action;
import com.secrethitler.ai.enums.GamePhase;
import com.secrethitler.ai.processors.GameplayProcessor;
import com.secrethitler.ai.processors.GameplayProcessorFactory;
import com.secrethitler.ai.utils.UriWrapper;

@ClientEndpoint
public class GamePlayWebsocketClientEndpoint extends WebsocketClientEndpoint {
	private static final Logger LOGGER = Logger.getLogger(GamePlayWebsocketClientEndpoint.class.getName());
	protected static final String GAMEPLAY_URL = "secrethitler.gameplay.url";
	protected static final String MOVE_DELAY = "secrethitler.ai.movedelay";
	
	public static class Builder extends WebsocketClientEndpoint.Builder {
		private SecretHitlerAi ai;
		private String accessToken;
		private int level;
		private String username;
		private String gameId;
		private GameplayProcessorFactory gameplayProcessorFactory;
		private Function<GamePlayWebsocketClientEndpoint.Builder, GamePlayWebsocketClientEndpoint> gamePlayClientBuildFunction;
		
		protected Builder() {
			super();
		}
		
		public Builder withAi(final SecretHitlerAi ai) {
			this.ai = ai;
			return this;
		}
		
		public Builder withAccessToken(final String accessToken) {
			this.accessToken = accessToken;
			return this;
		}
		
		public Builder withLevel(final int level) {
			this.level = level;
			return this;
		}
		
		public Builder withUsername(final String username) {
			this.username = username;
			return this;
		}
		
		public Builder withGameId(final String gameId) {
			this.gameId = gameId;
			return this;
		}
		
		public Builder withGameplayProcessorFactory(final GameplayProcessorFactory gameplayProcessorFactory) {
			this.gameplayProcessorFactory = gameplayProcessorFactory;
			return this;
		}
		
		public Builder withGamePlayClientBuildFunction(final Function<GamePlayWebsocketClientEndpoint.Builder, GamePlayWebsocketClientEndpoint> gamePlayClientBuildFunction) {
			this.gamePlayClientBuildFunction = gamePlayClientBuildFunction;
			return this;
		}
		
		public Builder withUriBuilderFunction(final Function<String, UriWrapper> uriBuilderFunction) {
			this.uriBuilderFunction = uriBuilderFunction;
			return this;
		}
		
		public Builder withUriConnectionConsumer(BiConsumer<WebsocketClientEndpoint, UriWrapper> uriConnectionConsumer) {
			this.uriConnectionConsumer = uriConnectionConsumer;
			return this;
		}
		
		@Override
		public GamePlayWebsocketClientEndpoint build() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, URISyntaxException {
			return new GamePlayWebsocketClientEndpoint(this);
		}
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	private final SecretHitlerAi ai;
	private final int moveDelay;
	private final String accessToken;
	private final int level;
	private final GameplayProcessor processor;
	private final String username;
	private final GameplayProcessorFactory gameplayProcessorFactory;
	private final Function<GamePlayWebsocketClientEndpoint.Builder, GamePlayWebsocketClientEndpoint> gamePlayClientBuildFunction;
	
	protected GamePhase previousPhase = null;

	private GamePlayWebsocketClientEndpoint(final Builder builder) throws InstantiationException, IllegalAccessException, URISyntaxException, InvocationTargetException, NoSuchMethodException {	
		super(builder);
		this.ai = builder.ai;
		this.moveDelay = Integer.parseInt(ai.getProp().getProperty(MOVE_DELAY));
		this.accessToken = builder.accessToken;
		this.level = builder.level;
		this.username = builder.username;
		this.gameplayProcessorFactory = builder.gameplayProcessorFactory;
		this.gamePlayClientBuildFunction = builder.gamePlayClientBuildFunction;
		processor = gameplayProcessorFactory.getGameplayProcessor(level, username);
		final String gameSetupUrlString = String.format("%s://%s%s?gameId=%s&auth=%s", ai.isSecureUrl() ? "wss" : "ws",
				ai.getBaseUrlString(), ai.getProp().getProperty(GAMEPLAY_URL), 
				builder.gameId, accessToken);
		setupWebsocketClientEndpoint(builder.uriBuilderFunction.apply(gameSetupUrlString));
	}

	@OnMessage
	@Override
	public void onMessage(String message) {
		LOGGER.fine(() -> String.format("Received Gameplay message: %s", message));
		try {
			ParticipantGameNotification gameNotification = SecretHitlerAi.getObjectMapper().readValue(message, ParticipantGameNotification.class);
			GameData gameData = gameNotification.getGameData();
			String nextGameId = gameData.getNextGameId();
			if (nextGameId != null) {
				LOGGER.info(() -> String.format("%s is joining the next game with id %s", username, nextGameId));
				gamePlayClientBuildFunction.apply(GamePlayWebsocketClientEndpoint.builder()
						.withAi(ai)
						.withGameId(nextGameId)
						.withAccessToken(accessToken)
						.withLevel(level)
						.withUsername(username)
						.withGameplayProcessorFactory(gameplayProcessorFactory)
						.withGamePlayClientBuildFunction(gamePlayClientBuildFunction));
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
					SecretHitlerAi.setGameOver(true);
					LOGGER.info("Press enter when you are ready to start a new game");
		        	SecretHitlerAi.getScanner().nextLine();
		        	SecretHitlerAi.setGameOver(false);
		        	String[] args = {};
		        	GameplayAction newGame = new GameplayAction(Action.NEW_GAME, args);
		            sendMessage(gameplayActionToString(newGame));
		            LOGGER.info("Game has been initiated by the host");
	        	});
	        	t.start();
			} else {
				processor.getActionToTake(gameNotification).ifPresent(this::sendDelayedMessage);
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Exception on a game setup message", e);
		}
	}

	private void sendDelayedMessage(GameplayAction gameplayAction) {
		try {
			Thread.sleep(moveDelay);
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "Exception when delaying the gameplay response message: %s", e);
			Thread.currentThread().interrupt();
		}
		String message = gameplayActionToString(gameplayAction);
		sendMessage(message);
	}
	
	private String gameplayActionToString(final GameplayAction gameplayAction) {
		return gameplayActionToString(gameplayAction, SecretHitlerAi.getObjectWriter());
	}
	
	protected String gameplayActionToString(final GameplayAction gameplayAction, final ObjectWriter writer) {
		try {
			return writer.writeValueAsString(gameplayAction);
		} catch (JsonProcessingException e) {
			LOGGER.log(Level.SEVERE, "Error parsing gameplay response message", e);
		}
		return null;
	}
}
