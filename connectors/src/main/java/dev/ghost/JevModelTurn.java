package dev.ghost;

import org.pipelineframework.connector.decision.DecisionRequest;

/** A nominal branch selects the existing hosted Jev decision binding. */
public record JevModelTurn(RunState run, DecisionRequest request) {}
