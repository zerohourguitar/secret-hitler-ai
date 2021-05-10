package com.secrethitler.ai.processors;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.secrethitler.ai.utils.RandomUtil;
import com.secrethitler.ai.utils.RandomUtilImpl;

public class GameplayProcessorFactoryImpl implements GameplayProcessorFactory {
	protected static final RandomUtil RANDOM_UTIL = new RandomUtilImpl();
	private static final Map<Integer, Class<? extends GameplayProcessor>> GAMEPLAY_PROCESSOR_LEVEL_MAP = ImmutableMap.<Integer, Class<? extends GameplayProcessor>>builder()
			.put(1, SimpleGameplayProcessor.class)
			.put(2, BooleanDeductionGameplayProcessor.class)
			.put(3, WeightedDeductionGameplayProcessor.class)
			.put(4, DeceptionGameplayProcessor.class)
			.build();
	
	@Override
	public GameplayProcessor getGameplayProcessor(final int level, final String username) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Class<? extends GameplayProcessor> processorClass = GAMEPLAY_PROCESSOR_LEVEL_MAP.get(level);
		if (processorClass == null) {
			throw new IllegalArgumentException("No gameplay handler defined for level " + level);
		}
		return processorClass.getDeclaredConstructor(String.class, RandomUtil.class).newInstance(username, RANDOM_UTIL);
	}
}
