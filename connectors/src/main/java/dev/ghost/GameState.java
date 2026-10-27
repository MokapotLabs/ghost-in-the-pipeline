package dev.ghost;

import java.util.List;

public record GameState(
    long seed,
    int level,
    int lastLevel,
    Maze maze,
    int room,
    int direction,
    List<Integer> remaining,
    List<RoomMemory> memory,
    String status,
    int moves,
    int actions,
    int collisions,
    int repeats,
    int explored,
    int exitsFound,
    String lastAction,
    String message) {
  public GameState {
    remaining = List.copyOf(remaining);
    memory = List.copyOf(memory);
  }
}
