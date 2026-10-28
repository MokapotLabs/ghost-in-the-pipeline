# The prompt, the spinning ghost, and the picker

This preserves the action-focused-v1 delivery at revision `95386fe`. The [forward-first follow-up](forward-and-polish.md) retains a new instruction and UI polish with separate results. Use that follow-up for current reproduction commands.

This follow-up changes the decision question and fixes browser controls. It preserves the maze, seven-entry rules, observed information, finite action order, model weights and budgets. Earlier compact, relative-edge and haunted-clue results remain in their original files.

## Where the prompt actually lives

The native TPF Decision Query takes a typed `DecisionRequest`, rather than a chat message. `ChooseTurn` constructs it through `Observations.request()`. Its question contains `instructions` and three labelled criteria. `pipeline.yaml` maps that request to the connector with `request: request`; it now points explicitly to `Observations.ACTION_PROMPT` and `CLUE_PROMPT`. There was already a prompt; its location was difficult to discover.

The new, frozen `action-focused-v1` question begins:

> Choose ONE action for this turn. Read relativeEdges: front, left, right.
> If front is wall, choose a turn: FORWARD cannot pass a wall.
> If front is exit, choose FORWARD.

It also asks for an open passage, fewer revisits and care around a room's last entry, and briefly defines the memory rows. The optional clue paragraph defines the signal order, remembered readings and weaker-signal response, and explains that a turn or bump is not progress. The criteria now describe when each action is appropriate. All three actions remain available; the model can still propose a collision. There is no action masking, solver replacement or automatic retry.

[The actual exported requests](../evidence/action-prompt-definition.json) contain both clue settings. [The previous requests](../evidence/action-prompt-previous.json) preserve the earlier question. Their states are identical within each clue setting. These are wire-shaped exports of real Java request values, not reconstructed prompts from a source-text search. Future evaluations also reject a server whose advertised prompt version differs.

## What the diagnosis establishes

Eight bounded provider-only probes compare the previous and new questions on the same observed seed-1983 starting room: front and left are walls; right is a passage. Each model sees each prompt with clues off and on. No gameplay action is applied by these probes.

| Model | Previous prompt: selected action / FORWARD probability | New prompt: selected action / FORWARD probability |
|---|---|---|
| 0.8B, clues off | FORWARD / 75.5% | FORWARD / 59.3% |
| 0.8B, clues on | FORWARD / 78.0% | FORWARD / 51.8% |
| 4B, clues off | TURN_RIGHT / 33.1% | TURN_RIGHT / 7.5% |
| 4B, clues on | TURN_RIGHT / 32.0% | TURN_RIGHT / 6.0% |

[Requests and raw responses](../evidence/action-prompt-diagnostics.json) show that the provider receives nonempty instructions and changes its scores with the wording. The bad 0.8B choice also occurs directly at the provider. The application preserves that choice and the shared rules reject it. This is not evidence of a missing prompt or an action-conversion bug.

The full runs reveal another failure mode: after the first bump, the small model usually keeps turning right, including when a passage is ahead. Changing the wording can change the failure without producing navigation. The coarse proximity signal has plateaus and supplies no exit direction; exploiting it requires comparing remembered visits and making useful moves. The records do not establish the model's internal reasoning, or fully isolate model capacity, serving behavior and prompt sensitivity.

## Controlled full runs

Results below are generated from all completed saved trials. Seeds are 1983, 1986 and 2026. As in the preceding haunted experiment, 0.8B covers all nine seed/level combinations; 4B covers the three 3×3 cases only. Each arm starts afresh with 128 decisions, 120 seconds, ten-second provider timeout, no retries and zero presentation delay. Models run sequentially. No case was discarded or maze modified.

| Model / scope | Question | Clues | Escapes | Decisions | Moves | Wall collisions | Rooms, summed | Re-entries | Exhaustions | Median latency |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 0.8B · nine cases | Previous | Off | 0/9 | 1152 | 4 | 1148 | 13 | 0 | 0 | 7 ms |
| 0.8B · nine cases | Previous | On | 0/9 | 1152 | 4 | 1148 | 13 | 0 | 0 | 7 ms |
| 0.8B · nine cases | Action-focused | Off | 0/9 | 1152 | 4 | 136 | 13 | 0 | 0 | 9 ms |
| 0.8B · nine cases | Action-focused | On | 0/9 | 1152 | 4 | 6 | 13 | 0 | 0 | 8 ms |
| 4B · three 3×3 cases | Previous | Off | 1/3 | 172 | 76 | 33 | 22 | 56 | 1 | 1233 ms |
| 4B · three 3×3 cases | Previous | On | 1/3 | 146 | 68 | 24 | 22 | 48 | 1 | 1682.5 ms |
| 4B · three 3×3 cases | Action-focused | Off | 1/3 | 174 | 96 | 0 | 24 | 74 | 1 | 1445.5 ms |
| 4B · three 3×3 cases | Action-focused | On | 1/3 | 122 | 68 | 0 | 22 | 48 | 1 | 1861.5 ms |

