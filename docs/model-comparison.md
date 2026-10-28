# Why the ghost walks into walls

This is the preserved compact-observation experiment. The current game now uses the clearer relative labels and offers both models in its menu; [the follow-up](relative-observations.md) reports the new 0/9 and 3/9 escape outcomes. The reproduction commands below apply to the compact-observation revision `b3de6ec`; use the follow-up instructions for current code and separate result filenames.

The collision loop is real model behavior. The Ollama response selects `FORWARD`; the TPF adapter preserves that selection; `DecodeChoice` validates its vocabulary; the shared reducer rejects movement into the wall. No fallback, solver action, state-write permission or automatic retry is involved. TPF result caching is bypassed.

## Fresh paired gameplay

Both installed models were run through the same native TPF pipeline on seeds **1983, 1986 and 2026**, at each of **3×3, 4×4 and 6×6**. Each case starts afresh, allows 128 decisions / 120 seconds, and uses the original compact NESW observation and persistent exploration memory. Presentation pacing was disabled. Maze generation, starting observations, instructions, criterion order and rules were unchanged. The pairing verifier checks all nine starting observations, source identities, six compiled-class identities and budgets. Later observations naturally differ as actions diverge. [Build verification](../evidence/comparison-build.json) ties the serving package to the current build. An earlier batch used an older observation formatter; those records are retained under [prior-build diagnostics](../evidence/prior-build/README.md) and excluded from the results below. Both models were rerun on the verified current package.

| Model | Exits | Decisions | Moves | Wall rejections | Re-entries | Rooms explored, summed |
|---|---:|---:|---:|---:|---:|---:|
| Tev1 0.8B | 0/9 | 1,152 | 4 | 1,148 | 0 | 13 |
| Tev1 4B | 1/9 | 616 | 102 | 257 | 80 | 30 |

0.8B selected **FORWARD on all 1,152 decisions** and stopped at the decision limit in every case. 4B selected FORWARD 359 times, LEFT 207 times and RIGHT 50 times. It escaped seed **1986, level 1**, in **15 decisions / 7 moves**. Six other attempts exhausted a room's entry allowance; two reached the decision limit. Larger-model exploration remained poor: 80 re-entries accompanied only 30 total rooms seen across the nine independent starts.

| Seed / level | 0.8B: outcome · moves / collisions | 4B: outcome · moves / collisions |
|---|---|---|
| 1983 / 1 | Limit · 0 / 128 | Lost · 14 / 27 |
| 1983 / 2 | Limit · 0 / 128 | Lost · 13 / 25 |
| 1983 / 3 | Limit · 0 / 128 | Limit · 8 / 60 |
| 1986 / 1 | Limit · 0 / 128 | Escaped · 7 / 4 |
| 1986 / 2 | Limit · 0 / 128 | Lost · 13 / 13 |
| 1986 / 3 | Limit · 0 / 128 | Lost · 13 / 15 |
| 2026 / 1 | Limit · 0 / 128 | Lost · 13 / 27 |
| 2026 / 2 | Limit · 0 / 128 | Lost · 13 / 26 |
| 2026 / 3 | Limit · 4 / 124 | Limit · 8 / 60 |

The precise per-case counts are authoritative in [model-comparison.json](../evidence/model-comparison.json); complete actions are retained in [0.8B trials](../evidence/comparison-08b-compact.json) and [4B trials](../evidence/comparison-4b-compact.json). The earlier fixed baselines remain **Random 1/9, Explorer 9/9**, with their unchanged policies and all actions in [the original comparison](../evidence/evaluation.json).

## Bounded diagnostic probes

Separately from gameplay, 24 direct provider requests replayed two saved visible observations: seed 1983's starting room, and that same room after collisions. In both, the player faces east into a wall; south is an observed passage, reachable by turning right. No action was applied. Each request had a ten-second timeout and no retry. Requests and raw probabilities are retained in [decision-diagnostics.json](../evidence/decision-diagnostics.json).

