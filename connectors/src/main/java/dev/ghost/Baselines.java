package dev.ghost;

import java.util.*;

/** Both policies use revealed memory only. No Maze access, seed-based route, or witness. */
public final class Baselines {
  private Baselines() {}

  public static String choose(GameState game, String player, int turn, List<Integer> stack) {
    Map<String, Object> o = Observations.view(game);
    int room = (int) o.get("room"), dir = (int) o.get("direction"), size = (int) o.get("size");
    String edges = (String) o.get("edges");
    if (player.equals("RANDOM")) {
      List<String> legal = new ArrayList<>(List.of("TURN_LEFT", "TURN_RIGHT"));
      if (edges.charAt(dir) == '.' || edges.charAt(dir) == 'E') legal.add("FORWARD");
      return legal.get(new Random(0x47484F5354L + turn * 104729L).nextInt(legal.size()));
    }
    Set<Integer> seen = new HashSet<>();
    game.memory().forEach(m -> seen.add(m.room()));
    int target = -1;
    for (int d = 0; d < 4; d++)
      if (edges.charAt(d) == 'E') {
        target = d;
        break;
      }
    if (target < 0)
      for (int d = 0; d < 4; d++) {
        int q = Rules.neighbor(room, d, size);
        if (edges.charAt(d) == '.' && q >= 0 && !seen.contains(q)) {
          target = d;
          break;
        }
      }
    if (target < 0)
      for (int d = 0; d < 4; d++)
        if (edges.charAt(d) == '?') {
          target = d;
          break;
        }
    if (target < 0 && stack.size() > 1) {
      int parent = stack.get(stack.size() - 2);
      for (int d = 0; d < 4; d++)
        if (Rules.neighbor(room, d, size) == parent && edges.charAt(d) == '.') target = d;
    }
    if (target < 0) return "TURN_RIGHT";
    if (target == dir) return "FORWARD";
    return (dir + 3) % 4 == target ? "TURN_LEFT" : "TURN_RIGHT";
  }

  public static List<Integer> advanceStack(RunState r, GameState next) {
    if (next.level() != r.game().level()) return List.of(next.room());
    if (next.room() == r.game().room()) return r.stack();
    List<Integer> s = new ArrayList<>(r.stack());
    int i = s.indexOf(next.room());
    if (i >= 0) return List.copyOf(s.subList(0, i + 1));
    s.add(next.room());
    return List.copyOf(s);
  }
}
