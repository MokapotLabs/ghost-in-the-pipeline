package dev.ghost;

import org.pipelineframework.connector.decision.DecisionRequest;

/** A distinct nominal branch selects the existing native 4B connector binding. */
public record LargeModelTurn(RunState run, DecisionRequest request) {}
