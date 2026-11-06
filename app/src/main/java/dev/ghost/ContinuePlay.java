package dev.ghost;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.pipelineframework.service.ReactiveService;

@ApplicationScoped
public class ContinuePlay implements ReactiveService<PublishedFrame, Continuation> {
  public Uni<Continuation> process(PublishedFrame value) {
    return Uni.createFrom().item(() -> transform(value.run()));
  }

  private Continuation transform(RunState r) {
    String end = Runs.endReason(r);
    if (r.player().equals("HUMAN"))
      end = r.game().status().equals("PLAYING") ? "HUMAN_TURN" : r.game().status();
    return end.isEmpty() ? new Continuation.Again(r) : new Continuation.Done(new RunResult(r, end));
  }
}
