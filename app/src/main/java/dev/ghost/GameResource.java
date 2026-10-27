package dev.ghost;

import io.smallrye.mutiny.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.*;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.pipelineframework.PipelineExecutionService;
import org.pipelineframework.execution.*;

@Path("/api/game")
@Produces(MediaType.APPLICATION_JSON)
public class GameResource {
  @Inject Session session;
  @Inject PipelineExecutionService pipelines;
  @Inject ModelSettings settings;

  public record NewGame(long seed, int level, boolean singleLevel) {}

  public record Action(String action, long revision) {}

  public record Play(
      String player, long revision, Integer maxDecisions, Long durationMs, Boolean agentClues) {}

  @GET
  public Map<String, Object> view() {
    var f = new LinkedHashMap<>(session.frame());
    f.put("model", settings.model(f.getOrDefault("player", "TEV1").toString()));
    f.put("models", settings.models());
    f.put("decisionPrompt", Observations.PROMPT_VERSION);
    return f;
  }

  @POST
  public Uni<Map<String, Object>> reset(NewGame request) {
    if (request == null) throw new BadRequestException("Game request is required");
    int level = request.level() == 0 ? 1 : request.level();
    RunState r;
    try {
      r = session.reset(request.seed(), level, request.singleLevel() ? level : 3);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    }
    return execute(r).replaceWith(this::view);
  }

  @POST
  @Path("/actions")
  public Uni<Map<String, Object>> action(Action action) {
    if (action == null || action.action() == null || !Rules.ACTIONS.contains(action.action()))
      throw new BadRequestException("Unknown action");
    RunState r = admit("HUMAN", action.action(), action.revision(), 1, 120000);
    return execute(r).replaceWith(this::view);
  }

  @POST
  @Path("/play")
  public Map<String, Object> play(Play p) {
    if (p == null
        || p.player() == null
        || !List.of("TEV1", "TEV1_4B", "RANDOM", "EXPLORER").contains(p.player()))
      throw new BadRequestException("Unknown player");
    int limit = p.maxDecisions() == null ? 128 : p.maxDecisions();
    long millis = p.durationMs() == null ? 120000 : p.durationMs();
    if (limit < 1 || limit > 128 || millis < 1 || millis > 120000)
      throw new BadRequestException("Budget exceeds demo bounds");
    RunState r;
    try {
      r =
          session.admit(
              p.player(), "", p.revision(), limit, millis, Boolean.TRUE.equals(p.agentClues()));
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new ClientErrorException(e.getMessage(), 409);
    }
    execute(r).subscribe().with(done -> {}, error -> {});
    return view();
  }

  @POST
  @Path("/pause")
  public Map<String, Object> pause() {
    session.pause();
    return view();
  }

  @GET
  @Path("/history")
  public List<ActionLog> history() {
    return session.history();
  }

  @GET
  @Path("/events")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @RestStreamElementType(MediaType.APPLICATION_JSON)
  public Multi<Map<String, Object>> events() {
    return Multi.createFrom()
        .ticks()
        .every(Duration.ofMillis(150))
        .onOverflow()
        .drop()
        .map(t -> view());
  }

  private RunState admit(String player, String action, long revision, int limit, long duration) {
    try {
      return session.admit(player, action, revision, limit, duration);
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new ClientErrorException(e.getMessage(), 409);
    }
  }

  /** This is the sole external root-admission boundary. Internal turns never re-enter it. */
  private Uni<RunResult> execute(RunState r) {
    String execution = UUID.randomUUID().toString();
    return Uni.createFrom()
        .<RunResult>emitter(
            emitter -> {
              PipelineExecutionContextHolder.set(
                  new PipelineExecutionContext("local-demo", execution, 0));
              try {
                var subscription =
                    pipelines
                        .<RunResult>executePipelineUnary(Uni.createFrom().item(r))
                        .ifNoItem()
                        .after(
                            Duration.ofMillis(
                                Math.max(1, (r.deadline() - System.nanoTime()) / 1_000_000)))
                        .fail()
                        .invoke(result -> session.finish(r.segment(), result.reason()))
                        .onFailure()
                        .invoke(
                            failure ->
                                session.finish(
                                    r.segment(),
                                    failure instanceof io.smallrye.mutiny.TimeoutException
                                            || failure
                                                instanceof java.util.concurrent.TimeoutException
                                        ? "TIME_LIMIT"
                                        : "PROVIDER_OR_PIPELINE_FAILURE: "
                                            + failure.getClass().getSimpleName()))
                        .subscribe()
                        .with(emitter::complete, emitter::fail);
                emitter.onTermination(subscription::cancel);
              } finally {
                PipelineExecutionContextHolder.clear();
              }
            });
  }
}
