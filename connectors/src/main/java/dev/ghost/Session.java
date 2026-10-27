package dev.ghost;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

/** One local host session; authoritative writes enter through the publish Command. */
@ApplicationScoped
public class Session {
  private RunState latest;
  private String generation = "", owner = "", reason = "READY";
  private boolean paused;
  private final List<ActionLog> history = new ArrayList<>();

  public synchronized RunState reset(long seed, int level, int last) {
    GameState initial = Rules.newGame(seed, level, last);
    generation = UUID.randomUUID().toString();
    latest = Runs.create(initial, generation, 0, "HUMAN", "START", 1, 120000);
    owner = latest.segment();
    paused = false;
    reason = "READY";
    history.clear();
    return latest;
  }

  public synchronized RunState admit(
      String player, String action, long revision, int limit, long millis) {
    if (latest == null) throw new IllegalArgumentException("Start a game first");
    if (!owner.isEmpty()) throw new IllegalStateException("A player already owns this turn");
    if (revision != latest.revision())
      throw new IllegalStateException("Stale revision; refresh the game");
    if (!latest.game().status().equals("PLAYING"))
      throw new IllegalStateException("Start a new game");
    latest = Runs.create(latest.game(), generation, revision, player, action, limit, millis);
    owner = latest.segment();
    paused = false;
    reason = "PLAYING";
    return latest;
  }

  public synchronized RunState control(RunState r) {
    String stop =
        !generation.equals(r.generation()) || !owner.equals(r.segment())
            ? "SUPERSEDED"
            : paused ? "PAUSED" : "";
    return Runs.control(r, System.nanoTime(), stop);
  }

  public synchronized RunState publish(RunState r) {
    if (!generation.equals(r.generation()) || !owner.equals(r.segment()))
      throw new IllegalStateException("stale-segment");
    if (r.revision() == latest.revision() && r.equals(latest)) return r;
    if (r.revision() != latest.revision() + 1) throw new IllegalStateException("stale-revision");
    latest = r;
    if (!r.log().isEmpty()) history.add(r.log().getLast());
    return r;
  }

  public synchronized void finish(String segment, String result) {
    if (owner.equals(segment)) {
      owner = "";
      reason = result;
      paused = false;
    }
  }

  public synchronized void pause() {
    if (!owner.isEmpty()) {
      paused = true;
      reason = "PAUSING";
    }
  }

  public synchronized boolean active() {
    return !owner.isEmpty();
  }

  public synchronized RunState latest() {
    return latest;
  }

  public synchronized List<ActionLog> history() {
    return List.copyOf(history);
  }

  public synchronized Map<String, Object> frame() {
    if (latest == null) return Map.of("ready", false, "active", false);
    GameState g = latest.game();
    var f = new LinkedHashMap<String, Object>();
    f.put("ready", true);
    f.put("generation", generation);
    f.put("segment", latest.segment());
    f.put("seed", g.seed());
    f.put("revision", latest.revision());
    f.put("observation", Observations.view(g));
    f.put("level", g.level());
    f.put("status", g.status());
    f.put("message", g.message());
    f.put("active", !owner.isEmpty());
    f.put("pausing", paused);
    f.put("reason", reason);
    f.put("player", latest.player());
    f.put("moves", g.moves());
    f.put("actions", g.actions());
    f.put("collisions", g.collisions());
    f.put("repeats", g.repeats());
    f.put("explored", g.explored());
    f.put("exitsFound", g.exitsFound());
    f.put("decisions", latest.decisions());
    f.put("latestAction", g.lastAction());
    f.put("latencyMs", latest.log().isEmpty() ? 0 : latest.log().getLast().latencyMs());
    return f;
  }
}
