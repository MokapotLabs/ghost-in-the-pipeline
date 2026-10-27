package dev.ghost;

public record ActionTurn(RunState run, String action, long latencyMs) {}
