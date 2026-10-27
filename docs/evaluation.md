# What the players actually did

**Follow-up:** the [fresh paired model investigation](model-comparison.md) compares Tev1 0.8B and the installed 4B variant on the same nine cases: **0/9 versus 1/9 escapes**, with **1,148 versus 257 wall rejections**. It includes controlled observation/order probes and preserves the original gameplay inputs. The three-player experiment below remains the original baseline record.

Protocol: nine fixed cases (seeds 1983, 1986 and 2026 × levels 1–3), each started afresh for Tev1, Random and Explorer. All paired starting observations were verified identical. Each trial allowed 128 decisions and 120 seconds. No seed, map or route was changed in response to a model result.

| Player | Escaped | Decisions | Successful moves | Wall rejections | Rooms explored, summed over cases |
|---|---:|---:|---:|---:|---:|
| TEV1 | 0 / 9 | 1152 | 4 | 1148 | 13 |
| RANDOM | 1 / 9 | 1032 | 170 | 0 | 59 |
| EXPLORER | 9 / 9 | 263 | 155 | 0 | 138 |

| Seed / level | Tev1 | Random | Explorer |
|---|---|---|---|
| 1983 / 1 | DECISION_LIMIT, 0 moves | LOST, 17 moves | WON, 6 moves |
| 1983 / 2 | DECISION_LIMIT, 0 moves | DECISION_LIMIT, 18 moves | WON, 20 moves |
| 1983 / 3 | DECISION_LIMIT, 0 moves | DECISION_LIMIT, 17 moves | WON, 16 moves |
| 1986 / 1 | DECISION_LIMIT, 0 moves | WON, 15 moves | WON, 7 moves |
| 1986 / 2 | DECISION_LIMIT, 0 moves | DECISION_LIMIT, 24 moves | WON, 18 moves |
| 1986 / 3 | DECISION_LIMIT, 0 moves | DECISION_LIMIT, 25 moves | WON, 36 moves |
| 2026 / 1 | DECISION_LIMIT, 0 moves | DECISION_LIMIT, 17 moves | WON, 8 moves |
| 2026 / 2 | DECISION_LIMIT, 0 moves | LOST, 13 moves | WON, 14 moves |
| 2026 / 3 | DECISION_LIMIT, 4 moves | DECISION_LIMIT, 24 moves | WON, 30 moves |

The recorded Tev1 decision latency had a median of **7 ms** and a maximum of **1995 ms**. This is wall time from the control observation to validated response, including connector/projection overhead. Visual pacing was disabled for every policy. Baseline latency is not instrumented; zero in a baseline action record is a sentinel, not a claim of zero computational cost. Ollama can reuse prompt computation internally; these timings are specific to this local model/hardware and include warm requests. TPF result caching was bypassed.

## Failure is the result

Tev1 made genuine finite-choice requests and consistently selected forward. Most attempts encountered a wall, so it barely explored and never escaped. This is the observed failure mechanism. The experiment does not isolate whether prompt encoding, the small model, fixed option order or another model-serving factor caused that preference. It does not establish general model intelligence or a universal model ranking. No navigation heuristic was substituted for its decisions.

Random chooses only legal moves and turns; its failures include room exhaustion and the action budget. Explorer uses a fixed direction preference, learned edges, visited rooms and a depth-first stack. It receives neither the hidden maze nor the solvability witness. Its success is a deliberately simple reference point, not AI inference.

The observation normalizes human-facing room text so it cannot contradict zero-based room identities. This was a review correction before the final comparison, not route assistance or maze tuning. Native generation, seeds, budgets, choice order and policies were unchanged.

## Metric definitions and limits

- A decision is an attempted action, including a turn or collision. A successful move changes rooms or crosses the exit.
- Repeated positions count room re-entries, not in-place turns. Rooms explored include the starting room.
- Wall attempts are rejected movement proposals; they count in both `collisions` and total `rejectedActions`. No unknown-vocabulary choices occurred in this completed comparison. Provider failures are reported separately, not concealed as successful moves.
- Runs stop at escape, entry exhaustion, pause, a decision limit, a deadline, or a provider/pipeline failure. The model is never required to escape for an integration test to pass.
- This is one trial per policy/case. It does not include repetitions, option-order counterbalancing, prompt sweeps or training. It makes no statistical-significance claim.

The full action records, initial and final observations, model identity/digest and per-trial summaries are in [evaluation.json](../evidence/evaluation.json). Fixtures and deterministic state transitions can be reproduced exactly. Live inference and timing may differ on a later run; replaying saved actions must be labelled as replay.
