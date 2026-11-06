# Execution contract
One new game/reset or admitted human action starts one root. One AI/baseline Play/resume segment starts one root. All internal turns use the named `play` recursive Pipeline and the same `ApplyAction` reducer in that Pipeline. Pause, view reads and SSE subscriptions start no gameplay roots. Model and control/time observations are Queries; frame publication is a Command. No Await, Expansion, persistence aspect, database or external resume callback. Recursion and provider attempts are bounded; PipelineExecutionService is used only at the external host boundary. Native root identity, parentage, ordering and terminal lifecycle are acceptance criteria. Replay document count is diagnostic, as explicitly approved by the user.

## Released compiler shape

The application imports the TPF `26.11.1` BOM. Compiler, contracts and runtime are
selected by that BOM. Decision Query adapters and connector generation tooling are
explicitly pinned to release `26.10.1`, matching the BOM's connector release line;
these particular artifacts are not managed by the BOM.

The named `play` definition observes control/time, selects a typed turn, invokes
`apply-and-publish`, then uses `pipeline: self` for a continuing `RunState`.
The child applies the shared rules, publishes the frame and returns a `Continuation`.
Returning from the child executes the caller's self-call when appropriate. Base cases
complete against the declared output contracts; there is no identity terminal service.

Each concrete type has at most one ordinary consumer per definition. In `play`,
`RunState` selects the control Query and can also select the separate self-call
continuation. `ControlObservation` means the host's ownership, pause and time have
been observed for this turn. In `apply-and-publish`, `PublishedFrame` means the
Command has acknowledged publication, so continuation policy cannot run before it.
These boundary results carry trusted run state; neither comes from model output.

The earlier snapshot compiler required inlining the action reducer into `play`
because it could not generate that nested invocation shape. That workaround is
removed: the released compiler supports the ordinary child call inside the recursive
parent. The same generation/segment/revision Command identity protects publication.
