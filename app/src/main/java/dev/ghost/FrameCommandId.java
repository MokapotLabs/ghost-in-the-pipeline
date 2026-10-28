package dev.ghost;

import jakarta.enterprise.context.ApplicationScoped;
import org.pipelineframework.command.*;

@ApplicationScoped
public class FrameCommandId implements CommandIdGenerator<RunState> {
  public String commandId(CommandDescriptor d, RunState r) {
    return "frame:" + r.generation() + ":" + r.segment() + ":" + r.revision();
  }
}
