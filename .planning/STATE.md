---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
last_updated: "2026-04-03T19:28:12.259Z"
progress:
  total_phases: 9
  completed_phases: 5
  total_plans: 40
  completed_plans: 31
---

# STATE.md — Ghosthand Project

## Project Reference

Ghosthand — Android accessibility-automation server + premium operator UI.
Accessibility-first device automation on the local app path only.
Current focus: **1.3.1 clean closeout** — Phase 24.4 is now planned as a bounded closeout pass to finish the 1.3.x line cleanly without feature expansion or 1.4.0 work.

## Current Position

Phase: 24.4 (1-3-1-clean-closeout) — EXECUTING
Plan: 4 of 4

- **Phase:** 24.4
- **Implementation baseline:** committed — Android app and Gradle project tracked in git
- **Verified route set:** `/ping`, `/screen`, `/tree`, `/info`, `/focused`, `/find`, `/tap`, `/click`, `/input`, `/setText`, `/scroll`, `/swipe`, `/longpress`, `/gesture`, `/back`, `/home`, `/recents`, `/screenshot`, `/notify`, `/wait`, `/clipboard`, `/commands`
- **Status:** Ready to execute

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

Overall: Ghosthand’s 1.3.x line now needs a bounded clean closeout so preview, hinting, and final contract surfaces become cleaner and less noisy before 1.4.0 starts.

## Recent Decisions

| Decision | Rationale |
|---|---|
| 1.3.x now needs a clean closeout instead of another feature wave | The remaining work is cleanup and contract sharpening, not capability expansion |
| Preview should not remain an advertised-but-hollow mechanism | If preview exists in schema, the retrieval path must be clear and actually usable |
| Hint fields should help, not accumulate into background noise | The closeout should reduce overlap and clutter rather than rename it |

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

## Blockers / Concerns

- `.planning/` and `docs/` are now local-only and intentionally untracked, so planning truth is local workspace state rather than repo-shared state.
- `docs/API.md` is absent in this workspace, so any final contract alignment must stay bounded to the command catalog/runtime surface rather than turning into a docs project.
- OpenClaw and other external agent validation remain user-driven rather than directly invokable from this workspace.

## Session Continuity

Last session: 2026-04-03T19:28:12.257Z
Next action: execute Phase 24.4 plan 04 for final closeout alignment.

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
