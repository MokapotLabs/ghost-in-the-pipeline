# A 60–90-second recording

An actual [90-second audiovisual take](recordings/sixth-sense-90s.mp4) is now included. It shows the human finding the exit doorway, then real 0.8B attempts on the same seed with navigation clues off and on. [Capture details and editing disclosures](haunted-sense.md) distinguish the accelerated human opening from real-time AI footage. To make a new take, enable optional sound with its visible toggle after clicking the page; use GHOST GLOW and AI SIXTH SENSE independently.

Start the application with `./scripts/run.sh`. Keep default visual pacing enabled. Run `./scripts/recording-reset.sh`, then open http://127.0.0.1:8087. This resets an ordinary seed, not a special route designed for the model.

| Time | What to show | Suggested narration |
|---|---|---|
| 0–10 s | The first-person maze and entry bars | “Those old home computers stayed with me. This is a little of that world, rebuilt in Java.” |
| 10–25 s | Use the arrows to turn and walk; watch the map fill in | “Seven entries per room. The maze remembers where you have been.” |
| 25–45 s | New Run on the same seed; choose Tev1 and Play | “Now a local 0.8B decision model gets the same openings and memory. It really chooses these moves.” |
| 45–55 s | Pause; show its latest action and measured decision time | “The rules belong to the game. A model proposal never writes the state.” |
| 55–75 s | New Run again; choose Explorer and Play | “A basic deterministic explorer is the comparison. It doesn't get a hidden map either.” |
| 75–90 s | Game view plus the README's result table | “The 0.8B ghost escaped zero of nine cases. The explorer escaped all nine. This ghost has more nostalgia than navigation.” |

The failed AI attempt is part of the story. Do not substitute Explorer footage for Tev1, imply a scripted route is autonomous, hide the player label, or claim that model confidence measures maze competence. If inference differs on a later run, describe what the recording actually shows.

For a 4B comparison take, select **TEV1-4B** in the menu and keep its model tag visible. Use the same ordinary seed and show the actual result. The [current paired investigation](relative-observations.md) records 3/9 escapes for 4B and 0/9 for 0.8B; do not promise an escape in a live recording. Let the animated quarter turns, map arrow and short forward steps establish orientation.

Use normal screen-recording software manually. There is no video-recording machinery or hosted deployment. Screenshots are in `docs/screenshots/`; action records and native replay examples accompany the evaluation dataset.
