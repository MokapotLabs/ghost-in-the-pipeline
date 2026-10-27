package dev.ghost;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.pipelineframework.service.ReactiveService;

@ApplicationScoped
public class ChooseTurn implements ReactiveService<RunState, NextTurn> {
  public Uni<NextTurn> process(RunState r) {
    return Uni.createFrom().item(() -> transform(r));
  }

  private NextTurn transform(RunState r) {
    String end = Runs.endReason(r);
    if (!end.isEmpty()) return new NextTurn.Done(new RunResult(r, end));
    if (r.player().equals("TEV1"))
      return new NextTurn.Model(new ModelTurn(r, Observations.request(r.game())));
    String action =
        r.player().equals("HUMAN")
            ? r.action()
            : Baselines.choose(r.game(), r.player(), r.decisions(), r.stack());
    return new NextTurn.Action(new ActionTurn(r, action, 0));
  }
}
