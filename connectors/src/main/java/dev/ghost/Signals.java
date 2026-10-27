package dev.ghost;

import java.util.*;

/** A coarse haunted sense. Exact graph distances stay inside this calculator. */
public final class Signals {
  private static final List<String> BANDS = List.of("FAINT", "NOTICEABLE", "STRONG", "INTENSE");

  private Signals() {}

  public static int rank(String signal) {
    int rank = BANDS.indexOf(signal);
    if (rank < 0) throw new IllegalArgumentException("Unknown signal strength");
    return rank;
  }

  public static String strength(GameState g) {
    Maze m = g.maze();
    int[] distance = new int[m.size() * m.size()];
    Arrays.fill(distance, -1);
    ArrayDeque<Integer> queue = new ArrayDeque<>();
    queue.add(m.exitRoom());
    distance[m.exitRoom()] = 0;
    while (!queue.isEmpty()) {
      int r = queue.remove();
      for (int d = 0; d < 4; d++) {
        int q = Rules.neighbor(r, d, m.size());
        if (q >= 0 && !Rules.wall(m, r, d) && distance[q] < 0) {
          distance[q] = distance[r] + 1;
          queue.add(q);
        }
      }
    }
    int d = distance[g.room()], n = m.size();
    if (d < 0) throw new IllegalArgumentException("Disconnected maze");
    return d <= Math.max(1, n / 2)
        ? "INTENSE"
        : d <= n ? "STRONG" : d <= 2 * n ? "NOTICEABLE" : "FAINT";
  }

  public static NavigationSignal initial(GameState g) {
    String signal = strength(g);
    return new NavigationSignal(
        signal, "NO_MOVEMENT", g.level(), g.room(), List.of(new SignalReading(g.room(), signal)));
  }

  public static NavigationSignal update(NavigationSignal before, GameState next) {
    if (before.level() != next.level()) return initial(next);
    if (before.room() == next.room())
      return new NavigationSignal(
          before.signal(), "NO_MOVEMENT", before.level(), before.room(), before.readings());
    String signal = strength(next);
    int difference = rank(signal) - rank(before.signal());
    List<SignalReading> readings = new ArrayList<>(before.readings());
    if (readings.stream().noneMatch(r -> r.room() == next.room()))
      readings.add(new SignalReading(next.room(), signal));
    readings.sort(Comparator.comparingInt(SignalReading::room));
    return new NavigationSignal(
        signal,
        difference > 0 ? "GETTING_STRONGER" : difference < 0 ? "GETTING_WEAKER" : "UNCHANGED",
        next.level(),
        next.room(),
        readings);
  }
}
