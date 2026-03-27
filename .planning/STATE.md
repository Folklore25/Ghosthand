# STATE.md — Ghosthand Project

## Project Reference

Ghosthand — Android accessibility-automation server + premium operator UI.
Accessibility-first device automation; root remains optional and secondary.
Current focus: **capability-parity** — close the gap between Ghosthand's current endpoint surface and the reference implementation's core capability set.

## Current Position

- **Phase:** 09 of 09 — Mainline capability-parity implementation
- **Stage 1:** Endpoint surface implemented — `/screen`, `/info`, `/focused`, `/click`, `/input`, `/setText`
- **Stage 2:** Endpoint surface implemented — `/scroll`, `/longpress`, `/gesture`, `/back`, `/home`, `/recents`
- **Stage 3:** Endpoint surface implemented — `/screenshot`, `/notify`, `/wait`, `/clipboard`
- **Status:** Endpoint surface implemented and build-verified; runtime acceptance incomplete

## Progress

- Phase 09 planning: **COMPLETE**
- Stage 1 implementation: **DONE** — route surface implemented, runtime acceptance still open
- Stage 2 implementation: **DONE** — route surface implemented, runtime acceptance still open
- Stage 3 implementation: **DONE** — route surface implemented, runtime acceptance still open

Overall: endpoint surface implemented across all 16 planned parity endpoints; real-device acceptance remains open

## Recent Decisions

| Decision | Rationale |
|---|---|
| Ghosthand stays accessibility-first; root optional/secondary | User reset architecture, no separate root direction |
| Stage 1 first, not Stage 2 or 3 | Maximum code reuse, no new permissions, lowest risk |
| Port/adapt proven external patterns | Don't re-derive already solved mechanics |
| Planning-only before coding | User explicitly requested staged plan first |
| 16 endpoints across 3 stages | Matches the reference implementation's remaining parity gap cleanly |
| `/notify` uses `NotificationManager` post/cancel, not notification interception | No extra listener access required for the current capability set |
| Full-screen `/screenshot` uses MediaProjection consent from the app UI | Keeps screenshot permission explicit and compatible with app-process ownership |

## Blockers / Concerns

- Runtime acceptance remains blocked by real-device defects:
  - `/tree` can return stale/cached snapshots
  - `nodeId` values drift between reads, making cached IDs unsafe
  - `/tap` is only reliable when driven by fresh `/find` nodeIds
  - `/screenshot` still hits `INTERNAL_ERROR` on device
  - `/find(text)` can hang on device
- Working tree still contains the uncommitted Android project implementation and closeout docs updates

## Session Continuity

Last session: 2026-03-28 — Verified the Android build and synced docs/planning to the implemented endpoint surface, while keeping runtime acceptance open.
Next action: commit the docs/planning sync only, then debug the remaining real-device parity blockers before any final closeout

## Context Notes

- `LocalApiServer` now handles the full capability-parity surface, including `/screen`, `/info`, `/focused`, `/click`, `/input`, `/setText`, `/scroll`, `/longpress`, `/gesture`, `/back`, `/home`, `/recents`, `/screenshot`, `/notify`, `/wait`, and `/clipboard`
- `GhostCoreAccessibilityService` exposes tap, swipe, node click, set-text, screenshot, long-press, gesture, and global-action dispatch paths on the stable accessibility execution core
- `AccessibilityTreeSnapshotProvider` already provides package/activity + flat nodes
- `docs/API.md` now reflects the implemented parity endpoints and their current request/response shapes
- Build verification passed, but runtime parity acceptance is still blocked by state-sync/observability defects on the target device
- Full plan: `.planning/phases/09-capability-parity-mainline/CAPABILITY-PARITY-PLAN.md`
