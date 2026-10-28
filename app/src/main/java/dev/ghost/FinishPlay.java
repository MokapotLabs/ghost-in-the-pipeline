package dev.ghost;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.pipelineframework.service.ReactiveService;

@ApplicationScoped
public class FinishPlay implements ReactiveService<RunResult, RunResult> {
  public Uni<RunResult> process(RunResult r) {
    return Uni.createFrom().item(() -> transform(r));
  }

  private RunResult transform(RunResult r) {
    return r;
  }
}