Escape counts did not improve. 0.8B replaced most forward attempts with right turns: **1,012 turns without clues; 1,142 with clues**, out of 1,152 decisions per arm. It still made only four successful moves, visited the same 13 summed rooms and hit every decision limit. Fewer wall rejections therefore did not mean better exploration.

4B recorded **zero collisions in both new arms**. Each still timed out on 1983, escaped 1986 and exhausted a room on 2026. With the new question, seed 1986 took 39 decisions / 23 moves without clues versus 11 / 7 with clues. That individual benefit did not generalize to more escapes across the three cases. Both timed-out 1983 runs visited all nine rooms.

The [0.8B controls](../evidence/action-08b-prompt-comparison.json) and [4B controls](../evidence/action-4b-prompt-comparison.json) check both old/new questions and both clue arms. Full action records are `evidence/action-08b-off/on.json` and `evidence/action-4b-off/on.json`. The recorded serving package's 12 game/agent classes match the final package in [the build check](../evidence/action-build.json); the later view-only prompt-version check does not change them.

These are one-attempt local observations, not a statistical ranking. Counts are committed actions; a deadline can cancel one pending inference. Wall avoidance, exploration and escaping are separate outcomes. Existing observation-only baselines remain Random 1/9 and Explorer 9/9 from the preserved haunted runs; they were not retuned or rerun for a question-only change.

## Browser repair

The frame stream arrives every 150 ms. Its update function previously assigned text to each model option on every frame, even when unchanged. That repeatedly mutates the native menu. Labels and the picker disabled state are now changed only when necessary. Selecting a player returns keyboard focus to the maze. Editable seed fields retain their normal keyboard behavior, including Enter after game over.

Safari's native checkbox appearance also overrode the intended dimensions. The clue controls now use explicit 16×16 independent styling and a drawn check mark. Versioned style/script URLs avoid stale cached controls. [A screenshot from the running application](screenshots/action-controls.png) shows 4B selected and a human step completed. No gameplay dashboard or framework capability was added.

The disposable browser regression runs both WebKit 26.5 and Chromium 151: no picker mutations during an idle frame stream, all four variants selectable, movement focus, clue-checkbox keys, isolated seed editing, pause returning control and checkbox geometry. This is WebKit engine testing, not a claim of a manual native Safari test.

## Reproduce

Use a separate instance/session for evaluation; do not interact with it during trials. Choose fresh result names to preserve these records.

```sh
GHOST_PORT=8088 GHOST_PACE_MS=0 ./scripts/run.sh
# Another terminal:
GHOST_PACE_MS=0 python3 scripts/evaluate.py --url http://127.0.0.1:8088 --runtime app/target/quarkus-app --players TEV1 --agent-clues off --out evidence/reproduction-08b-off.json
GHOST_PACE_MS=0 python3 scripts/evaluate.py --url http://127.0.0.1:8088 --runtime app/target/quarkus-app --players TEV1 --agent-clues on --out evidence/reproduction-08b-on.json
python3 scripts/compare-clues.py evidence/reproduction-08b-off.json evidence/reproduction-08b-on.json --out evidence/reproduction-08b-comparison.json
# Repeat the two arms with --players TEV1_4B --levels 1 and separate output names.
python3 -O scripts/test-prompt-comparison.py
python3 scripts/verify-replay.py evidence/replay-action-prompt
mvn -B -ntp verify -Dghost.live=true
```

The browser regression uses a disposable UI-only fixture, not the running game or a model. To install its optional development-only dependency and run both engines:

```sh
npm install --no-save --prefix .work/browser-test playwright
node .work/browser-test/node_modules/playwright/cli.js install chromium webkit
NODE_PATH="$PWD/.work/browser-test/node_modules" node scripts/test-browser-controls.cjs
```

Playwright is not an application dependency. The existing 90-second recording documents the previous haunted-question revision; it is retained as historical footage, not presented as this prompt's outcome.

Verification: **22 Java tests passed with real inference from both models**, including the named human-campaign/bounded-agent E2E. Nine prompt-control tests, seven historical clue-control tests, 32 historical comparison tests, renderer/audio checks and both browser engines passed. Native complete provider samples preserve root identity, nesting and termination. [Acceptance record](../evidence/action-acceptance.json).
