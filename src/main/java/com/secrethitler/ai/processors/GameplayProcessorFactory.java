package com.secrethitler.ai.processors;

import java.lang.reflect.InvocationTargetException;

public interface GameplayProcessorFactory {
	GameplayProcessor getGameplayProcessor(final int level, final String username) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;
}
