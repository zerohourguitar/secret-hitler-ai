package com.secrethitler.ai.utils;

import java.util.List;
import java.util.Random;

public class RandomUtilImpl implements RandomUtil {
	private static final Random RANDOM_GENERATOR = new Random();
	
	@Override
	public <T> T getRandomItemFromList(List<T> list) {
		int index = RANDOM_GENERATOR.nextInt(list.size());
		return list.get(index);
	}
}
