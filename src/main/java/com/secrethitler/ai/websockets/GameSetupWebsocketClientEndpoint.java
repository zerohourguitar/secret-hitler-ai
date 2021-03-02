package com.secrethitler.ai.websockets;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.secrethitler.ai.SecretHitlerAi;
import com.secrethitler.ai.dtos.GameRequest;

@ClientEndpoint
public class GameSetupWebsocketClientEndpoint extends WebsocketClientEndpoint {	
	private static final Logger LOGGER = Logger.getLogger(GameSetupWebsocketClientEndpoint.class.getName());
	
	public static class Builder {
		private SecretHitlerAi ai;
		private String gameId;
		private String accessToken;
		private int gameplayLevel;
		private String username;
		private boolean host;
		
		public Builder() {
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
		
		public GameSetupWebsocketClientEndpoint build() throws URISyntaxException {
			return new GameSetupWebsocketClientEndpoint(this);
		}
		
		@Override
		public boolean equals(Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
		
		@Override
		public int hashCode() {
			return HashCodeBuilder.reflectionHashCode(this);
		}
		
		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
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
	
	private GameSetupWebsocketClientEndpoint(Builder builder) throws URISyntaxException {
		this.ai = builder.ai;
		this.gameId = builder.gameId;
		this.accessToken = builder.accessToken;
		this.gameplayLevel = builder.gameplayLevel;
		this.username = builder.username;
		this.host = builder.host;
		
		final String gameSetupUrlString = String.format("%s://%s%s?gameId=%s&auth=%s", ai.isSecureUrl() ? "wss" : "ws",
				ai.getBaseUrlString(), ai.getProp().getProperty("secrethitler.gamesetup.url"), 
				gameId, accessToken);
		setupWebsocketClientEndpoint(new URI(gameSetupUrlString));
	}

	@OnOpen
	@Override
    public void onOpen(Session userSession) {
        this.userSession = userSession;
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
				new GamePlayWebsocketClientEndpoint(ai, gameId, accessToken, gameplayLevel, username);
				userSession.close();
			}
		} catch (IOException | InstantiationException | IllegalAccessException | URISyntaxException | InvocationTargetException | NoSuchMethodException e) {
			LOGGER.log(Level.SEVERE, "Exception on a game setup message", e);
		}
    }
}
