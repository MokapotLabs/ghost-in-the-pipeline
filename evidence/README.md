# Demo Execution Dataset

This directory contains the compact, shareable game execution evidence, not the development transcript.

- `acceptance.json`: final 16-test live verification, named E2E, seeded-solvability and dependency-hash checks.
- `fixtures.json`: reproducible seeds, level sizes, bounds and expected human/integration results.
- `evaluation.json`: all 27 final paired trials, actual action records, decision latency and model identity/digest. No route was supplied to Tev1 or either baseline.
- `dependencies.json`: exact published TPF artifact timestamps and SHA-256 identities. The build POMs pin those versions.
- `replay/`: unmodified native TPF replay documents from completed human, real Tev1 and deterministic Explorer runs. Filenames identify the samples; their contents were not translated or reconstructed.
- `lineage.json`: native trace identity, event ordering, recursive invocation lifetime and root completion checks. Exported-document counts are diagnostic.
- `qualification-lineage.json`: the earlier thin-slice gate check. Those original gate documents remain local under ignored `evidence/raw/qualification/`; final completed-run samples are exported in `replay/`.

Run `python3 scripts/verify-replay.py evidence/replay` to inspect the final native invariants. Native events preserve their own trace/span and item identities. Screenshots from the actual game are in `../docs/screenshots/`.

The raw test/evaluation exporter directories also contain interrupted/budget-watchdog traces. The final samples are representative completed executions, not a claim that every interrupted export has a complete lifecycle. Full automated results are recorded in acceptance; no failed model escape was removed from the comparison.

Synthetic game data only. Raw console logs, source caches, account diagnostics, unrelated installed-model information, credentials and development-session exports are excluded. Nothing was uploaded automatically.
