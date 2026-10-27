# Demo Execution Dataset

This directory contains the compact, shareable game execution evidence, not the development transcript.

The newest **ordered-policy experiment** retains `policy-fixtures.json` (15 synthetic local cases), `policy-decision-cases.json` (60 actual decisions comparing two questions), and `policy-prompt-definition.json`. `policy-08b-off/on.json` and `policy-4b-off/on.json` retain 24 full maze trials and their comparisons. `policy-build.json`, `policy-acceptance.json`, `policy-lineage.json` and `replay-policy/` record verification. The current evaluator default is `evaluation-policy.json`; [the report](../docs/ordered-policy.md) distinguishes local-policy agreement from navigation outcomes.

The preceding **forward-first prompt and UI polish** retains `forward-08b-off/on.json`, `forward-4b-off/on.json`, clue comparisons and previous/new-question comparisons. `forward-prompt-definition.json` is the actual typed question; `polish-browser-checks.json`, `polish-build.json`, `polish-acceptance.json`, `forward-lineage.json` and `replay-forward/` record verification. The two `polish-live-*.json` files record separate real UI Play/Pause/resume checks, not comparison trials. That revision used `evaluation-forward-prompt.json`. See [the report](../docs/forward-and-polish.md).

The preceding **action-focused prompt** experiment uses `action-08b-off/on.json` (nine cases per arm), `action-4b-off/on.json` (three 3×3 cases per arm), their paired summaries and historical/new prompt comparisons. `action-prompt-definition.json` exports the actual typed question; `action-prompt-previous.json` preserves its predecessor; `action-prompt-diagnostics.json` retains eight provider-only probes. `action-acceptance.json`, `action-browser-checks.json`, `action-build.json` and `action-prompt-lineage.json` record focused verification. `replay-action-prompt/` preserves native completed provider samples. See [the report](../docs/action-prompt.md). That revision used `evaluation-action-prompt.json`.

The preceding **haunted-sense** experiment retains separate `haunted-08b-off/on.json` (nine cases per arm) and `haunted-4b-off/on.json` (three 3×3 cases per arm), paired comparison summaries, and `haunted-baselines.json`. `haunted-acceptance.json` records 21 Java tests, audio/renderer checks, seven paired-control checks and native lineage. `haunted-lineage.json` verifies unmodified bounded samples in `replay-haunted/`; raw timeouts remain separate and may be partial. The original compact and relative-label datasets remain intact. That historical revision wrote `evaluation-haunted.json` by default.

The preceding relative-observation experiment has separate `relative-08b.json` and `relative-4b.json` action records, `relative-model-comparison.json`, `relative-controls.json` (cross-encoding checks and original-file hashes), `relative-build.json`, `relative-acceptance.json` and `relative-lineage.json`. `replay-relative/` contains unmodified complete bounded provider samples for both native model branches. The original compact-observation files below remain unchanged. Those historical evaluations used `evaluation-relative.json`; the current default is the separate `evaluation-policy.json`.

- `acceptance.json`: historical 16-test delivery, named E2E, seeded-solvability and dependency-hash checks. The relative-observation 18-test run and 32 comparison checks are in `relative-acceptance.json`.
- `fixtures.json`: reproducible seeds, level sizes, bounds and expected human/integration results.
- `evaluation.json`: all 27 final paired trials, actual action records, decision latency and model identity/digest. No route was supplied to Tev1 or either baseline.
- `comparison-08b-compact.json`, `comparison-4b-compact.json`: 18 fresh model trials on the same nine cases, with all actions, observations, identities and source hashes. The original compact gameplay input was unchanged.
- `model-comparison.json`: verified pairing and aggregate/per-case summaries from those fresh trials.
- `comparison-build.json`: compiled-class verification against the current package. Source hashes identify checkout files separately from the serving binary.
- `decision-diagnostics.json`: 24 bounded provider-only probes of choice order and redundant relative-edge labels, including raw probabilities. These requests apply no gameplay actions.
- `investigation-acceptance.json`: follow-up tests, model-pairing checks, browser verification and review results.
- `prior-build/`: superseded paired trials retained after detecting an older serving package. These are explicitly excluded from current-release results, rather than discarded.
- `dependencies.json`: exact published TPF artifact timestamps and SHA-256 identities. The build POMs pin those versions.
- `replay/`: unmodified native TPF replay documents from completed human, real Tev1 and deterministic Explorer runs. Filenames identify the samples; their contents were not translated or reconstructed.
- `lineage.json`: native trace identity, event ordering, recursive invocation lifetime and root completion checks. Exported-document counts are diagnostic.
- `qualification-lineage.json`: the earlier thin-slice gate check. Those original gate documents remain local under ignored `evidence/raw/qualification/`; final completed-run samples are exported in `replay/`.

Run `python3 scripts/verify-replay.py evidence/replay` to inspect the final native invariants. Native events preserve their own trace/span and item identities. Screenshots from the actual game are in `../docs/screenshots/`.

The raw test/evaluation exporter directories also contain interrupted/budget-watchdog traces. The final samples are representative completed executions, not a claim that every interrupted export has a complete lifecycle. Full automated results are recorded in acceptance; no failed model escape was removed from the comparison.

Synthetic game data only. Raw console logs, source caches, account diagnostics, unrelated installed-model information, credentials and development-session exports are excluded. Nothing was uploaded automatically.
