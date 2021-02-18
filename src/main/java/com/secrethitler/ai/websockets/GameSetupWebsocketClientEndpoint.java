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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secrethitler.ai.SecretHitlerAi;
import com.secrethitler.ai.dtos.GameRequest;

@ClientEndpoint
public class GameSetupWebsocketClientEndpoint extends WebsocketClientEndpoint {	
	private static final Logger LOGGER = Logger.getLogger(GameSetupWebsocketClientEndpoint.class.getName());
	
	private final String gameId;
	private final String accessToken;
	private final int gameplayLevel;
	private final String username;
	private final boolean host;
	
	public GameSetupWebsocketClientEndpoint(final String gameId, final String accessToken, final int gameplayLevel, final String username, final boolean host) throws URISyntaxException {
		this.gameId = gameId;
		this.accessToken = accessToken;
		this.gameplayLevel = gameplayLevel;
		this.username = username;
		this.host = host;
		
		final String gameSetupUrlString = String.format("%s://%s%s?gameId=%s&auth=%s", SecretHitlerAi.isSecureUrl() ? "wss" : "ws",
				SecretHitlerAi.getBaseUrlString(), SecretHitlerAi.getProp().getProperty("secrethitler.gamesetup.url"), 
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
			GameRequest gameRequest = new ObjectMapper().readValue(message, GameRequest.class);
			if (gameRequest.isStarted()) {
				LOGGER.info(() -> String.format("%s is starting the game!", username));
				new GamePlayWebsocketClientEndpoint(gameId, accessToken, gameplayLevel, username);
				userSession.close();
			}
		} catch (IOException | InstantiationException | IllegalAccessException | URISyntaxException | InvocationTargetException | NoSuchMethodException e) {
			LOGGER.log(Level.SEVERE, "Exception on a game setup message", e);
		}
    }
}
