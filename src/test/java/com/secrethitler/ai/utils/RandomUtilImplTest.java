package com.secrethitler.ai.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

public class RandomUtilImplTest {
	private RandomUtil rand = new RandomUtilImpl();
	
	@Test
	public void testGetRandomItemFromList() {
		List<String> list = Arrays.asList("Test1", "Test2", "Test3", "Test4");
		
		final String result = rand.getRandomItemFromList(list);
		
		assertNotNull(result);
		assertTrue(new HashSet<>(list).contains(result));
	}
}
