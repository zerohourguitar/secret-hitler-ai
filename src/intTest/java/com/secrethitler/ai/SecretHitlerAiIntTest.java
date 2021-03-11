package com.secrethitler.ai;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.Test;

import com.secrethitler.ai.websockets.GameSetupWebsocketClientEndpoint;

public class SecretHitlerAiIntTest {
	private static final Logger LOGGER = Logger.getLogger(SecretHitlerAiIntTest.class.getName());
	private static final int TIMEOUT_SECONDS = 120;
	
	@Test
	public void testMain() throws Exception {
		final String[] args = {"newGame", "1", "2", "1", "2", "1"};
		PipedOutputStream userWriter = new PipedOutputStream();
		PipedInputStream userIn = new PipedInputStream(userWriter);
		System.setIn(userIn);
		PipedInputStream gameSetupReader = new PipedInputStream();
		PipedOutputStream systemOut = new PipedOutputStream(gameSetupReader);
		Handler handler = new StreamHandler(systemOut, new SimpleFormatter());
		handler.setLevel(Level.ALL);
		Logger logger = Logger.getLogger(GameSetupWebsocketClientEndpoint.class.getName());
		logger.setLevel(Level.ALL);
		logger.setUseParentHandlers(false);
		logger.addHandler(handler);
		
		Thread t = new Thread(() -> {
			try {
				SecretHitlerAi.main(args);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});
		t.start();
		try (BufferedReader systemReader = new BufferedReader(new InputStreamReader(gameSetupReader))) {
			boolean gameStarted = false;
			while (!gameStarted) {
				final String out = systemReader.readLine();
				LOGGER.info(out);
				if ("INFO: Robot 5 joined the game session".equals(out)) {
					Thread.sleep(500);
					userWriter.write("\r\n".getBytes());
					gameStarted = true;
				}
			}
		}
		
		int secondsPassed = 0;
		while (!SecretHitlerAi.isGameOver() && secondsPassed < TIMEOUT_SECONDS) {
			Thread.sleep(1000);
			secondsPassed++;
		}
		
		SecretHitlerAi.stopGame();
		t.join();
		
		if (secondsPassed >= TIMEOUT_SECONDS) {
			fail(String.format("Game timed out after %d seconds", secondsPassed));
		}
	}
}
