# A 60–90-second recording

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

For a 4B comparison take, restart with `GHOST_MODEL=tev1:latest ./scripts/run.sh --no-build` and keep its model tag visible. Use the same ordinary seed and show the actual result. The [paired investigation](model-comparison.md) includes both models' full attempts; do not promise an escape in a live recording. Let the animated quarter turns and map arrow establish orientation before moving again.

Use normal screen-recording software manually. There is no video-recording machinery or hosted deployment. Screenshots are in `docs/screenshots/`; action records and native replay examples accompany the evaluation dataset.
