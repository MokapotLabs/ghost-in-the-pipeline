package dev.ghost;

import java.util.List;

public record Maze(int size, List<Integer> walls, int exitRoom, int exitDirection) {
  public Maze {
    walls = List.copyOf(walls);
  }
}
