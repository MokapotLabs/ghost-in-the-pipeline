# Does a sixth sense help?

This is the preserved haunted-question experiment at revision `1f263f8`. The [new action-focused question and browser fixes](action-prompt.md) have separate results. The reproduction commands below apply to this historical revision; use the follow-up instructions and fresh filenames with current code.

The game now has one deterministic haunted signal, shared by the human presentation and the optional agent observation. An independent breadth-first calculation measures corridor steps to the exit room. Exact distance, exit coordinates, direction and route never enter the model input or browser frame.

| Room distance, for level width `n` | Signal |
|---|---|
| At most `max(1, floor(n/2))` | INTENSE |
| Otherwise, at most `n` | STRONG |
| Otherwise, at most `2n` | NOTICEABLE |
| Farther | FAINT |

Thus the thresholds are **1 / 3 / 6** for 3×3, **2 / 4 / 8** for 4×4, and **3 / 6 / 12** for 6×6. Walls matter: an adjacent cell behind a wall can still have a long corridor distance. These deliberately coarse bands have plateaus; they do not point out the next action. This is an original new aid, not a claim of historical fidelity.

Movement compares the current band with the last occupied room's band: `GETTING_STRONGER`, `GETTING_WEAKER` or `UNCHANGED`. Turns and wall bumps report `NO_MOVEMENT`. A separate immutable reading list remembers only actually visited rooms, resets on level change, and survives pause/resume. The same typed `NavigationSignal` travels through the existing action pipeline; there are no extra services, model tools or execution roots.

## Play and listen

- **GHOST GLOW:** restrained rim illumination and a four-bar signal HUD. Slow breathing illumination respects reduced motion. An observed exit has a distinctive frame.
- **SOUND OFF / ON:** opt-in Web Audio triangle tones. They last 75 ms, recur every 4.6 / 3 / 1.7 / 0.85 seconds, and rise from 180 to 360 Hz. Discovery and crossing have original two/three-note chimes. Default master gain is 0.035; there are no external assets or microphone access. Mute, reset, game over and page destruction stop or clear active resources.
- **AI SIXTH SENSE:** chooses whether the next AI segment receives the signal, trend and visited-room readings. It is fixed during a segment; Pause returns control before changing it. Visual and audio switches operate independently and never change game rules.

With AI clues off, its question and observation are the preceding relative-edge version, with no signal fields or signal instructions. With clues on, the same request gains `signal`, `signalTrend`, `signalMemory` and an explanation of those qualitative facts. The model still proposes one of the same three actions through the existing TPF Decision Query. No legal-action replacement or solver is used.

## What happened

All trials used the fixed seeds **1983, 1986, 2026**, the same model identities, initial states, **128 decisions / 120 seconds**, ten-second provider timeout and no retries. Visual pacing was disabled in the separate evaluation process. The 0.8B test covers all three level sizes; the exploratory 4B follow-up covers only 3×3. Compare arms within each model, not their differently sized samples.

| Model / scope | Agent clues | Escapes | Decisions | Moves | Wall rejections | Rooms explored, summed | Re-entries | Entry-exhaustion losses | Median latency |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 0.8B · nine cases | Off | 0/9 | 1152 | 4 | 1148 | 13 | 0 | 0 | 7 ms |
| 0.8B · nine cases | On | 0/9 | 1152 | 4 | 1148 | 13 | 0 | 0 | 7 ms |
| 4B · three 3×3 cases | Off | 1/3 | 172 | 76 | 33 | 22 | 56 | 1 | 1233 ms |
| 4B · three 3×3 cases | On | 1/3 | 146 | 68 | 24 | 22 | 48 | 1 | 1682.5 ms |

**The extra sense did not improve escape counts in this experiment.** 0.8B selected FORWARD throughout both arms and hit its decision limit. Both 4B arms escaped seed 1986, timed out on 1983 and exhausted a room on 2026. Fewer 4B wall collisions with clues also came with fewer decisions and moves, and a slower median inference: that is not evidence of improved navigation. Its visited-room total did not improve. The signal was useful to the human in the recording; neither tested model consistently used it to escape.

The retained baselines were also rerun under the enhanced app: **Random 1/9, Explorer 9/9**, with their original observation-only policies. Neither uses the signal or a hidden map. No maze, seed, room allowance or core rule was adjusted. Original compact and relative-edge results remain intact.

One attempt per case/arm, coarse bands, prompt changes explaining the new facts and local warm timing limit interpretation. These results do not isolate model capacity, wording or planning behavior. A timeout can leave one pending Query without a committed action; decision and latency totals describe committed actions, not every in-flight request. Timed-out native exports can have partial lifecycles; complete bounded provider samples retain native root/lineage verification separately.

All action records, signal readings and model digests are in `evidence/haunted-*-off.json` and `evidence/haunted-*-on.json`. The two `haunted-*-comparison.json` files verify controls. `haunted-baselines.json` retains the reference runs.

## Watch the demonstration

[90-second audiovisual demonstration](recordings/sixth-sense-90s.mp4)

This records the **live game canvas, observed map and visible UI labels**, with audio captured directly from the game's Web Audio output. The human follows the signal to the discovered exit, then resets the ordinary seed 1983 for actual 0.8B attempts with clues off and on. The human stops at the exit doorway before resetting; no human victory is claimed in this clip. Both AI attempts hit the wall and are paused. No model choices or routes are scripted.

The first 60 seconds of raw human footage play at a clearly labelled **1.5×**; the remainder, including both AI attempts, stays at real speed. Pitch is preserved, and the captured audio is boosted 3× for audibility. In-game default gain remains conservative and audio starts muted. Output: 90 seconds, 1200×740, 20 fps, H.264/AAC. Raw capture is retained locally under ignored `.work/`; nothing was uploaded.

## Reproduce the experiment

```sh
GHOST_PACE_MS=0 ./scripts/run.sh
# Another terminal, sequentially; keep this session free of other interaction:
GHOST_PACE_MS=0 python3 scripts/evaluate.py --players TEV1 --agent-clues off --runtime app/target/quarkus-app --out evidence/haunted-08b-off.json
GHOST_PACE_MS=0 python3 scripts/evaluate.py --players TEV1 --agent-clues on --runtime app/target/quarkus-app --out evidence/haunted-08b-on.json
python3 scripts/compare-clues.py evidence/haunted-08b-off.json evidence/haunted-08b-on.json --out evidence/haunted-08b-comparison.json
# 4B follow-up: same two commands with --players TEV1_4B --levels 1 and separate 4b output names.
python3 -O scripts/test-clue-comparison.py
node scripts/test-audio.cjs
node scripts/test-renderer.cjs
mvn -B -ntp verify -Dghost.live=true
```

For normal play, restart with default pacing. Future inference choices and timings can differ. Saved actions replay deterministically and must be labelled as replay.
