package com.secrethitler.ai.utils;

import java.net.URISyntaxException;

import com.secrethitler.ai.websockets.GameSetupWebsocketClientEndpoint;

public class BadGameSetupClientBuilder extends GameSetupWebsocketClientEndpoint.Builder {
	@Override
	public GameSetupWebsocketClientEndpoint build() throws URISyntaxException {
		throw new URISyntaxException("Input", "Reason");
	}
}
