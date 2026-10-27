package dev.ghost;

import java.util.*;

/** Original implementation. No upstream source or drawing data is incorporated. */
public final class Rules {
  public static final List<String> ACTIONS = List.of("FORWARD", "TURN_LEFT", "TURN_RIGHT");
  public static final int[] SIZES = {3, 4, 6};

  private Rules() {}

  public static int neighbor(int room, int direction, int size) {
    int x = room % size, y = room / size;
    return switch (direction) {
      case 0 -> y > 0 ? room - size : -1;
      case 1 -> x < size - 1 ? room + 1 : -1;
      case 2 -> y < size - 1 ? room + size : -1;
      case 3 -> x > 0 ? room - 1 : -1;
      default -> throw new IllegalArgumentException("direction");
    };
  }

  public static boolean wall(Maze m, int r, int d) {
    return (m.walls().get(r) & (1 << d)) != 0;
  }

  public static boolean exit(Maze m, int r, int d) {
    return m.exitRoom() == r && m.exitDirection() == d;
  }

  public static GameState newGame(long seed, int level, int lastLevel) {
    if (level < 1 || level > 3 || lastLevel < level || lastLevel > 3)
      throw new IllegalArgumentException("level range 1..3");
    int n = SIZES[level - 1];
    Random rng = new Random(seed ^ (0x9E3779B97F4A7C15L * level));
    List<Integer> walls = new ArrayList<>(Collections.nCopies(n * n, 15));
    boolean[] seen = new boolean[n * n];
    Deque<Integer> stack = new ArrayDeque<>();
    stack.push(0);
    seen[0] = true;
    while (!stack.isEmpty()) {
      int r = stack.peek();
      List<Integer> ds = new ArrayList<>();
      for (int d = 0; d < 4; d++) {
        int q = neighbor(r, d, n);
        if (q >= 0 && !seen[q]) ds.add(d);
      }
      if (ds.isEmpty()) {
        stack.pop();
        continue;
      }
      int d = ds.get(rng.nextInt(ds.size())), q = neighbor(r, d, n);
      walls.set(r, walls.get(r) & ~(1 << d));
      walls.set(q, walls.get(q) & ~(1 << ((d + 2) % 4)));
      seen[q] = true;
      stack.push(q);
    }
    int side = rng.nextInt(4), offset = rng.nextInt(n);
    int out =
        switch (side) {
          case 0 -> offset;
          case 1 -> offset * n + n - 1;
          case 2 -> (n - 1) * n + offset;
          default -> offset * n;
        };
    Maze maze = new Maze(n, walls, out, side);
    // A fixed independent distance rule gives a nontrivial start, never conditioned on a player.
    int[] dist = distances(maze, out);
    int start = 0;
    for (int i = 1; i < dist.length; i++) if (dist[i] > dist[start]) start = i;
    GameState game =
        observe(
            new GameState(
                seed,
                level,
                lastLevel,
                maze,
                start,
                rng.nextInt(4),
                Collections.nCopies(n * n, 7),
                List.of(new RoomMemory(start, 1, "????")),
                "PLAYING",
                0,
                0,
                0,
                0,
                1,
                0,
                "START",
                "Find the exit. Seven entries per room."));
    validateSolvable(game);
    return game;
  }

  public static GameState observe(GameState g) {
    List<RoomMemory> memory = new ArrayList<>(g.memory());
    for (int i = 0; i < memory.size(); i++)
      if (memory.get(i).room() == g.room()) {
        RoomMemory old = memory.get(i);
        char[] edges = old.edges().toCharArray();
        for (int d : new int[] {g.direction(), (g.direction() + 1) % 4, (g.direction() + 3) % 4})
          edges[d] = exit(g.maze(), g.room(), d) ? 'E' : wall(g.maze(), g.room(), d) ? '#' : '.';
        memory.set(i, new RoomMemory(old.room(), old.visits(), new String(edges)));
      }
    memory.sort(Comparator.comparingInt(RoomMemory::room));
    return copy(
        g,
        g.room(),
        g.direction(),
        g.remaining(),
        memory,
        g.status(),
        g.moves(),
        g.actions(),
        g.collisions(),
        g.repeats(),
        g.explored(),
        g.exitsFound(),
        g.lastAction(),
        g.message());
  }

