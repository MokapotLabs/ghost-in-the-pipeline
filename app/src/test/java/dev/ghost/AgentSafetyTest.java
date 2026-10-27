package dev.ghost;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.pipelineframework.connector.decision.*;

class AgentSafetyTest {
  @Test
  void relativeDirectionsUseOnlyObservedEdgesAndTurnWithThePlayer() {
    GameState g = Rules.newGame(1983, 1, 1);
    var v = Observations.view(g);
    assertEquals(
        Map.of("front", "wall", "left", "wall", "right", "open passage"), v.get("relativeEdges"));
    assertEquals(
        List.of("front", "left", "right"),
        new ArrayList<>(((Map<?, ?>) v.get("relativeEdges")).keySet()));
    g = Rules.act(g, "TURN_RIGHT");
    assertEquals(
        "open passage", ((Map<?, ?>) Observations.view(g).get("relativeEdges")).get("front"));
    assertEquals(1, g.memory().size());
    assertEquals(7, g.remaining().get(g.room()));
  }

  @Test
  void bothModelBranchesCarryExactlyTheSameObservationAndHonorBounds() {
    GameState g = Rules.newGame(1983, 1, 1);
    RunState small = Runs.create(g, "test", 0, "TEV1", "", 128, 120000);
    RunState large = Runs.create(g, "test", 0, "TEV1_4B", "", 128, 120000);
    var a = (NextTurn.Model) new ChooseTurn().process(small).await().indefinitely();
    var b = (NextTurn.Large) new ChooseTurn().process(large).await().indefinitely();
    assertEquals(a.value().request(), b.value().request());
    var stopped = Runs.create(g, "test", 0, "TEV1_4B", "", 0, 120000);
    assertInstanceOf(NextTurn.Done.class, new ChooseTurn().process(stopped).await().indefinitely());
  }

  @Test
  void rejectsModelActionsOutsideTheFiniteVocabulary() {
    RunState r = Runs.create(Rules.newGame(1983, 1, 1), "test", 0, "TEV1", "", 128, 120000);
    DecisionAnswer a =
        new DecisionAnswer(
            "action",
            DecisionQuestionType.CHOICE,
            "TELEPORT",
            BigDecimal.ZERO,
            BigDecimal.ONE,
            List.of());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DecodeChoice()
                .process(new Chosen(r, new DecisionResult(List.of(a))))
                .await()
                .indefinitely());
    assertEquals(0, r.game().moves());
  }

  @Test
  void budgetAndPauseAreTypedBaseCasesBeforeAnyInference() {
    RunState r = Runs.create(Rules.newGame(1, 1, 1), "test", 0, "TEV1", "", 0, 1000);
    assertEquals("DECISION_LIMIT", Runs.endReason(r));
    assertInstanceOf(NextTurn.Done.class, new ChooseTurn().process(r).await().indefinitely());
    RunState paused = Runs.control(r, r.observedAt(), "PAUSED");
    assertEquals("PAUSED", Runs.endReason(paused));
    RunState fresh = Runs.create(r.game(), "test", 0, "TEV1", "", 128, 1000);
    assertEquals("TIME_LIMIT", Runs.endReason(Runs.control(fresh, fresh.deadline(), "")));
  }

  @Test
  void observedExitAndVisitsArePresentButUnseenTopologyNeverEntersModelState() {
    GameState g = Rules.newGame(2026, 3, 3);
    String json = Observations.request(g).stateJson();
    assertTrue(json.contains("memory"));
    assertTrue(json.contains("entriesLeft"));
    assertFalse(json.contains("walls"));
    assertFalse(json.contains("seed"));
    assertFalse(json.contains("exitRoom"));
    assertFalse(json.contains("route"));
    assertEquals(1, g.memory().size());
  }
}
