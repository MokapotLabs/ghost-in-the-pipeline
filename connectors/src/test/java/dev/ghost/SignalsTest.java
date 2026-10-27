package dev.ghost;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class SignalsTest {
  private static List<Integer> snake(int n) {
    List<Integer> path = new ArrayList<>();
    for (int y = 0; y < n; y++)
      for (int x = 0; x < n; x++) path.add(y * n + (y % 2 == 0 ? x : n - 1 - x));
    return path;
  }

  static GameState fixture(int level, int pathIndex, int direction) {
    int n = Rules.SIZES[level - 1];
    List<Integer> path = snake(n);
    List<Integer> walls = new ArrayList<>(Collections.nCopies(n * n, 15));
    for (int i = 1; i < path.size(); i++) {
      int a = path.get(i - 1), b = path.get(i);
      for (int d = 0; d < 4; d++)
        if (Rules.neighbor(a, d, n) == b) {
          walls.set(a, walls.get(a) & ~(1 << d));
          walls.set(b, walls.get(b) & ~(1 << ((d + 2) % 4)));
        }
    }
    int room = path.get(pathIndex), exit = path.getLast();
    return Rules.observe(
        new GameState(
            1,
            level,
            level,
            new Maze(n, walls, exit, exit % n == 0 ? 3 : 1),
            room,
            direction,
            Collections.nCopies(n * n, 7),
            List.of(new RoomMemory(room, 1, "????")),
            "PLAYING",
            0,
            0,
            0,
            0,
            1,
            0,
            "START",
            "test"));
  }

  @Test
  void corridorDistancesAndThresholdsScaleAcrossAllLevels() {
    for (int level = 1; level <= 3; level++) {
      int n = Rules.SIZES[level - 1], end = n * n - 1;
      assertEquals("INTENSE", Signals.strength(fixture(level, end, 0)));
      assertEquals("INTENSE", Signals.strength(fixture(level, end - n / 2, 0)));
      assertEquals("STRONG", Signals.strength(fixture(level, end - n, 0)));
      assertEquals("NOTICEABLE", Signals.strength(fixture(level, end - 2 * n, 0)));
      assertEquals("FAINT", Signals.strength(fixture(level, end - 2 * n - 1, 0)));
    }
    // Room 5 is one grid step from the exit at 8, but a wall forces five corridor steps.
    assertEquals("NOTICEABLE", Signals.strength(fixture(1, 3, 0)));
  }

  @Test
  void readingsAndTrendsFollowDistinctPositionsNotTurnsOrCollisions() {
    var far = fixture(1, 0, 1);
    assertEquals(
        "UNCHANGED", Signals.update(Signals.initial(far), Rules.act(far, "FORWARD")).signalTrend());
    GameState g = fixture(1, 1, 1);
    NavigationSignal nav = Signals.initial(g);
    GameState next = Rules.act(g, "FORWARD");
    nav = Signals.update(nav, next);
    assertEquals("GETTING_STRONGER", nav.signalTrend());
    assertEquals(List.of(1, 2), nav.readings().stream().map(SignalReading::room).toList());
    g = next;
    next = Rules.act(g, "TURN_LEFT");
    nav = Signals.update(nav, next);
    assertEquals("NO_MOVEMENT", nav.signalTrend());
    g = next;
    next = Rules.act(g, "FORWARD");
    nav = Signals.update(nav, next);
    assertEquals(g.room(), next.room());
    assertEquals("NO_MOVEMENT", nav.signalTrend());
    assertEquals("NOTICEABLE", nav.signal());
    next = Rules.act(next, "TURN_LEFT");
    nav = Signals.update(nav, next);
    next = Rules.act(next, "FORWARD");
    nav = Signals.update(nav, next);
    assertEquals("GETTING_WEAKER", nav.signalTrend());
    assertEquals(2, nav.readings().size());
    var immutable = nav;
    assertThrows(UnsupportedOperationException.class, () -> immutable.readings().clear());
    var nextLevel = Signals.update(nav, fixture(2, 0, 0));
    assertEquals(2, nextLevel.level());
    assertEquals(1, nextLevel.readings().size());
    assertEquals("NO_MOVEMENT", nextLevel.signalTrend());
  }

  @Test
  void clueModeChangesOnlyObservationNotGameRulesAndCannotLeakRoutes() {
    GameState g = Rules.newGame(1983, 1, 1);
    NavigationSignal nav = Signals.initial(g);
    String off = Observations.request(g, nav, false).stateJson();
    assertEquals(Observations.request(g).stateJson(), off);
    assertFalse(off.contains("signal"));
    String on = Observations.request(g, nav, true).stateJson();
    assertTrue(on.contains("signalMemory"));
    for (String forbidden :
        List.of("exitRoom", "exitDirection", "distance", "route", "seed", "walls"))
      assertFalse(on.contains(forbidden));
    var human = Observations.view(g, nav, true);
    assertEquals(nav.signal(), human.get("signal"));
    assertEquals(nav.signalTrend(), human.get("signalTrend"));
    RunState a = Runs.create(g, "test", 0, "HUMAN", "", 128, 120000, nav, false);
    RunState b = Runs.create(g, "test", 0, "TEV1", "", 128, 120000, nav, true);
    for (String action : List.of("TURN_RIGHT", "FORWARD", "TURN_LEFT", "FORWARD", "TURN_RIGHT")) {
      a = Runs.apply(new ActionTurn(a, action, 0));
      b = Runs.apply(new ActionTurn(b, action, 0));
      assertEquals(a.game(), b.game());
      assertEquals(a.navigation(), b.navigation());
    }
  }
}