  public static GameState act(GameState g, String action) {
    if (!g.status().equals("PLAYING")) return g;
    if (action.equals("START")) return g;
    if (!ACTIONS.contains(action)) return message(g, action, "Rejected unknown action.");
    int dir = g.direction();
    if (!action.equals("FORWARD"))
      return observe(
          copy(
              g,
              g.room(),
              (dir + (action.equals("TURN_LEFT") ? 3 : 1)) % 4,
              g.remaining(),
              g.memory(),
              g.status(),
              g.moves(),
              g.actions() + 1,
              g.collisions(),
              g.repeats(),
              g.explored(),
              g.exitsFound(),
              action,
              "Turned " + (action.equals("TURN_LEFT") ? "left." : "right.")));
    if (exit(g.maze(), g.room(), dir)) {
      if (g.level() == g.lastLevel())
        return copy(
            g,
            g.room(),
            dir,
            g.remaining(),
            g.memory(),
            "WON",
            g.moves() + 1,
            g.actions() + 1,
            g.collisions(),
            g.repeats(),
            g.explored(),
            g.exitsFound() + 1,
            action,
            "ESCAPED — the ghost is free.");
      GameState next = newGame(g.seed(), g.level() + 1, g.lastLevel());
      return copy(
          next,
          next.room(),
          next.direction(),
          next.remaining(),
          next.memory(),
          "PLAYING",
          g.moves() + 1,
          g.actions() + 1,
          g.collisions(),
          g.repeats(),
          g.explored() + 1,
          g.exitsFound() + 1,
          action,
          "EXIT FOUND — welcome to level " + next.level() + ".");
    }
    if (wall(g.maze(), g.room(), dir))
      return copy(
          g,
          g.room(),
          dir,
          g.remaining(),
          g.memory(),
          g.status(),
          g.moves(),
          g.actions() + 1,
          g.collisions() + 1,
          g.repeats(),
          g.explored(),
          g.exitsFound(),
          action,
          "BUMP — a wall. Try turning.");
    int room = neighbor(g.room(), dir, g.maze().size());
    if (room < 0) throw new IllegalStateException("open boundary without exit");
    List<Integer> remaining = new ArrayList<>(g.remaining());
    remaining.set(room, remaining.get(room) - 1);
    List<RoomMemory> memory = new ArrayList<>(g.memory());
    int index = -1;
    for (int i = 0; i < memory.size(); i++) if (memory.get(i).room() == room) index = i;
    boolean repeated = index >= 0;
    RoomMemory old = repeated ? memory.get(index) : new RoomMemory(room, 0, "????");
    char[] edges = old.edges().toCharArray();
    edges[(dir + 2) % 4] = '.';
    RoomMemory entered = new RoomMemory(room, old.visits() + 1, new String(edges));
    if (repeated) memory.set(index, entered);
    else memory.add(entered);
    boolean lost = remaining.get(room) == 0;
    return observe(
        copy(
            g,
            room,
            dir,
            remaining,
            memory,
            lost ? "LOST" : "PLAYING",
            g.moves() + 1,
            g.actions() + 1,
            g.collisions(),
            g.repeats() + (repeated ? 1 : 0),
            g.explored() + (repeated ? 0 : 1),
            g.exitsFound(),
            action,
            lost
                ? "GAME OVER — this room has no entries left."
                : "Room " + (room + 1) + " · " + remaining.get(room) + " entries left."));
  }

  public static GameState message(GameState g, String a, String m) {
    return copy(
        g,
        g.room(),
        g.direction(),
        g.remaining(),
        g.memory(),
        g.status(),
        g.moves(),
        g.actions(),
        g.collisions(),
        g.repeats(),
        g.explored(),
        g.exitsFound(),
        a,
        m);
  }

  private static GameState copy(
      GameState g,
      int room,
      int dir,
      List<Integer> remaining,
      List<RoomMemory> memory,
      String status,
      int moves,
      int actions,
      int collisions,
      int repeats,
      int explored,
      int exits,
      String action,
      String message) {
    return new GameState(
        g.seed(),
        g.level(),
        g.lastLevel(),
        g.maze(),
        room,
        dir,
        remaining,
        memory,
        status,
        moves,
        actions,
        collisions,
        repeats,
        explored,
        exits,
        action,
        message);
  }

  private static int[] distances(Maze m, int start) {
    int[] ds = new int[m.size() * m.size()];
    Arrays.fill(ds, -1);
    ds[start] = 0;
    Deque<Integer> q = new ArrayDeque<>();
    q.add(start);
    while (!q.isEmpty()) {
      int r = q.remove();
      for (int d = 0; d < 4; d++) {
        int n = neighbor(r, d, m.size());
        if (n >= 0 && !wall(m, r, d) && ds[n] < 0) {
          ds[n] = ds[r] + 1;
          q.add(n);
        }
      }
    }
    return ds;
  }

  /** Only admission/tests may call this witness. Players receive observations, never this value. */
  public static List<String> witness(GameState g) {
    int[] ds = distances(g.maze(), g.maze().exitRoom());
    if (ds[g.room()] < 0) throw new IllegalStateException("unreachable exit");
    List<String> actions = new ArrayList<>();
    int room = g.room(), dir = g.direction();
    while (room != g.maze().exitRoom()) {
      int target = -1;
      for (int d = 0; d < 4; d++) {
        int q = neighbor(room, d, g.maze().size());
        if (q >= 0 && !wall(g.maze(), room, d) && ds[q] == ds[room] - 1) {
          target = d;
          break;
        }
      }
      if (target < 0) throw new IllegalStateException("no path");
      while (dir != target) {
        actions.add("TURN_RIGHT");
        dir = (dir + 1) % 4;
      }
      actions.add("FORWARD");
      room = neighbor(room, dir, g.maze().size());
    }
    while (dir != g.maze().exitDirection()) {
      actions.add("TURN_RIGHT");
      dir = (dir + 1) % 4;
    }
    actions.add("FORWARD");
    return List.copyOf(actions);
  }

  public static void validateSolvable(GameState g) {
    GameState single =
        new GameState(
            g.seed(),
            g.level(),
            g.level(),
            g.maze(),
            g.room(),
            g.direction(),
            g.remaining(),
            g.memory(),
            g.status(),
            g.moves(),
            g.actions(),
            g.collisions(),
            g.repeats(),
            g.explored(),
            g.exitsFound(),
            g.lastAction(),
            g.message());
    for (String a : witness(single)) single = act(single, a);
    if (!single.status().equals("WON") || single.remaining().stream().anyMatch(x -> x <= 0))
      throw new IllegalStateException("level not solvable within entry allowance");
  }
}
