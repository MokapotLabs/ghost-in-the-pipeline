# Historical comparison

Reference inspected: [antor44/3dmaze](https://github.com/antor44/3dmaze), especially the readable [`3dmaze` listing](https://github.com/antor44/3dmaze/blob/main/3dmaze), identifying version 2.44. The `.bas` downloads are tokenized; the extensionless listings are readable. No source was imported into the application.

| Mechanic | Historical observation | Modern interpretation |
|---|---|---|
| Maze | Per-room directional wall array; randomized internal east/north wall choices, closed boundaries, one exit (lines 120–340) | Independent seeded depth-first generator; connected rooms; one boundary exit |
| Start | Random position subject to separation from exit (360) | Reproducible farthest-room start, independent of any player |
| Movement | Forward and quarter-turns; no backward movement (470–650) | Same three actions; deterministic validation |
| Perspective | Direction selects front and side walls (370–450) | Original first-person geometry showing observed openings |
| Failure | Entry counters start at 11−2×difficulty; decrement on entering; zero loses (110, 650–660) | Medium setting: seven entries; the seventh entry exhausts a room. Spawn consumes none |
| Collision | Rejects movement without decrementing a room counter (630) | Same behavior |
| Progression | Exit advances maze size; initial sizes are 3, 4, 6 (9000–9010) | Those three sizes, followed by a final campaign win |
| Aids | Limited map reveals, directional sound assistance and visit warning colors | Persistent observed memory, entry bars; no full-map reveal or directional sound |
| Score | No numeric scoring found in the inspected listing | Moves and exploration are experiment measurements, not a score |

## Running the historical game separately

The upstream README links to a WebMSX session and names openMSX as another option. Obtain the original game separately from its maintainer and follow that emulator's own instructions with firmware you are entitled to use. This repository does not distribute or download ROMs, cassette images or firmware, and its setup does not launch an emulator.

No emulator-based behavior comparison was performed. The comparison above is source inspection. The modern game runs independently. See [NOTICE.md](../NOTICE.md) for unresolved upstream redistribution rights.
