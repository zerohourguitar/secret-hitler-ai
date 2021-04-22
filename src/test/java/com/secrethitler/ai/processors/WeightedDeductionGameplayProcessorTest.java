package com.secrethitler.ai.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.secrethitler.ai.enums.Policy;

public class WeightedDeductionGameplayProcessorTest extends AbstractDeductionGameplayProcessorTest {

	private static final Map<Scenario, Pair<Integer, Integer>> SCENARIO_TO_EXPECTED_SUSPICIONS = ImmutableMap.<Scenario, Pair<Integer, Integer>>builder()
			.put(Scenario.VOTE_LIBERAL_BOT_ONE_SUSPECTED_FASCIST, Pair.of(-3, -1))
			.put(Scenario.VOTE_HITLER_BOT_OPPOSITE_SUSPECTED_MEMBERSHIPS, Pair.of(-1,1))
			.put(Scenario.VOTE_LIBERAL_BOT_SUSPECTED_OPPOSITE_MEMBERSHIPS_KNOWN_FASCIST, Pair.of(-101, -99))
			.put(Scenario.VOTE_LIBERAL_BOT_TWO_KNOWN_OPPOSITES, Pair.of(-1, 1))
			.put(Scenario.VOTE_LIBERAL_BOT_ALL_UNKNOWN, Pair.of(0, 0))
			.put(Scenario.GOVERNMENT_DENIED_LIBERAL_JA, Pair.of(1, -1))
			.put(Scenario.GOVERNMENT_DENIED_LIBERAL_NEIN, Pair.of(-1, 1))
			.put(Scenario.GOVERNMENT_DENIED_HITLER_JA, Pair.of(-1, 1))
			.put(Scenario.GOVERNMENT_DENIED_HITLER_NEIN, Pair.of(1, -1))
			.put(Scenario.ANARCHY, Pair.of(1, -1))
			.put(Scenario.POLICY_PLAYED_NO_POLICY_OPTION, Pair.of(0, 0))
			.put(Scenario.POLICY_PLAYED_WITH_NEIN_VOTER, Pair.of(1, -1))
			.put(Scenario.POLICY_PLAYED_VETO_REQUESTOR, Pair.of(-1, 1000))
			.put(Scenario.POLICY_PLAYED_CHANCELLOR_NO_VETO, Pair.of(-1, -500))
			.put(Scenario.POLICY_PLAYED_PRESIDENT_FAILED_VETO, Pair.of(-1, -1000))
			.put(Scenario.POLICY_PLAYED_PRESIDENT_NO_VETO, Pair.of(-1, -437))
			.put(Scenario.SPECIAL_ELECTION_CHOSEN_SUSPECTED_LIBERAL, Pair.of(1, 2))
			.put(Scenario.SPECIAL_ELECTION_CHOSEN_SUSPECTED_FASCIST, Pair.of(-1, -3))
			.put(Scenario.PRESIDENT_VETOED_KNOWN_LIBERAL, Pair.of(999, 1001))
			.put(Scenario.PRESIDENT_VETOED_KNOWN_FASCIST, Pair.of(-1001, -999))
			.put(Scenario.PRESIDENT_VETOED_SUSPECTED_LIBERAL, Pair.of(1, 1))
			.put(Scenario.PRESIDENT_VETOED_SUSPECTED_FASCIST, Pair.of(-1, -1))
			.put(Scenario.PRESIDENT_VETOED_OPPOSITE_KNOWN_MEMBERSHIPS, Pair.of(-1, 1))
			.put(Scenario.PRESIDENT_VETOED_OPPOSITE_SUSPECTED_MEMBERSHIPS, Pair.of(0, 0))
			.put(Scenario.PLAYER_KILLED_SUSPECTED_FASCIST, Pair.of(-1, 1))
			.put(Scenario.PLAYER_KILLED_SUSPECTED_LIBERAL, Pair.of(1, -1))
			.put(Scenario.PLAYER_KILLED_UNKNOWN_KILLER_UNKNOWN_KILLED, Pair.of(0, 0))
			.put(Scenario.PLAYER_KILLED_SUSPECTED_LIBERAL_KILLER_UNKNOWN_KILLED, Pair.of(0, 1))
			.put(Scenario.PLAYER_KILLED_KNOWN_LIBERAL_KILLER, Pair.of(1, 1))
			.put(Scenario.PRESIDENT_VETO_LIBERAL_ALL_FASCIST, Pair.of(1000, 0))
			.put(Scenario.PRESIDENT_VETO_LIBERAL_ALL_LIBERAL, Pair.of(-1000, 0))
			.put(Scenario.PRESIDENT_VETO_FASCIST_ALL_FASCIST, Pair.of(1000, 0))
			.put(Scenario.PRESIDENT_VETO_FASCIST_ALL_LIBERAL, Pair.of(-1000, 0))
			.put(Scenario.PRESIDENT_VETO_MIXED, Pair.of(0, 0))
			.put(Scenario.PRESIDENT_VETO_KNOWN_LIBERAL_MIXED, Pair.of(1, 0))
			.put(Scenario.PRESIDENT_VETO_KNOWN_FASCIST_ALL_FASCIST, Pair.of(-1, 0))
			.put(Scenario.PLAYER_KILLED_PRESIDENT, Pair.of(0, 0))
			.put(Scenario.PRESIDENT_VETOED_PRESIDENT, Pair.of(0, 1))
			.build();
	
	private WeightedDeductionGameplayProcessor deductionProcessor;

	@Before
	public void setUp() {
		deductionProcessor = new WeightedDeductionGameplayProcessor("Robot 1", randomUtil);
		super.setUp();
	}
	@Override
	protected AbstractDeductionGameplayProcessor getDeductionProcessor() {
		return deductionProcessor;
	}

	@Override
	protected Pair<Integer, Integer> getExpectedSuspicions(Scenario scenario) {
		return SCENARIO_TO_EXPECTED_SUSPICIONS.get(scenario);
	}

	@Test
	public void testSpecialElectionChosen_ResetKnownDiscardedPolicies() {
		deductionProcessor.knownDiscardedPolicies = new ArrayList<>(Arrays.asList(Policy.LIBERAL, Policy.FASCIST));
		gameData.setDeniedPolicies(0);
		
		testSpecialElectionChosen();
		
		assertTrue(deductionProcessor.knownDiscardedPolicies.isEmpty());
	}
	
	@Test
	public void testSpecialElectionChosen_KeepKnownDiscardedPolicies() {
		deductionProcessor.knownDiscardedPolicies = new ArrayList<>(Arrays.asList(Policy.LIBERAL, Policy.FASCIST));
		gameData.setDeniedPolicies(2);
		
		testSpecialElectionChosen();
		
		assertEquals(Arrays.asList(Policy.LIBERAL, Policy.FASCIST), deductionProcessor.knownDiscardedPolicies);
	}
}
