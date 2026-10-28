package dev.ghost;

import java.util.List;

/** Application-owned sensory observation, carried immutably through the existing pipeline. */
public record NavigationSignal(
    String signal, String signalTrend, int level, int room, List<SignalReading> readings) {
  public NavigationSignal {
    Signals.rank(signal);
    if (!List.of("NO_MOVEMENT", "UNCHANGED", "GETTING_STRONGER", "GETTING_WEAKER")
        .contains(signalTrend)) throw new IllegalArgumentException("Unknown signal trend");
    readings = List.copyOf(readings);
  }
}
