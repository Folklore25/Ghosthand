---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
last_updated: "2026-04-04T10:05:10.470Z"
progress:
  total_phases: 10
  completed_phases: 6
  total_plans: 45
  completed_plans: 35
---

# STATE.md — Ghosthand Project

## Project Reference

Ghosthand — Android accessibility-automation server + premium operator UI.
Accessibility-first device automation on the local app path only.
Current focus: **1.4.0 agent-native interface evolution** — Phase 25 is now planned as a bounded layered-interface release that keeps HTTP/JSON stable while adding an observation plane, strengthening the capability plane, normalizing action evidence, and only allowing a thin intent layer if it earns inclusion.

## Current Position

Phase: 25 (agent-native-interface-evolution-for-1-4-0) — EXECUTING
Plan: 3 of 5

- **Phase:** 25
- **Implementation baseline:** committed — Android app and Gradle project tracked in git
- **Verified route set:** `/ping`, `/screen`, `/tree`, `/info`, `/focused`, `/find`, `/tap`, `/click`, `/input`, `/setText`, `/scroll`, `/swipe`, `/longpress`, `/gesture`, `/back`, `/home`, `/recents`, `/screenshot`, `/notify`, `/wait`, `/clipboard`, `/commands`
- **Status:** Executing Phase 25

## Progress

- Phase 20: **PLANNED FOR V1.1.0** — semantic reachability, OCR fallback, and `/wait` outcome separation
- Phase 21: **PLANNED FOR V1.2.0** — interaction semantics, action confidence, selector failure disclosure, and partial-output legibility
- Phase 22: **PLANNED FOR V1.2.1** — OCR fallback discoverability, stale-node classification, and modal-transition contract clarification
- Phase 23: **PLANNED FOR V1.3.0** — post-action state legibility, `/screen` summary mode, render/readability signals, and lightweight visual preview access
- Phase 24: **PLANNED FOR V1.3.1** — maintainability convergence, state or contract layering cleanup, vocabulary normalization, test ownership cleanup, and future 2.0 seam preparation
- Phase 24.1: **PLANNED FOR V1.3.1 FIX PASS** — strict follow-up for LocalApiServer and StateCoordinator thinning, real package/domain decomposition, real layer convergence, real test ownership convergence, and stronger execution/observation seams
- Phase 24.2: **PLANNED FOR V1.3.1 FIX PASS 02** — strict follow-up for the still-large coordinator, new handler/payload mini-monoliths, and missing canonical ownership of render/readability/state-legibility concepts
- Phase 24.3: **PLANNED FOR V1.3.1 FIX PASS 03** — strict final follow-up for the last remaining blocker: a still-too-large StateCoordinator
- Phase 24.4: **PLANNED FOR V1.3.1 CLEAN CLOSEOUT** — preview cleanup, field/hint convergence, bounded code cleanup, and final contract alignment for the 1.3.x line
- Phase 25: **PLANNED FOR V1.4.0** — agent-native interface evolution through observation plane v1, capability-plane strengthening, action-evidence normalization, and only a very thin intent layer if it is justified after A/B/C stabilize

Overall: Ghosthand’s next mainline move is not route proliferation. Phase 25 exists to make the substrate easier for agents to reason about with less polling, lower ambiguity, and stronger machine-readable self-description.

## Recent Decisions

| Decision | Rationale |
|---|---|
| 1.4.0 will evolve Ghosthand through layered exposure, not endpoint accumulation | The product goal is a clearer substrate with less request stitching, not more random routes |
| Observation plane v1 will be pollable and cursor-based over HTTP/JSON | This keeps the event layer debuggable and bounded without prematurely adopting streaming complexity |
| Thin intent helpers are gated behind A/B/C and may be explicitly deferred | The primitive, capability, observation, and evidence planes remain primary unless a helper clearly reduces repeated low-value reasoning |

## Decisions

