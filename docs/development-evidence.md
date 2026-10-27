# Development evidence and privacy boundary

The repository's existing Entire integration is enabled. Git commits automatically capture session checkpoints; none were created manually. Significant milestones separate the deterministic game/TPF path, original interface, and final AI verification/demonstration. Use `git log` and the installed Entire CLI to inspect them locally. Start with `entire agent-help`, then `entire agent-help checkpoint list` for the installed flags.

The implementation followed the approved demo plan and TPF authoring guidance. A generation limitation was handled by inlining the shared action reducer into the existing recursive Pipeline, not by modifying TPF. The qualification report records the adaptation.

The final source and selected execution artifacts were scanned for credential-shaped values. Model observations and replay examples contain synthetic game facts and framework lifecycle metadata. The public dataset excludes console logs, account diagnostics, unrelated model listings, local configuration files and raw session exports.

**Raw development checkpoints are not publication-ready.** This session includes local filesystem paths, other repository references, account context from tooling, and inspected historical source whose redistribution rights are unresolved. Before sharing a session, inspect its actual export, redact private context and any credentials, remove unlicensed reference-source output, and explicitly choose the material to publish. Existing checkpoint push is disabled; no push, session upload or DEV publication was performed.

CodeRabbit reviewed the application changes. Five relevant application findings were addressed: missing request fields, timeout classification, modified keyboard shortcuts, consistent model room identities, and memory-map availability at narrow widths. Two duplicate findings about generated Entire Windows hooks were outside the application change and were not acted on. The user asked for a local game and no broader runtime/tooling work.

Delivery timing, interruptions and repair notes are kept separately in `.work/implementation-metrics.json`, not in the customer-facing dataset.

The follow-up CodeRabbit review of the corrected application changes completed with zero findings.

The later wall-choice investigation adds an animation milestone and a separate model-comparison checkpoint. A compiled-class check caught an older immutable evaluation package: its observation formatter lacked the existing room-label normalization. Both models had used that same package, but the trials were ineligible as current-release evidence. All records were retained under `evidence/prior-build/`, and both models were rerun from a package verified byte-for-byte against the current build. Evaluation records now distinguish checkout hashes from serving-class hashes. This was an application verification mistake; it required no TPF change.

The turn renderer has its own clock/geometry checks, and model-pairing tests reject altered observations, identities, budgets, duplicate cases and execution failures even under Python optimization. Provider-only observation probes apply no game actions. The fixed gameplay observations and maze generator were not tuned to improve either model's results.

The next user-requested checkpoint promotes relative labels into actual gameplay, adds a native 4B binding selected by the menu, and adds forward-step motion (`f37e2c2`). Question wording, criteria, mazes and rules remain unchanged. A separate 18-trial rerun and 32 offline validation checks preserve and compare the original results. Source review tightened model-role validation and allowed valid early terminal outcomes in the live test; the current acceptance record is `relative-acceptance.json`. Final live verification passed 18 Java tests and native lineage checks. No TPF runtime change was needed.
