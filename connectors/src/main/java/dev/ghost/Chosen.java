package dev.ghost;


public record Chosen(
    RunState run, org.pipelineframework.connector.decision.DecisionResult result) {}
