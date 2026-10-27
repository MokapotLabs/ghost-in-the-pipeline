package dev.ghost;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.pipelineframework.connector.decision.*;

class AgentSafetyTest {
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
