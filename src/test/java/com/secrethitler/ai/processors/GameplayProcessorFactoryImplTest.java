package com.secrethitler.ai.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

public class GameplayProcessorFactoryImplTest {
	private static final String USERNAME = "testUser";

	private GameplayProcessorFactory factory = new GameplayProcessorFactoryImpl();
	
	@Test
	public void testGetGameplayProcessor_1() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		GameplayProcessor processor = factory.getGameplayProcessor(1, USERNAME);
		assertTrue(processor instanceof SimpleGameplayProcessor);
	}
	
	@Test
	public void testGetGameplayProcessor_2() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		GameplayProcessor processor = factory.getGameplayProcessor(2, USERNAME);
		assertTrue(processor instanceof BooleanDeductionGameplayProcessor);
	}
	
	@Test
	public void testGetGameplayProcessor_3() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		GameplayProcessor processor = factory.getGameplayProcessor(3, USERNAME);
		assertTrue(processor instanceof WeightedDeductionGameplayProcessor);
	}
	
	@Test
	public void testGetGameplayProcessor_InvalidLevel() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		try {
			factory.getGameplayProcessor(-1, USERNAME);
			fail("Expected an IllegalArgumentException to be thrown if an invalid level is given");
		} catch(IllegalArgumentException e) {
			assertEquals("No gameplay handler defined for level -1", e.getMessage());
		}
	}
}
