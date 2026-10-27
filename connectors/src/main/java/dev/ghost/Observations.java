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
    var relative = new LinkedHashMap<String, String>();
    for (int i = 0; i < 3; i++) {
      char edge = current.edges().charAt((g.direction() + List.of(0, 3, 1).get(i)) % 4);
      relative.put(
          List.of("front", "left", "right").get(i),
          switch (edge) {
            case '#' -> "wall";
            case '.' -> "open passage";
            case 'E' -> "exit";
            default -> "unseen";
          });
    }
    out.put("entriesLeft", g.remaining().get(g.room()));
    out.put(
        "memory",
        g.memory().stream()
            .map(r -> List.of(r.room(), r.visits(), r.edges(), g.remaining().get(r.room())))
            .toList());
    out.put("previousAction", g.lastAction());
    // UI room labels are one-based; never mix those labels into zero-based model identities.
    out.put(
        "previousResult",
        g.message().startsWith("Room ")
            ? "Entered a room; " + g.remaining().get(g.room()) + " entries left."
            : g.message());
    out.put("relativeEdges", relative);
    return out;
  }

  public static DecisionRequest request(GameState g) {
    return request(g, null, false);
  }

  public static Map<String, Object> view(GameState g, NavigationSignal navigation, boolean clues) {
    var out = new LinkedHashMap<>(view(g));
    if (clues) {
      out.put("signal", navigation.signal());
      out.put("signalTrend", navigation.signalTrend());
      out.put(
          "signalMemory",
          navigation.readings().stream().map(r -> List.of(r.room(), r.signal())).toList());
    }
    return out;
  }

  public static DecisionRequest request(GameState g, NavigationSignal navigation, boolean clues) {
    try {
      return new DecisionRequest(
          JSON.writeValueAsString(view(g, navigation, clues)),
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
                      + " to navigate; an exit requires moving FORWARD through it."
                      + (clues
                          ? " The haunted signal ranks FAINT, NOTICEABLE, STRONG, INTENSE from far"
                                + " to close through corridors. Compare signalMemory readings in"
                                + " visited rooms to explore toward stronger signals. Turns and"
                                + " wall bumps report NO_MOVEMENT; UNCHANGED after movement means"
                                + " the same coarse band. The signal gives no exit direction or"
                                + " route."
                          : ""),
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
