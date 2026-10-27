package dev.ghost;

import java.util.*;

public final class Runs {
  private Runs() {}

  public static RunState create(
      GameState g,
      String generation,
      long revision,
      String player,
      String action,
      int limit,
      long millis) {
    long now = System.nanoTime();
    return new RunState(
        g,
        generation,
        UUID.randomUUID().toString(),
        revision,
        player,
        action,
        0,
        limit,
        now + millis * 1_000_000,
        now,
        now,
        "",
        List.of(g.room()),
        List.of());
  }

  public static RunState control(RunState r, long now, String reason) {
    return new RunState(
        r.game(),
        r.generation(),
        r.segment(),
        r.revision(),
        r.player(),
        r.action(),
        r.decisions(),
        r.maxDecisions(),
        r.deadline(),
        now,
        now,
        reason,
        r.stack(),
        r.log());
  }

  public static String endReason(RunState r) {
    if (!r.stopReason().isEmpty()) return r.stopReason();
    if (!r.game().status().equals("PLAYING")) return r.game().status();
    if (r.decisions() >= r.maxDecisions()) return "DECISION_LIMIT";
    if (r.observedAt() >= r.deadline()) return "TIME_LIMIT";
    return "";
  }

  public static RunState apply(ActionTurn t) {
    RunState r = t.run();
    GameState g = Rules.act(r.game(), t.action());
    List<ActionLog> log = new ArrayList<>(r.log());
    log.add(
        new ActionLog(
            r.decisions() + 1,
            r.game().level(),
            r.game().room(),
            r.game().direction(),
            t.action(),
            g.message(),
            t.latencyMs(),
            r.player()));
    return new RunState(
        g,
        r.generation(),
        r.segment(),
        r.revision() + 1,
        r.player(),
        t.action(),
        r.decisions() + (t.action().equals("START") ? 0 : 1),
        r.maxDecisions(),
        r.deadline(),
        r.observedAt(),
        r.decisionStarted(),
        "",
        Baselines.advanceStack(r, g),
        log);
  }
}
