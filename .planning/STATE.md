---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
last_updated: "2026-04-03T16:22:10.236Z"
progress:
  total_phases: 7
  completed_phases: 3
  total_plans: 32
  completed_plans: 20
---

# STATE.md — Ghosthand Project

## Project Reference

Ghosthand — Android accessibility-automation server + premium operator UI.
Accessibility-first device automation on the local app path only.
Current focus: **1.3.1 maintainability convergence fix 02** — Phase 24.2 is now planned as a strict second corrective pass to resolve the still-open coordinator, monolith, and canonical-ownership failures without broadening scope or adding features.

## Current Position

Phase: 24.2 (maintainability-convergence-fix-02) — EXECUTING
Plan: 2 of 5

- **Phase:** 24.2
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

Overall: Ghosthand has made meaningful structural progress, but strict review still rejects the maintainability convergence target until the remaining coordinator size, replacement-monolith, and canonical-ownership problems are actually removed.

## Recent Decisions

| Decision | Rationale |
|---|---|
| The second strict follow-up exists because 24.1 still did not satisfy maintainability convergence review | Structural progress is not enough if the remaining change points are still oversized and drift-prone |
| Phase 24.2 now has a binding architecture-fix note before more code movement | The corrective pass needs an explicit acceptance gate that maps every remaining review failure to required ownership moves |
| `StateCoordinator` is still too central at roughly 867 lines | The coordinator still mixes too many unrelated screen/state/input/preview responsibilities |
| New large handler/payload files replaced the old monoliths | `ActionRouteHandlers`, `ReadRouteHandlers`, and `GhosthandPayloadSupport` are still too large to count as converged ownership |
| Canonical ownership of render/readability/state-legibility concepts is still missing | Review explicitly called out duplicated derivation paths as a remaining failure |

## Decisions

- Runtime owners for server, state, payload, catalog, wait, and execution now live in domain packages instead of the flat root runtime namespace.
- `LocalApiServer` and `StateCoordinator` remain thin shells that import explicit route, execution, observation, and payload collaborators without adding root-backed placeholders.
- [Phase 24.1]: Runtime-domain tests now follow behavior ownership even when some non-runtime UI tests remain in the flat root package.
- [Phase 24.1]: Because docs/API.md is absent in this workspace, contract alignment stays bounded to the command catalog rather than introducing new documentation.
- [Phase 24.2]: Phase 24.2 now executes against a binding architecture-fix note that maps each remaining review failure to a required ownership move.

## Blockers / Concerns

- `.planning/` and `docs/` are now local-only and intentionally untracked, so planning truth is local workspace state rather than repo-shared state.
- Remaining strict-review failures are concentrated in `state/StateCoordinator.kt` (867 lines), `routes/action/ActionRouteHandlers.kt` (657), `payload/GhosthandPayloadSupport.kt` (543), and `routes/read/ReadRouteHandlers.kt` (468).
- OpenClaw and other external agent validation remain user-driven rather than directly invokable from this workspace.

## Session Continuity

Last session: 2026-04-03T16:22:10.230Z
Next action: execute Phase 24.2-02 using the binding architecture-fix note to thin `StateCoordinator`, then reduce the new mini-monoliths and establish canonical ownership of render/readability/state-legibility concepts.

## Performance Metrics

| Phase | Plan | Duration | Tasks | Files |
|---|---:|---:|---:|---:|
| 24.1 | 05 | 13m | 1 | 45 |
| 24.1 | 06 | 11m | 2 | 13 |
| 24.2 | 01 | 4m | 1 | 4 |
