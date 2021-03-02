package com.secrethitler.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.secrethitler.ai.websockets.GameSetupWebsocketClientEndpoint;

public class SecretHitlerAiIntTest {
	public class BadGameSetupClient extends GameSetupWebsocketClientEndpoint.Builder {
		@Override
		public GameSetupWebsocketClientEndpoint build() throws URISyntaxException {
			throw new URISyntaxException("Input", "Reason");
		}
	}
	@Test
	public void testMain() throws Exception {
		final String[] args = {"testGameId", "1", "2"};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1); 
		SecretHitlerAi ai = null;
		Future<?> future = executor.submit(() -> {
			try {
				SecretHitlerAi.main(args);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});
		try {
		    future.get(5000, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e){
		    future.cancel(true);
		}
	}
	
	@Test
	public void testBadUrl() {
		try {
			SecretHitlerAi.GET_URL_FUNCTION.apply("BAD_URL");
			fail("Expected an IllegalArgumentException to be thrown if a bad url is given");
		} catch (IllegalArgumentException e) {
			assertEquals("Bad URL", e.getMessage());
		}
	}
	
	@Test
	public void testBadUri() {
		
		try {
			SecretHitlerAi.GAME_SETUP_CLIENT_BUILD_FUNCTION.apply(new BadGameSetupClient());
			fail("Expected an IllegalArgumentException to be thrown if a bad uri is given");
		} catch (IllegalArgumentException e) {
			assertEquals("Bad URI", e.getMessage());
		}
	}
}
