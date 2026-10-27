package dev.ghost;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.pipelineframework.connector.decision.*;

/** The only projection eligible for model input and the browser view. */
public final class Observations {
  private static final ObjectMapper JSON = new ObjectMapper();
  public static final String PROMPT_VERSION = "ordered-policy-v3";
  public static final String ACTION_PROMPT =
      """
      Choose ONE action: FORWARD, TURN_LEFT, TURN_RIGHT.
      front, left, right mean relativeEdges.front, relativeEdges.left, relativeEdges.right.
      Edge values are "wall", "open passage", "exit".
      Apply rules top-to-bottom. Stop at the first match. Other state fields do not override these rules.
      1. If front == "exit", choose FORWARD.
      2. If left == "exit", choose TURN_LEFT.
      3. If right == "exit", choose TURN_RIGHT.
      4. If previousAction is TURN_LEFT or TURN_RIGHT and front == "open passage", choose FORWARD.
      5. If front == "open passage" and current_room_visits == 1, choose FORWARD.
      6. If left == "open passage", choose TURN_LEFT.
      7. If right == "open passage", choose TURN_RIGHT.
      8. If front == "open passage", choose FORWARD.
      9. Otherwise, choose TURN_RIGHT to look behind.
      """
          .strip();
  public static final String CLUE_PROMPT =
      """
      Signal order: FAINT < NOTICEABLE < STRONG < INTENSE means closer through corridors.
      signalMemory rows are [visited room, reading]. Prefer openings toward stronger remembered readings.
      If a move made the signal weaker, try another opening.
      NO_MOVEMENT means a turn or bump, not progress.
      """
          .strip();

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
    out.put("current_room_visits", current.visits());
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
                  ACTION_PROMPT + (clues ? "\n" + CLUE_PROMPT : ""),
                  List.of(
                      new DecisionCriterion(
                          "FORWARD",
                          "Move ahead. Appropriate only when front is open passage or exit."),
                      new DecisionCriterion(
                          "TURN_LEFT",
                          "Turn left in place. Appropriate for an opening on the left."),
                      new DecisionCriterion(
                          "TURN_RIGHT",
                          "Turn right in place. Appropriate for an opening on the right.")))));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
