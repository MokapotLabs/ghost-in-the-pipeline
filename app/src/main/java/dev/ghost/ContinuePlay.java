package dev.ghost;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.pipelineframework.service.ReactiveService;

@ApplicationScoped
public class ContinuePlay implements ReactiveService<RunState, Continuation> {
  public Uni<Continuation> process(RunState r) {
    return Uni.createFrom().item(() -> transform(r));
  }

  private Continuation transform(RunState r) {
    String end = Runs.endReason(r);
    if (r.player().equals("HUMAN"))
      end = r.game().status().equals("PLAYING") ? "HUMAN_TURN" : r.game().status();
    return end.isEmpty() ? new Continuation.Again(r) : new Continuation.Done(new RunResult(r, end));
  }
}
