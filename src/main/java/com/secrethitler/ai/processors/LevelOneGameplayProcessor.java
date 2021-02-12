package com.secrethitler.ai.processors;

import java.util.Optional;

import com.secrethitler.ai.dtos.GameData;

public class LevelOneGameplayProcessor implements GameplayProcessor {

	@Override
	public Optional<String> getMessageToSend(GameData gameData) {
		return Optional.empty();
	}

}
