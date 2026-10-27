package dev.ghost;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@QuarkusTest
class GhostRecordingE2ETest {
  @Inject Session session;

  private void reset(long seed, int level, boolean single) {
    given()
        .contentType("application/json")
        .body(Map.of("seed", seed, "level", level, "singleLevel", single))
        .post("/api/game")
        .then()
        .statusCode(200);
  }

  private void action(String a) {
    given()
        .contentType("application/json")
        .body(Map.of("action", a, "revision", session.latest().revision()))
        .post("/api/game/actions")
        .then()
        .statusCode(200);
  }

  private void play(String player, int limit, long millis) {
    given()
        .contentType("application/json")
        .body(
            Map.of(
                "player",
                player,
                "revision",
                session.latest().revision(),
                "maxDecisions",
                limit,
                "durationMs",
                millis))
        .post("/api/game/play")
        .then()
        .statusCode(200);
  }

  private void waitForPlayer() throws Exception {
    long deadline = System.nanoTime() + 125_000_000_000L;
    while (session.active() && System.nanoTime() < deadline) Thread.sleep(30);
    assertFalse(session.active());
  }

  @Test
  void ghostRecordingE2ECompletesHumanCampaignThenBoundedPlayers() throws Exception {
    reset(1983, 1, false);
    for (int level = 1; level <= 3; level++)
      for (String a : Rules.witness(session.latest().game())) action(a);
    assertEquals("WON", session.latest().game().status());
    assertEquals(3, session.latest().game().exitsFound());
    if (Boolean.getBoolean("ghost.live")) {
      reset(1983, 1, true);
      play("TEV1", 3, 40000);
      waitForPlayer();
      assertTrue(session.latest().decisions() > 0);
      assertFalse(session.frame().get("reason").toString().startsWith("PROVIDER_"));
    }
    reset(1983, 1, true);
    play("EXPLORER", 128, 120000);
    waitForPlayer();
    assertEquals("WON", session.latest().game().status());
    assertTrue(session.latest().decisions() <= 128);
    given().get("/").then().statusCode(200);
    given().get("/api/game").then().statusCode(200);
  }

  @Test
  void validatesActionsOwnershipAndBounds() throws Exception {
    reset(1986, 1, true);
    given()
        .contentType("application/json")
        .body(Map.of())
        .post("/api/game/actions")
        .then()
        .statusCode(400);
    given()
        .contentType("application/json")
        .body(Map.of())
        .post("/api/game/play")
        .then()
        .statusCode(400);
    given()
        .contentType("application/json")
        .body(Map.of("action", "TELEPORT", "revision", session.latest().revision()))
        .post("/api/game/actions")
        .then()
        .statusCode(400);
    given()
        .contentType("application/json")
        .body(Map.of("action", "FORWARD", "revision", -1))
        .post("/api/game/actions")
        .then()
        .statusCode(409);
    play("EXPLORER", 1, 120000);
    waitForPlayer();
    assertEquals(1, session.latest().decisions());
    assertEquals("DECISION_LIMIT", session.frame().get("reason"));
    reset(1986, 1, true);
    play("EXPLORER", 128, 1);
    waitForPlayer();
    assertTrue(session.latest().decisions() <= 128);
    assertEquals("TIME_LIMIT", session.frame().get("reason"));
  }

  @Test
  void pauseStopsAfterAtMostOneInFlightDecision() throws Exception {
    reset(1986, 1, true);
    play("EXPLORER", 128, 120000);
    int before = session.latest().decisions();
    given()
        .contentType("application/json")
        .body(Map.of())
        .post("/api/game/pause")
        .then()
        .statusCode(200);
    waitForPlayer();
    assertTrue(session.latest().decisions() <= before + 1);
    assertEquals("PAUSED", session.frame().get("reason"));
  }

  @Test
  void canonicalMetadataContainsOneRecursiveRootAndNativeQueriesAndCommand() throws Exception {
    String contract =
        new String(
            getClass()
                .getClassLoader()
                .getResourceAsStream("META-INF/pipeline/pipeline-contract.json")
                .readAllBytes());
    assertTrue(contract.contains("Next turn"));
    assertTrue(contract.contains("Ask Tev1"));
    assertTrue(contract.contains("Publish frame"));
    assertTrue(contract.contains("play"));
  }

  @Test
  @EnabledIfSystemProperty(named = "ghost.live", matches = "true")
  void realTev1ProposesValidatedActionsWithoutRequiringEscape() throws Exception {
    reset(2026, 1, true);
    play("TEV1", 3, 40000);
    waitForPlayer();
    assertFalse(session.history().isEmpty());
    assertTrue(
        session.history().stream()
            .allMatch(
                a ->
                    a.player().equals("TEV1") && Rules.ACTIONS.contains(a.action())
                        || a.action().equals("START")));
    assertTrue(
        session.history().stream().anyMatch(a -> a.player().equals("TEV1") && a.latencyMs() > 0));
    assertFalse(
        session.frame().get("reason").toString().startsWith("PROVIDER_"),
        session.frame().toString());
  }
}
