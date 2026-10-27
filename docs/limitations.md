# Limitations and follow-on work

This is a local nostalgic engineering demo. Tev1's measured navigation is poor; finite-choice schema validity does not imply useful exploration. The nine cases are a small deterministic sample, one run per policy/case, and model output and timings may change across hardware, model revisions or providers. Replaying recorded actions is deterministic; repeating live inference is not guaranteed to be identical.

The [optional haunted-sense experiment](haunted-sense.md) found no escape-count improvement: 0.8B escaped 0/9 with clues off and on; 4B escaped 1/3 in each arm of a separate three-case 3×3 follow-up. This smaller scope cannot be compared directly with the previous nine-case 4B total. Coarse bands have plateaus and provide no direction or route. Audio is opt-in, depends on browser Web Audio support and stops or suspends with lifecycle events.

The [model follow-up](model-comparison.md) found 0/9 escapes for 0.8B and 1/9 for 4B with unchanged inputs. Relative-edge labels helped 4B on two diagnostic observations; this was not tested as a full campaign policy or substituted into the comparison. The root cause of 0.8B's persistent forward preference is not fully isolated. The installed variants also differ in quantization, and warm timings are not an isolated hardware benchmark.

The subsequent [actual-gameplay experiment](relative-observations.md) uses those relative labels in both models' inputs. 0.8B still escaped 0/9 cases; 4B escaped 3/9, with five timeouts and one entry-exhaustion loss. The original results remain available. Improved local collision avoidance does not imply reliable navigation. Time-limit records describe committed actions; cancellation can leave an in-flight Query without an action or complete native lifecycle.

The perspective is a stylized view of the current room. An open doorway fades into unknown space; it does not render hidden neighboring topology. There is no emulator, numeric score, sound system, backward move, full-map reveal, network multiplayer or training system. The historical generator and progression beyond three levels are intentionally replaced.

The application has one in-memory local session and no authentication, persistence or recovery across restart. It binds to loopback. Do not use it as a public multi-user service. Model settings are trusted deployment inputs; the supplied host connection is for local Ollama and uses its placeholder bearer value. Hosted credentials are outside this demo.

Native replay captures execution identity and lifecycle. A forced outer cancellation can leave partial step lifecycles; retain and distinguish those from completed normal-run traces. The verifier is used on completed recording/evaluation runs, not to relabel interrupted runs as successful. Console logs may contain provider diagnostics and local paths; they are excluded from the publishable execution dataset.

A production successor would need an explicit multi-user/session model, durable storage and recovery, authentication and authorization, configured provider credentials, rate limits, telemetry retention, privacy review, dependency/release maintenance, CI and broader accessibility/device testing. None is necessary to demonstrate this local game, and none is implemented here.

MCP was deliberately omitted because no existing tool materially improved the playable demonstration. TPF itself was not changed.
