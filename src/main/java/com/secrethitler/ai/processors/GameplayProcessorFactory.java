package com.secrethitler.ai.processors;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class GameplayProcessorFactory {
	private static final Map<Integer, Class<? extends GameplayProcessor>> GAMEPLAY_PROCESSOR_LEVEL_MAP = ImmutableMap.<Integer, Class<? extends GameplayProcessor>>builder()
			.put(1, SimpleGameplayProcessor.class)
			.build();
	
	public static GameplayProcessor getGameplayProcessor(final int level, final String username) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Class<? extends GameplayProcessor> processorClass = GAMEPLAY_PROCESSOR_LEVEL_MAP.get(level);
		if (processorClass == null) {
			throw new IllegalArgumentException("No gameplay handler defined for level " + level);
		}
		return processorClass.getDeclaredConstructor(String.class).newInstance(username);
	}
	
	private GameplayProcessorFactory() {
		super();
	}
}
