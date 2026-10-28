# Ordered-policy experiment

The candidate `ordered-policy-v3` uses the approved nine-rule policy: visible exits first, then move forward after a turn when the passage is open, then first-visit forward movement, left/right opening preferences and a final turn to inspect behind. It uses the existing `FORWARD`, `TURN_LEFT` and `TURN_RIGHT` labels. Criteria descriptions and order are unchanged.

`current_room_visits` is a direct alias of the current room's existing memory count. All earlier observation facts and structured exploration memory remain available. No desired action, route, unseen topology or solver output is supplied. The application does not execute the policy for the model: Tev1 still chooses every gameplay action, and the existing rules validate it.

## Fixed local cases

Fifteen frozen synthetic classification fixtures cover corridors, blocked paths, dead ends, exits, junctions, revisits, turn commitment and leaving a room with one entry remaining. These are local observation tests, not admitted mazes or valid full gameplay histories. Both questions receive identical fixture states, including the visit alias and observed-memory rows. Expected labels remain outside the provider request.

| Model | Previous forward-first question | Ordered question | Forward-into-wall attempts, previous / ordered | Missed turn commitments, previous / ordered |
|---|---:|---:|---:|---:|
| 0.8B | 6/15 | 6/15 | 5 / 5 | 1 / 1 |
| 4B | 12/15 | 11/15 | 0 / 0 | 0 / 0 |

These scores count agreement with the corrected policy, not general navigation accuracy. The previous question did not prescribe every new tie-break. There were 60 genuine provider decisions and no provider errors. The fixture set and candidate were not changed in response to their scores.

0.8B chose FORWARD in 14 of the 15 cases under each question. In the after-right-turn fixture it chose TURN_RIGHT again. 4B handled walls and turn commitments, but both questions missed the left-hand exit and the two revisited-junction choices. The ordered question also selected RIGHT instead of the specified LEFT when both side passages were open. More explicit ordering did not improve this local test.

## Same maze cases

Previous forward-first version → ordered policy:

| Model / scope | Clues | Escapes | Moves | Wall attempts | Rooms, summed |
|---|---|---:|---:|---:|---:|
| 0.8B · nine cases | Off | 0/9 → 0/9 | 4 → 6 | 1022 → 899 | 13 → 15 |
| 0.8B · nine cases | On | 0/9 → 0/9 | 10 → 6 | 1038 → 643 | 17 → 15 |
| 4B · three cases | Off | 1/3 → 1/3 | 82 → 77 | 0 → 0 | 22 → 22 |
| 4B · three cases | On | 1/3 → 1/3 | 68 → 70 | 0 → 0 | 22 → 22 |

No escape-count gain was observed. Both small-model arms reached every decision limit. In both 4B arms, 1983 timed out, 1986 escaped in 11 decisions / 7 moves, and 2026 exhausted a room. Median committed-decision latencies were 9 ms for 0.8B in either arm, and 1576 / 1893.5 ms for 4B off/on. Revisit, exhaustion and complete per-turn metrics remain in the JSON records.


Seeds remain 1983, 1986 and 2026. 0.8B covers all nine seed/level combinations; 4B covers the same three 3×3 cases as the preceding experiment. Each clue arm starts afresh with 128 decisions, 120 seconds, a ten-second provider timeout, no retries and zero presentation delay. No seed, maze, entry allowance, baseline policy or pipeline was modified. One attempt per case and local warm timings limit interpretation.

The ordered policy does not use signal readings to select actions. Its clue-on/off arms test sensitivity to extra context; they do not establish the usefulness of a signal-aware navigation policy. The previous clue explanation is retained, while the new ordered question explicitly gives its rules priority. This is an intentionally narrow policy-compliance experiment.

The full-maze comparison changes the question plus one redundant observation field. The local fixture comparison holds state identical to isolate the question. Earlier result files are preserved; `policy-*-off/on.json` and comparison summaries contain the new trials. `policy-decision-cases.json` contains the fixed-case requests and decisions; `policy-prompt-definition.json` exports the actual typed question.

The app remains a single-session local game. UI resources and `pipeline.yaml` are byte-identical to the preceding delivery. Native bounded provider samples in `replay-policy/` retain root identity, nesting and termination checks. No TPF runtime capabilities or production infrastructure were added.

## Reproduce

```sh
# Local policy comparison; choose a fresh output filename:
python3 scripts/evaluate-policy.py --out evidence/policy-cases-reproduction.json
python3 -O scripts/test-policy.py
mvn -B -ntp verify -Dghost.live=true

# Start a separate game instance for full trials:
GHOST_PORT=8088 GHOST_PACE_MS=0 ./scripts/run.sh
# In another terminal; do not interact with that single session:
GHOST_PACE_MS=0 python3 scripts/evaluate.py --url http://127.0.0.1:8088 --runtime app/target/quarkus-app --players TEV1 --agent-clues off --out evidence/policy-reproduction-off.json
GHOST_PACE_MS=0 python3 scripts/evaluate.py --url http://127.0.0.1:8088 --runtime app/target/quarkus-app --players TEV1 --agent-clues on --out evidence/policy-reproduction-on.json
# Repeat for TEV1_4B with --levels 1 and separate filenames.
```

The evaluator refuses to overwrite earlier results. Native action validation, execution budgets, entry exhaustion and level progression remain application-owned. The reference policy function exists only in the diagnostic/test scripts and is never an application fallback.

Verification: 23 Java tests passed with real inference from both models, including the named human-campaign/bounded-agent E2E. Six policy checks and 49 historical control checks passed. Renderer, audio lifecycle and native lineage checks passed. [Acceptance record](../evidence/policy-acceptance.json). The tested candidate is served at port 8087 with normal presentation pacing.
