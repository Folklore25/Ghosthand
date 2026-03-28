# STATE.md — Ghosthand Project

## Project Reference

Ghosthand — Android accessibility-automation server + premium operator UI.
Accessibility-first device automation; root remains optional and secondary.
Current focus: **Phase 09 runtime-acceptance consolidation** — normalize the repo, refresh planning, and record deliberate acceptance for the verified local-agent route set.

## Current Position

- **Phase:** 09 of 09 — Mainline capability-parity runtime-acceptance consolidation
- **Implementation baseline:** committed — Android app and Gradle project now tracked in git
- **Verified route set:** `/ping`, `/screen`, `/tree`, `/info`, `/focused`, `/find`, `/tap`, `/click`, `/input`, `/setText`, `/scroll`, `/swipe`, `/longpress`, `/gesture`, `/back`, `/home`, `/recents`, `/screenshot`, `/notify`, `/wait`, `/clipboard`, `/commands`
- **Status:** Route surface implemented, build-verified, and substantially verified on the target device; planning refresh and acceptance reporting in progress

## Progress

- Phase 09 implementation: **DONE** — route surface committed as the tracked Android baseline
- Runtime acceptance pass: **DONE FOR VERIFIED ROUTES** — device-shell verification completed for the current local-agent route set
- Consolidation/reporting: **IN PROGRESS** — planning files and acceptance record being aligned with the real runtime state

Overall: capability-parity route surface is implemented and the verified route set is working on-device; regression hardening and machine-readable contract strengthening are the next mainline steps

## Recent Decisions

| Decision | Rationale |
|---|---|
| Ghosthand stays accessibility-first; root optional/secondary | User reset architecture, no separate root direction |
| Stage 1 first, not Stage 2 or 3 | Maximum code reuse, no new permissions, lowest risk |
| Port/adapt proven external patterns | Don't re-derive already solved mechanics |
| Planning-only before coding | User explicitly requested staged plan first |
| 16 endpoints across 3 stages | Matches the reference implementation's remaining parity gap cleanly |
| `/notify` uses `NotificationManager` post/cancel, not notification interception | No extra listener access required for the current capability set |
| `GET /commands` is part of the runtime surface for local agents | Agents on-device need discoverable route/method/param metadata from Ghosthand itself |
| Screenshot baseline uses accessibility screenshot capability when available | Manual MediaProjection consent is not acceptable as the default baseline for the settled local-agent path |
| Device-shell orchestration is the trustworthy acceptance method | Host-side timing introduced false negatives during route verification |

## Blockers / Concerns

- Main remaining risks are consolidation risks, not route-surface risks:
  - planning artifacts lagged behind the real runtime state and need refresh
  - regression protection is still too light for the now-verified route contract
  - repeated APK reinstalls on this ROM can require accessibility rebind through secure settings during testing
- Working tree still contains docs/planning sync changes and the one-shot handoff deletion

## Session Continuity

Last session: 2026-03-28 — committed the Android implementation baseline and completed a broad device-shell acceptance pass of the local-agent route set.
Next action: commit the docs/planning refresh and runtime acceptance summary as a separate sync commit

## Context Notes

- `LocalApiServer` now serves the full local-agent route surface plus `GET /commands`
- `GhostCoreAccessibilityService` is the primary stable execution core; screenshot now works through declared accessibility screenshot capability on the device
- `AccessibilityTreeSnapshotProvider`, `AccessibilityNodeLocator`, and `AccessibilityNodeFinder` now provide fresh snapshots, snapshot-scoped node identity, and action-ready selector results
- Acceptance evidence is recorded in `.planning/phases/09-capability-parity-mainline/RUNTIME-ACCEPTANCE-SUMMARY.md`
- Full planning lineage:
  - `.planning/phases/09-capability-parity-mainline/CAPABILITY-PARITY-PLAN.md`
  - `.planning/phases/09-capability-parity-mainline/RUNTIME-ACCEPTANCE-PLAN.md`
