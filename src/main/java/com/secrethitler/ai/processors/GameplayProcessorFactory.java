package com.secrethitler.ai.processors;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class GameplayProcessorFactory {
	private static final Map<Integer, Class<? extends GameplayProcessor>> GAMEPLAY_PROCESSOR_LEVEL_MAP = ImmutableMap.<Integer, Class<? extends GameplayProcessor>>builder()
			.put(1, LevelOneGameplayProcessor.class)
			.build();
	
	public static GameplayProcessor getGameplayProcessor(final int level) throws InstantiationException, IllegalAccessException {
		Class<? extends GameplayProcessor> processorClass = GAMEPLAY_PROCESSOR_LEVEL_MAP.get(level);
		if (processorClass == null) {
			throw new IllegalArgumentException("No gameplay handler defined for level " + level);
		}
		return processorClass.newInstance();
	}
}
