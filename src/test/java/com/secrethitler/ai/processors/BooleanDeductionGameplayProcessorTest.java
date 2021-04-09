package com.secrethitler.ai.processors;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableMap;

public class BooleanDeductionGameplayProcessorTest extends AbstractDeductionGameplayProcessorTest {
	private static final Map<Scenario, Pair<Integer, Integer>> SCENARIO_TO_EXPECTED_SUSPICIONS = ImmutableMap.<Scenario, Pair<Integer, Integer>>builder()
			.put(Scenario.VOTE_LIBERAL_BOT_ONE_SUSPECTED_FASCIST, Pair.of(-1, -1))
			.put(Scenario.VOTE_HITLER_BOT_OPPOSITE_SUSPECTED_MEMBERSHIPS, Pair.of(0,0))
			.put(Scenario.VOTE_LIBERAL_BOT_SUSPECTED_OPPOSITE_MEMBERSHIPS_KNOWN_FASCIST, Pair.of(-1, -1))
			.put(Scenario.VOTE_LIBERAL_BOT_TWO_KNOWN_OPPOSITES, Pair.of(-1, 1))
			.put(Scenario.VOTE_LIBERAL_BOT_ALL_UNKNOWN, Pair.of(0, 0))
			.put(Scenario.GOVERNMENT_DENIED_LIBERAL_JA, Pair.of(1, -1))
			.put(Scenario.GOVERNMENT_DENIED_LIBERAL_NEIN, Pair.of(-1, 1))
			.put(Scenario.GOVERNMENT_DENIED_HITLER_JA, Pair.of(-1, 1))
			.put(Scenario.GOVERNMENT_DENIED_HITLER_NEIN, Pair.of(1, -1))
			.put(Scenario.ANARCHY, Pair.of(1, -1))
			.put(Scenario.POLICY_PLAYED_NO_POLICY_OPTION, Pair.of(0, 0))
			.put(Scenario.LIBERAL_POLICY_PLAYED, Pair.of(1, -1))
			.put(Scenario.FASCIST_POLICY_PLAYED, Pair.of(-1, 1))
			.put(Scenario.POLICY_PLAYED_VETO_REQUESTOR, Pair.of(-1, 1))
			.put(Scenario.SPECIAL_ELECTION_CHOSEN_SUSPECTED_LIBERAL, Pair.of(1, 1))
			.put(Scenario.SPECIAL_ELECTION_CHOSEN_SUSPECTED_FASCIST, Pair.of(-1, -1))
			.put(Scenario.PRESIDENT_VETOED_KNOWN_LIBERAL, Pair.of(1, 1))
			.put(Scenario.PRESIDENT_VETOED_KNOWN_FASCIST, Pair.of(-1, -1))
			.put(Scenario.PRESIDENT_VETOED_SUSPECTED_LIBERAL, Pair.of(1, 1))
			.put(Scenario.PRESIDENT_VETOED_SUSPECTED_FASCIST, Pair.of(-1, -1))
			.put(Scenario.PRESIDENT_VETOED_OPPOSITE_KNOWN_MEMBERSHIPS, Pair.of(-1, 1))
			.put(Scenario.PRESIDENT_VETOED_OPPOSITE_SUSPECTED_MEMBERSHIPS, Pair.of(0, 0))
			.put(Scenario.PLAYER_KILLED_SUSPECTED_FASCIST, Pair.of(-1, 1))
			.put(Scenario.PLAYER_KILLED_SUSPECTED_LIBERAL, Pair.of(1, -1))
			.put(Scenario.PLAYER_KILLED_UNKNOWN_KILLER_UNKNOWN_KILLED, Pair.of(0, 0))
			.put(Scenario.PLAYER_KILLED_SUSPECTED_LIBERAL_KILLER_UNKNOWN_KILLED, Pair.of(0, 1))
			.put(Scenario.PLAYER_KILLED_KNOWN_LIBERAL_KILLER, Pair.of(1, 1))
			.put(Scenario.PRESIDENT_VETO_LIBERAL_ALL_FASCIST, Pair.of(1, 0))
			.put(Scenario.PRESIDENT_VETO_LIBERAL_ALL_LIBERAL, Pair.of(-1, 0))
			.put(Scenario.PRESIDENT_VETO_FASCIST_ALL_FASCIST, Pair.of(1, 0))
			.put(Scenario.PRESIDENT_VETO_FASCIST_ALL_LIBERAL, Pair.of(-1, 0))
			.put(Scenario.PRESIDENT_VETO_MIXED, Pair.of(0, 0))
			.put(Scenario.PRESIDENT_VETO_KNOWN_LIBERAL_MIXED, Pair.of(1, 0))
			.put(Scenario.PRESIDENT_VETO_KNOWN_FASCIST_ALL_FASCIST, Pair.of(-1, 0))
			.put(Scenario.PLAYER_KILLED_PRESIDENT, Pair.of(0, 0))
			.put(Scenario.PRESIDENT_VETOED_PRESIDENT, Pair.of(0, 1))
			.build();

	@Override
	protected AbstractDeductionGameplayProcessor getDeductionProcessor() {
		return new BooleanDeductionGameplayProcessor("Robot 1", randomUtil);
	}

	@Override
	protected Pair<Integer, Integer> getExpectedSuspicions(Scenario scenario) {
		return SCENARIO_TO_EXPECTED_SUSPICIONS.get(scenario);
	}

}
