package dev.ghost;

public sealed interface NextTurn {
  String discriminator();

  record Model(ModelTurn value) implements NextTurn {
    public String discriminator() {
      return "model";
    }
  }

  record Large(LargeModelTurn value) implements NextTurn {
    public String discriminator() {
      return "large";
    }
  }

  record Action(ActionTurn value) implements NextTurn {
    public String discriminator() {
      return "action";
    }
  }

  record Jev(JevModelTurn value) implements NextTurn {
    public String discriminator() {
      return "jev";
    }
  }

  record Done(RunResult value) implements NextTurn {
    public String discriminator() {
      return "done";
    }
  }
}
