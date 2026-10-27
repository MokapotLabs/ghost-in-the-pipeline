# Ghost in the Pipeline 👻

**A maze from the 1980s, a Java framework from 2026, and an AI trying to find its way out.**

An independently implemented, MSX-inspired first-person maze built with [The Pipeline Framework](https://pipelineframework.org/). Play with the keyboard, hand control to a real local Tev1 decision model, or compare it with Random and a deterministic Explorer. Seven entries per room; three growing levels; no hidden map for the AI.

**New: a haunted sixth sense.** Optional glow and quiet synthesized tones grow stronger along corridors toward the exit. The AI can receive the same qualitative signal and visited-room readings. [Watch the 90-second audiovisual demonstration](docs/recordings/sixth-sense-90s.mp4) and read [the controlled clue-on/off experiment](docs/haunted-sense.md): escape counts did not improve in these tested cases.

**Latest experiment:** [ordered policy with a turn-commit rule](docs/ordered-policy.md). The fixed-case comparison and same-seed reruns showed no escape gain. Existing memory is preserved, with a direct current-room visit summary.

**Game UI:** [compact game UI and forward-first prompt results](docs/forward-and-polish.md). The hero and slogans are removed; the supplied portal PNG is used for branding and favicon. Instructions live in Help. The model now receives the ordered policy described above; earlier questions and results remain preserved.

![The game at 1280×720](docs/screenshots/polished-game-1280.png)

## Run locally

Requirements: Java 21+, Maven 3.9+, Python 3 for the helper scripts, and Ollama with its decision API. The recorded run used Java 25 while compiling Java 21-compatible code. Dependencies and model weights need an initial download; gameplay then runs locally without a cloud account, CDN or hosted inference service.

```sh
ollama pull tev1:0.8b
ollama pull tev1:latest  # Optional 4B menu choice; recorded installation is 4.2B / mxfp8.
# Start Ollama if it is not already running.
./scripts/run.sh
```

Open **http://127.0.0.1:8087**. Use **↑ / W** to walk, **← / A** and **→ / D** to turn. Choose **tev1:0.8b**, **tev1:latest · 4B**, Explorer or Random in the menu, then Play. Pause finishes at most the current turn. Reset restarts the selected seed. An observed-only map and entry bars help you keep your bearings.

Turns rotate the room and map arrow over a short quarter turn, with a direction cue. Successful forward steps have a brief approach and footstep bob; collisions do not animate room entry. Reduced-motion preferences keep transitions instant.

**GLOW**, **SOUND OFF/ON**, and **AI CLUES** are independent controls. Audio starts muted and is enabled by clicking its toggle. The AI setting applies to the next Play segment. Turns and bumps never improve the proximity trend. The seven-entry rules and maze generator are unchanged.

The levels are 3×3, 4×4 and 6×6. Each room starts with seven entries; entering decrements its allowance and reaching zero loses. Spawning, turning and bumping into a wall consume none. Cross the exit to advance; escape the third level to win. Every admitted level is checked for a valid escape under these same rules.

A second launch can reuse the package with `./scripts/run.sh --no-build`. Change the port with `GHOST_PORT=8089`. The two native model bindings can be configured with `GHOST_MODEL` (small), `GHOST_LARGE_MODEL` (4B), and `GHOST_MODEL_URL`; the launcher writes native TPF deployment bindings through `PIPELINE_CONFIG`. The supplied host connection is for local Ollama, not hosted credential management.

## Test and compare

```sh
mvn -B -ntp verify
mvn -B -ntp verify -Dghost.live=true  # Includes real Tev1 inference; Ollama must be running.
node scripts/test-renderer.cjs      # Optional renderer/animation checks; Node.js 18+.

# With the application running:
python3 scripts/evaluate.py
./scripts/recording-reset.sh
```

The ordinary test suite uses real TPF gameplay and observation-only baselines. The opt-in live test validates genuine decisions from both models without requiring an escape or an exact action. The [current acceptance record](evidence/policy-acceptance.json) reports **23 passing Java tests**, with signal distance/trend/privacy checks, plus audio and renderer checks. Core tests validate **600 seeded levels**. `haunted-acceptance.json`, `relative-acceptance.json` and `acceptance.json` retain the historical 21-, 18- and 16-test deliveries.

Evaluation uses the same nine seed/level combinations for all players, with 128 decisions and 120 seconds per case. For timings without the visual pause, restart with `GHOST_PACE_MS=0 ./scripts/run.sh --no-build`, then run `GHOST_PACE_MS=0 python3 scripts/evaluate.py`. Do not interact with that single session during evaluation. The script leaves a fresh normal campaign ready afterwards.

The preserved original compact-observation results:

| Player | Escaped | What actually happened |
|---|---:|---|
| Tev1 0.8B | 0 / 9 | Repeated forward attempts, mostly rejected by walls |
| Tev1 4B | 1 / 9 | Explored more; still hit walls and repeatedly exhausted rooms |
| Random legal movement | 1 / 9 | Occasionally escaped; also exhausted rooms or its action budget |
| Deterministic Explorer | 9 / 9 | Used visited-room memory and depth-first exploration |

These are small-sample observations, not a general model ranking. The maze and seeds were not adjusted to make Tev1 win. Full actions, latencies and paired starting observations are in [the evaluation dataset](evidence/evaluation.json); see [the report](docs/evaluation.md) for definitions and limits.

The current [prompt investigation](docs/action-prompt.md) preserves clue-on/off reruns for the same nine small-model cases and three 3×3 large-model cases. The [original model investigation](docs/model-comparison.md) retains every compact-observation attempt. The current game adds redundant front/left/right labels to actual gameplay; see [the follow-up comparison](docs/relative-observations.md) for both reruns on the unchanged seeds and rules. Switch between the two models directly in the menu. Later observations differ as their actions diverge.

With clearer observations, **0.8B remained at 0/9 escapes** and **4B improved to 3/9**, with 276 successful moves and 105 wall rejections. Five 4B attempts still timed out. Original result files are preserved; the new action records are `evidence/relative-08b.json` and `evidence/relative-4b.json`.

## How it works

One bounded TPF execution owns each automated Play segment. Typed recursive composition makes the next turn; a native Decision Query asks Tev1 for one action; the same reducer used by human play validates it. A native Command publishes the frame. The model has no state-write authority.

The model receives visited rooms, observed walls/passages/exits, entry allowances, orientation and the prior result. Front/left/right labels express the same visible edges in plain language. It does not receive unseen topology, the maze seed, the validation witness or a computed route. Explorer uses those same observations, not the solver.

See [architecture](docs/architecture.md), [execution contract](docs/execution-contract.md), and [bounded qualification](docs/qualification.md). Published TPF snapshot binaries are pinned by timestamp; [dependency identities and SHA-256 values](evidence/dependencies.json) record what was exercised. No TPF runtime changes were made. MCP was unnecessary and is omitted.

## Record and inspect

The primary deliverable is the game. The [90-second audiovisual recording](docs/recordings/sixth-sense-90s.mp4) shows human wayfinding and real 0.8B play with clues off and on. See [capture/edit details](docs/haunted-sense.md) or follow [the recording sequence](docs/recording.md) for a fresh take. [The DEV submission](docs/dev-submission.md) remains a draft requiring publication links and reviewed session material. Nothing was uploaded or published automatically.

Native replay goes to `target/replay/` by default. Set `GHOST_REPLAY_PATH` to an absolute directory for a separate run. [The execution dataset](evidence/README.md) includes representative native documents and root/lineage verification:

```sh
python3 scripts/verify-replay.py evidence/replay
```

Meaningful commits are captured by the repository's existing Entire session tooling. See [development evidence](docs/development-evidence.md). Checkpoints remain local and require privacy review before sharing. Nothing is pushed or published automatically.

## Historical provenance and boundaries

Inspired by [antor44's 3D Maze Pro](https://github.com/antor44/3dmaze). [The comparison](docs/historical-reference.md) documents inspected mechanics, differences and optional emulator instructions. No historical source, graphics, music, binary, ROM or firmware is bundled or required.

Original contributions use **Apache-2.0**. Upstream redistribution rights remain unresolved; that license claim does not extend to historical material or model weights. Read [NOTICE.md](NOTICE.md).

This is a loopback-only, single-session, in-memory demo. Restart loses progress. [Limitations and follow-on work](docs/limitations.md) describe the boundary; there is no cloud deployment, multiplayer, training system or production infrastructure.
