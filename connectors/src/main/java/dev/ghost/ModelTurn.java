package dev.ghost;


public record ModelTurn(
    RunState run, org.pipelineframework.connector.decision.DecisionRequest request) {}
