package dev.ghost;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.pipelineframework.service.ReactiveService;

@ApplicationScoped
public class ApplyAction implements ReactiveService<ActionTurn, RunState> {
  public Uni<RunState> process(ActionTurn r) {
    return Uni.createFrom().item(() -> transform(r));
  }

  private RunState transform(ActionTurn r) {
    return Runs.apply(r);
  }
}