The probe crossed two representations with three cyclic criterion orders for each model. The **compact** representation was exactly the game observation. **Relative edges** added only redundant `front: wall, left: wall, right: open passage` facts computed from the same observed edges and orientation. It exposed no unseen cell, route, exit location or maze seed. Instructions and action descriptions stayed the same.

| Model | Compact observation, six probes | Same facts plus relative labels, six probes |
|---|---|---|
| 0.8B | FORWARD 6/6 | FORWARD 6/6 |
| 4B | FORWARD 3, LEFT 2, RIGHT 1 | RIGHT 6/6 |

This rules out a simple "always choose the first option" explanation for 0.8B on these fixtures. It also reproduces the bad choice directly at the provider, outside the application adapter. For 4B, making the relative opening explicit consistently changed the choice toward the passage. That supports an **observation-encoding sensitivity**: interpreting a compact NESW string, orientation and memory adds work before choosing an action. It does not establish the model's internal reasoning or prove that relative labels would fix full-maze exploration. The persistent 0.8B forward preference remains unexplained by this small probe; model capability, task wording and serving behavior are not fully isolated.

The request shape matches the documented [Ollama decision API](https://github.com/ollama/ollama/blob/main/docs/capabilities/decision.mdx) and [Tev1 model interface](https://registry.ollama.com/library/tev1): a structured state, named choice question and labelled criteria. This was not a chat-completion request accidentally parsed as an action. The diagnostic representation was **not promoted into the gameplay protocol** during the comparison.

## Identities, timing and limits

- `tev1:0.8b`: locally reported **752M / BF16**, digest `b661d565…f4bdbf1`.
- `tev1:latest`: locally reported **4.2B / mxfp8**, digest `4d24c6f6…bb7e53d3`; labelled 4B throughout. Full digests and model details are preserved in the datasets.
- Local Ollama **0.40.2**. These are different model sizes **and quantizations**, so this is not an isolated parameter-count experiment.
- Median/max decision latency: **0.8B 7 / 1,326 ms**, **4B 946.5 / 5,156 ms**. This includes native connector/projection overhead and excludes presentation delay. Repeated identical wall observations make extensive prompt reuse possible. The accepted model batches ran sequentially; prompt reuse and ordinary desktop activity mean local timing was not an isolated speed benchmark. No case hit the time bound or had a provider/schema failure.
- One live trial per model/case, fixed execution order, and two diagnostic fixtures do not support statistical significance or a general intelligence claim. Later inference can differ. No failed attempt was discarded.

## Reproduce

Run from the repository root. Use separate terminals for the server and evaluation, and stop the server between model configurations. Rebuild first if source has changed. Do not interact with the single session during evaluation.

```sh
ollama pull tev1:0.8b
ollama pull tev1:latest
# This tag was 4.2B in the recorded installation; verify its details.

GHOST_MODEL=tev1:0.8b GHOST_PACE_MS=0 ./scripts/run.sh
# In another terminal:
GHOST_PACE_MS=0 python3 scripts/evaluate.py --runtime app/target/quarkus-app --players TEV1 --out evidence/comparison-08b-compact.json

# Stop the server, then restart:
GHOST_MODEL=tev1:latest GHOST_PACE_MS=0 ./scripts/run.sh --no-build
GHOST_PACE_MS=0 python3 scripts/evaluate.py --runtime app/target/quarkus-app --players TEV1 --out evidence/comparison-4b-compact.json

python3 scripts/compare-models.py evidence/comparison-08b-compact.json evidence/comparison-4b-compact.json
python3 scripts/test-comparison.py
python3 scripts/probe-decisions.py
```

The scripts intentionally preserve actual failures. Comparison validation also remains active under `python -O`. Diagnostic calls are inspection requests only; all recorded gameplay choices execute through TPF's existing native Decision Query and shared rules.
