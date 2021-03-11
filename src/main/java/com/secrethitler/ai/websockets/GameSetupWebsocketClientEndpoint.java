package com.secrethitler.ai.websockets;

import java.net.URISyntaxException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import com.secrethitler.ai.SecretHitlerAi;
import com.secrethitler.ai.dtos.GameRequest;
import com.secrethitler.ai.utils.UriWrapper;

@ClientEndpoint
public class GameSetupWebsocketClientEndpoint extends WebsocketClientEndpoint {	
	private static final Logger LOGGER = Logger.getLogger(GameSetupWebsocketClientEndpoint.class.getName());
	protected static final String GAME_SETUP_URL = "secrethitler.gamesetup.url";
	protected static final Function<GamePlayWebsocketClientEndpoint.Builder, GamePlayWebsocketClientEndpoint> GAME_PLAY_CLIENT_BUILD_FUNCTION =
			builder -> {
				try {
					return builder.build();
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			};
	
	public static class Builder extends WebsocketClientEndpoint.Builder {
		private SecretHitlerAi ai;
		private String gameId;
		private String accessToken;
		private int gameplayLevel;
		private String username;
		private boolean host;
		private Function<GamePlayWebsocketClientEndpoint.Builder, GamePlayWebsocketClientEndpoint> gamePlayClientBuildFunction = GAME_PLAY_CLIENT_BUILD_FUNCTION;
		
		protected Builder() {
			super();
		}
		
		public Builder withAi(final SecretHitlerAi ai) {
			this.ai = ai;
			return this;
		}
		
		public Builder withGameId(final String gameId) {
			this.gameId = gameId;
			return this;
		}
		
		public Builder withAccessToken(final String accessToken) {
			this.accessToken = accessToken;
			return this;
		}
		
		public Builder withGameplayLevel(final int gameplayLevel) {
			this.gameplayLevel = gameplayLevel;
			return this;
		}
		
		public Builder withUsername(final String username) {
			this.username = username;
			return this;
		}
		
		public Builder withHost(final boolean host) {
			this.host = host;
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
		public GameSetupWebsocketClientEndpoint build() throws URISyntaxException {
			return new GameSetupWebsocketClientEndpoint(this);
		}
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	private final SecretHitlerAi ai;
	private final String gameId;
	private final String accessToken;
	private final int gameplayLevel;
	private final String username;
	private final boolean host;
	private final Function<GamePlayWebsocketClientEndpoint.Builder, GamePlayWebsocketClientEndpoint> gamePlayClientBuildFunction;
	
	private GameSetupWebsocketClientEndpoint(Builder builder) throws URISyntaxException {
		super(builder);
		this.ai = builder.ai;
		this.gameId = builder.gameId;
		this.accessToken = builder.accessToken;
		this.gameplayLevel = builder.gameplayLevel;
		this.username = builder.username;
		this.host = builder.host;
		this.gamePlayClientBuildFunction = builder.gamePlayClientBuildFunction;
		
		final String gameSetupUrlString = String.format("%s://%s%s?gameId=%s&auth=%s", ai.isSecureUrl() ? "wss" : "ws",
				ai.getBaseUrlString(), ai.getProp().getProperty(GAME_SETUP_URL), 
				gameId, accessToken);
		setupWebsocketClientEndpoint(builder.uriBuilderFunction.apply(gameSetupUrlString));
	}

	@OnOpen
	@Override
    public void onOpen(Session userSession) {
        super.onOpen(userSession);
        if (host) {
        	Thread t = new Thread(() -> {
	        	LOGGER.info("Press enter when you are ready to start the game");
	        	SecretHitlerAi.getScanner().nextLine();
	            sendMessage("START");
	            LOGGER.info("Game has been initiated by the host");
        	});
        	t.start();
        } else {
	        sendMessage("JOIN");
        }
        LOGGER.info(() -> String.format("%s joined the game session", username));
    }
	
	@OnMessage
	@Override
    public void onMessage(String message) {
		LOGGER.fine(() -> String.format("Received game setup message: %s", message));
		try {
			GameRequest gameRequest = SecretHitlerAi.getObjectMapper().readValue(message, GameRequest.class);
			if (gameRequest.isStarted()) {
				LOGGER.info(() -> String.format("%s is starting the game!", username));
				gamePlayClientBuildFunction.apply(GamePlayWebsocketClientEndpoint.builder()
						.withAi(ai)
						.withGameId(gameId)
						.withAccessToken(accessToken)
						.withLevel(gameplayLevel)
						.withUsername(username)
						.withGameplayProcessorFactory(SecretHitlerAi.getGameplayProcessorFactory())
						.withGamePlayClientBuildFunction(gamePlayClientBuildFunction));
				userSession.close();
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception on a game setup message", e);
		}
    }
}
