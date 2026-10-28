package dev.ghost;

/** One qualitative reading at an actually visited room. Never a distance or route. */
public record SignalReading(int room, String signal) {
  public SignalReading {
    Signals.rank(signal);
  }
}
