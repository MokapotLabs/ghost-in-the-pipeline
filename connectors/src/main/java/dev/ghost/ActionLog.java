package dev.ghost;

public record ActionLog(
    int turn,
    int level,
    int room,
    int direction,
    String action,
    String result,
    long latencyMs,
    String player) {}
