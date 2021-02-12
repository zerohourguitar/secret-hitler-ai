package com.secrethitler.ai.processors;

import java.util.Optional;

import com.secrethitler.ai.dtos.GameData;

public interface GameplayProcessor {
	Optional<String> getMessageToSend(final GameData gameData);
}
