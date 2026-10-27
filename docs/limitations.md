# Limitations and follow-on work

This is a local nostalgic engineering demo. Tev1's measured navigation is poor; finite-choice schema validity does not imply useful exploration. The nine cases are a small deterministic sample, one run per policy/case, and model output and timings may change across hardware, model revisions or providers. Replaying recorded actions is deterministic; repeating live inference is not guaranteed to be identical.

The perspective is a stylized view of the current room. An open doorway fades into unknown space; it does not render hidden neighboring topology. There is no emulator, numeric score, sound system, backward move, full-map reveal, network multiplayer or training system. The historical generator and progression beyond three levels are intentionally replaced.

The application has one in-memory local session and no authentication, persistence or recovery across restart. It binds to loopback. Do not use it as a public multi-user service. Model settings are trusted deployment inputs; the supplied host connection is for local Ollama and uses its placeholder bearer value. Hosted credentials are outside this demo.

Native replay captures execution identity and lifecycle. A forced outer cancellation can leave partial step lifecycles; retain and distinguish those from completed normal-run traces. The verifier is used on completed recording/evaluation runs, not to relabel interrupted runs as successful. Console logs may contain provider diagnostics and local paths; they are excluded from the publishable execution dataset.

A production successor would need an explicit multi-user/session model, durable storage and recovery, authentication and authorization, configured provider credentials, rate limits, telemetry retention, privacy review, dependency/release maintenance, CI and broader accessibility/device testing. None is necessary to demonstrate this local game, and none is implemented here.

MCP was deliberately omitted because no existing tool materially improved the playable demonstration. TPF itself was not changed.
