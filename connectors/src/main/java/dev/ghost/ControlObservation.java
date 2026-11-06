package dev.ghost;

/** The run after observing current segment ownership, pause and time. */
public record ControlObservation(RunState run) {}
