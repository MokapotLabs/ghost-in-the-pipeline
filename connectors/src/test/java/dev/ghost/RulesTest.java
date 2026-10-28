package dev.ghost;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class RulesTest {
  @Test
  void seededGenerationIsDeterministicConnectedAndSolvableWithinSevenEntries() {
    for (long seed = 0; seed < 200; seed++)
      for (int level = 1; level <= 3; level++) {
        GameState g = Rules.newGame(seed, level, level);
        assertEquals(g, Rules.newGame(seed, level, level));
        Rules.validateSolvable(g);
        for (int r = 0; r < g.maze().size() * g.maze().size(); r++)
          for (int d = 0; d < 4; d++) {
            int q = Rules.neighbor(r, d, g.maze().size());
            if (q < 0) assertTrue(Rules.wall(g.maze(), r, d));
            else assertEquals(Rules.wall(g.maze(), r, d), Rules.wall(g.maze(), q, (d + 2) % 4));
          }
      }
  }

  @Test
  void turnsAndCollisionsDoNotConsumeEntriesAndTransitionsAreDeterministic() {
    GameState g = Rules.newGame(1983, 1, 3);
    var entries = g.remaining();
    int room = g.room();
    GameState right = Rules.act(g, "TURN_RIGHT");
    assertEquals((g.direction() + 1) % 4, right.direction());
    assertEquals(room, right.room());
    assertEquals(entries, right.remaining());
    assertEquals(g.direction(), Rules.act(right, "TURN_LEFT").direction());
    for (int i = 0; i < 4; i++) {
      if (Rules.wall(g.maze(), g.room(), g.direction())
          && !Rules.exit(g.maze(), g.room(), g.direction())) break;
      g = Rules.act(g, "TURN_RIGHT");
    }
    GameState collision = Rules.act(g, "FORWARD");
    assertEquals(g.room(), collision.room());
    assertEquals(g.remaining(), collision.remaining());
    assertEquals(g.collisions() + 1, collision.collisions());
    assertEquals(collision, Rules.act(g, "FORWARD"));
  }

  @Test
  void validMovementConsumesTargetAllowanceAndSeventhEntryLoses() {
    GameState g = Rules.newGame(2, 1, 1);
    while (Rules.wall(g.maze(), g.room(), g.direction())) g = Rules.act(g, "TURN_RIGHT");
    int q = Rules.neighbor(g.room(), g.direction(), 3);
    assertTrue(q >= 0);
    GameState moved = Rules.act(g, "FORWARD");
    assertEquals(q, moved.room());
    assertEquals(6, moved.remaining().get(q));
    List<Integer> remaining = new ArrayList<>(g.remaining());
    remaining.set(q, 1);
    GameState last =
        new GameState(
            g.seed(),
            g.level(),
            g.lastLevel(),
            g.maze(),
            g.room(),
            g.direction(),
            remaining,
            g.memory(),
            g.status(),
            0,
            0,
            0,
            0,
            1,
            0,
            "START",
            "");
    GameState lost = Rules.act(last, "FORWARD");
    assertEquals("LOST", lost.status());
    assertEquals(0, lost.remaining().get(q));
    assertEquals(lost.room(), Rules.act(lost, "FORWARD").room());
  }

  @Test
  void levelTransitionsAndFinalVictory() {
    GameState g = Rules.newGame(2026, 1, 3);
    for (int level = 1; level <= 3; level++) {
      for (String a : Rules.witness(g)) g = Rules.act(g, a);
      assertEquals(level == 3 ? "WON" : "PLAYING", g.status());
      assertEquals(Math.min(3, level + 1), g.level());
    }
    assertEquals(3, g.exitsFound());
  }

  @Test
  void observationHasOnlyDiscoveredMemoryAndRetainsAnObservedExit() {
    GameState g = Rules.newGame(1986, 1, 1);
    assertEquals(1, g.memory().size());
    String prompt = Observations.request(g).stateJson();
    for (String forbidden : List.of("walls", "exitRoom", "exitDirection", "seed", "witness"))
      assertFalse(prompt.contains("\"" + forbidden + "\""));
    for (String a : Rules.witness(g)) {
      if (Rules.exit(g.maze(), g.room(), g.direction())) break;
      g = Rules.act(g, a);
    }
    assertTrue(g.memory().stream().anyMatch(m -> m.edges().contains("E")));
    for (int i = 0; i < 4; i++) g = Rules.act(g, "TURN_RIGHT");
    assertTrue(g.memory().stream().anyMatch(m -> m.edges().contains("E")));
  }

  @Test
  void explorerUsesObservationAndEscapesAllEvaluationCases() {
    for (long seed : List.of(1983L, 1986L, 2026L))
      for (int level = 1; level <= 3; level++) {
        RunState r =
            Runs.create(Rules.newGame(seed, level, level), "test", 0, "EXPLORER", "", 128, 120000);
        for (int t = 0; t < 128 && r.game().status().equals("PLAYING"); t++) {
          String a = Baselines.choose(r.game(), "EXPLORER", t, r.stack());
          r = Runs.apply(new ActionTurn(r, a, 0));
        }
        assertEquals("WON", r.game().status(), "seed=" + seed + " level=" + level);
      }
  }

  @Test
  void unknownActionsCannotChangePositionAndListsAreImmutable() {
    GameState g = Rules.newGame(1, 1, 1);
    assertEquals(g.room(), Rules.act(g, "TELEPORT").room());
    assertEquals(g.remaining(), Rules.act(g, "TELEPORT").remaining());
    assertThrows(UnsupportedOperationException.class, () -> g.remaining().set(0, 0));
  }

  @Test
  void hostProtectsOwnershipPauseDuplicateAndReset() {
    Session s = new Session();
    RunState start = s.reset(1983, 1, 3);
    RunState initial = Runs.apply(new ActionTurn(start, "START", 0));
    s.publish(initial);
    s.finish(initial.segment(), "HUMAN_TURN");
    RunState ai = s.admit("EXPLORER", "", initial.revision(), 128, 120000);
    assertThrows(
        IllegalStateException.class,
        () -> s.admit("HUMAN", "FORWARD", initial.revision(), 1, 1000));
    s.pause();
    assertEquals("PAUSED", s.control(ai).stopReason());
    RunState next = Runs.apply(new ActionTurn(ai, "TURN_LEFT", 0));
    s.publish(next);
    assertEquals(next, s.publish(next));
    s.reset(1986, 1, 3);
    assertEquals("SUPERSEDED", s.control(next).stopReason());
    assertThrows(
        IllegalStateException.class,
        () -> s.publish(Runs.apply(new ActionTurn(next, "FORWARD", 0))));
  }
}
