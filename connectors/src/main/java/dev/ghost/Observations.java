package dev.ghost;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.pipelineframework.connector.decision.*;

/** The only projection eligible for model input and the browser view. */
public final class Observations {
  private static final ObjectMapper JSON = new ObjectMapper();

  private Observations() {}

  public static Map<String, Object> view(GameState g) {
    RoomMemory current =
        g.memory().stream().filter(r -> r.room() == g.room()).findFirst().orElseThrow();
    var out = new LinkedHashMap<String, Object>();
    out.put("level", g.level());
    out.put("size", g.maze().size());
    out.put("room", g.room());
    out.put("facing", List.of("N", "E", "S", "W").get(g.direction()));
    out.put("direction", g.direction());
    out.put("edges", current.edges());
    out.put("entriesLeft", g.remaining().get(g.room()));
    out.put(
        "memory",
        g.memory().stream()
            .map(r -> List.of(r.room(), r.visits(), r.edges(), g.remaining().get(r.room())))
            .toList());
    out.put("previousAction", g.lastAction());
    out.put("previousResult", g.message());
    return out;
  }

  public static DecisionRequest request(GameState g) {
    try {
      return new DecisionRequest(
          JSON.writeValueAsString(view(g)),
          List.of(
              new DecisionQuestion(
                  "action",
                  DecisionQuestionType.CHOICE,
                  "Explore the maze and escape through an observed exit. Choose one action. Edges"
                      + " and memory use N,E,S,W: # wall, . passage, E exit, ? unseen. Memory rows"
                      + " are [room, visits, edges, entriesLeft]. Avoid walls and repeated rooms."
                      + " Room numbers are row-major; north decreases the row. You cannot move"
                      + " backwards. A turn changes only orientation; FORWARD goes in the facing"
                      + " direction. Entering the same room seven times loses. Use explored memory"
                      + " to navigate; an exit requires moving FORWARD through it.",
                  List.of(
                      new DecisionCriterion(
                          "FORWARD",
                          "Move one room in the current facing direction, or cross an exit ahead."),
                      new DecisionCriterion(
                          "TURN_LEFT", "Rotate 90 degrees left; stay in the same room."),
                      new DecisionCriterion(
                          "TURN_RIGHT", "Rotate 90 degrees right; stay in the same room.")))));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