- Runtime owners for server, state, payload, catalog, wait, and execution now live in domain packages instead of the flat root runtime namespace.
- `LocalApiServer` and `StateCoordinator` remain thin shells that import explicit route, execution, observation, and payload collaborators without adding root-backed placeholders.
- [Phase 24.1]: Runtime-domain tests now follow behavior ownership even when some non-runtime UI tests remain in the flat root package.
- [Phase 24.1]: Because docs/API.md is absent in this workspace, contract alignment stays bounded to the command catalog rather than introducing new documentation.
- [Phase 24.2]: Phase 24.2 now executes against a binding architecture-fix note that maps each remaining review failure to a required ownership move.
- [Phase 24.2]: StateCoordinator now delegates health, find, read, preview, and execution composition to domain collaborators.
- [Phase 24.2]: Screenshot fallback selection stays in an explicit non-root execution seam instead of living in StateCoordinator.
- [Phase 24.3]: The final follow-up is now bound to a strict plan split: 24.3-02 only moves state/read/preview ownership, 24.3-03 only moves execution and utility ownership, and 24.3-04 only preserves the already-passing areas and verifies them.
- [Phase 24.3]: Plan 24.3-02 moved state, tree/find, and preview read composition behind StateReadCoordinator, ScreenSnapshotCoordinator, ScreenFindCoordinator, and ScreenPreviewCoordinator.
- [Phase 24.3]: StateCoordinator remains a route-facing facade, but read-facing ownership now lives in bounded collaborators so plan 24.3-03 can focus only on execution and utility thinning.
- [Phase 24.4]: Preview metadata now publishes previewPath on /screen and routes retrieval through /screenshot instead of opaque preview tokens.
- [Phase 24.4]: Inline preview thumbnail embedding and includePreview opt-in were removed so advertised preview access stays lightweight and truthful.
- [Phase 24.4]: The /screen fallback contract now emits only suggestedSource and fallbackReason when a recommendation exists.
- [Phase 24.4]: Runtime owners now import their direct payload family instead of depending on GhosthandApiPayloads at runtime.
- [Phase 24.4]: Screen fallback payload helpers no longer carry the dead includeRetryHint closeout parameter.
- [Phase 24.4]: Publish /commands from a catalog-owned serializer so runtime and tests share one closeout surface.
- [Phase 24.4]: Keep explicit interaction defaults only where they communicate action semantics; drop null, empty, and not-applicable command metadata elsewhere.
- [Phase 24.4]: Because docs/API.md is absent locally, finalize closeout contract alignment in the runtime/catalog only instead of creating new docs.
- [Phase 25]: Keep HTTP/JSON as the stable control base and add a bounded pollable observation plane rather than replacing the current API model.
- [Phase 25]: Strengthen the existing capability plane through `/commands` and runtime state surfaces instead of creating a separate self-description system.
- [Phase 25]: Treat thin intent helpers as conditional scope that is only allowed after observation, capability, and evidence planes stabilize and only if the helper remains generic and inspectable.
- [Phase 25]: Observation plane v1 now uses a bounded in-memory cursor log exposed through `/events`, with `/screen` publishing edge-triggered foreground, readability, fallback, preview, and transient accessibility events instead of a noisy firehose.
- [Phase 25]: Capability-plane metadata now publishes `plane`, `availabilityModel`, `truthType`, `directness`, `preconditions`, and `failureModes` through `/commands`, while runtime capability summaries expose current blockers instead of only raw allowed/usable flags.
- [Phase 25]: Major action routes now project one evidence family around `performed`, execution path/backend, observed change, compact post-action state, and bounded observation-shift hints instead of route-local payload conventions.

## Blockers / Concerns

- `.planning/` and `docs/` are now local-only and intentionally untracked, so planning truth is local workspace state rather than repo-shared state.
- `docs/API.md` is absent in this workspace, so any final contract alignment must stay bounded to the command catalog/runtime surface rather than turning into a docs project.
- OpenClaw and other external agent validation remain user-driven rather than directly invokable from this workspace.

## Session Continuity

Last session: 2026-04-04T00:00:00.000Z
Next action: execute Phase 25-04 and make the thin-intent decision explicitly; defer helpers unless a very small generic helper clearly earns inclusion.

## Performance Metrics

| Phase | Plan | Duration | Tasks | Files |
|---|---:|---:|---:|---:|
| 24.1 | 05 | 13m | 1 | 45 |
| 24.1 | 06 | 11m | 2 | 13 |
| 24.2 | 01 | 4m | 1 | 4 |
| 24.2 | 02 | 8 min | 1 | 11 |
| 24.3 | 01 | 0 min | 1 | 4 |
| 24.3 | 02 | 4m | 1 | 8 |
| 24.4 | 01 | 9 min | 1 | 14 |
| 24.4 | 02 | 3 min | 1 | 6 |
| 24.4 | 03 | 3 min | 1 | 13 |
| 24.4 | 04 | 1 min | 2 | 3 |
| 25 | 01 | 18 min | 1 | 11 |
| 25 | 02 | 12 min | 1 | 8 |
| 25 | 03 | 19 min | 1 | 13 |
