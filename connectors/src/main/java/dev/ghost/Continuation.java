package dev.ghost;

public sealed interface Continuation {
  String discriminator();

  record Again(RunState value) implements Continuation {
    public String discriminator() {
      return "again";
    }
  }

  record Done(RunResult value) implements Continuation {
    public String discriminator() {
      return "done";
    }
  }
}
