package dev.ghost;

import java.util.List;

public record RunState(
    GameState game,
    String generation,
    String segment,
    long revision,
    String player,
    String action,
    int decisions,
    int maxDecisions,
    long deadline,
    long observedAt,
    long decisionStarted,
    String stopReason,
    List<Integer> stack,
    List<ActionLog> log) {
  public RunState {
    stack = List.copyOf(stack);
    log = List.copyOf(log);
  }
}
