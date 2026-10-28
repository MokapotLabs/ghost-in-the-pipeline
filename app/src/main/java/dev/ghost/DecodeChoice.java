package dev.ghost;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.pipelineframework.service.ReactiveService;

@ApplicationScoped
public class DecodeChoice implements ReactiveService<Chosen, ActionTurn> {
  public Uni<ActionTurn> process(Chosen r) {
    return Uni.createFrom().item(() -> transform(r));
  }

  private ActionTurn transform(Chosen r) {
    var answers = r.result().answers();
    if (answers.size() != 1
        || !answers.getFirst().name().equals("action")
        || !Rules.ACTIONS.contains(answers.getFirst().selected()))
      throw new IllegalArgumentException("invalid-model-action");
    return new ActionTurn(
        r.run(),
        answers.getFirst().selected(),
        Math.max(0, (System.nanoTime() - r.run().decisionStarted()) / 1_000_000));
  }
}
