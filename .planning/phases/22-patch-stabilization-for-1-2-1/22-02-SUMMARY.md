---
phase: 22-patch-stabilization-for-1-2-1
plan: 02
subsystem: api
tags: [android, accessibility, local-api, click, nodeid, testing]
requires:
  - phase: 21-interaction-and-contract-hardening-for-1-2-0
    provides: /click failure shaping, selector miss semantics, and snapshot-scoped node identity
provides:
  - Distinct `/click` stale node reference classification for expired nodeId requests
  - Regression coverage for stale nodeId versus ordinary `/click` miss handling
affects: [22-03, commands, docs, local-api]
tech-stack:
  added: []
  patterns: [Keep stale snapshot classification as a narrow `/click` contract change keyed off same-snapshot nodeId truth]
key-files:
  created: [.planning/phases/22-patch-stabilization-for-1-2-1/22-02-SUMMARY.md]
  modified: [app/src/main/java/com/folklore25/ghosthand/LocalApiServer.kt, app/src/test/java/com/folklore25/ghosthand/LocalApiServerDisclosureTest.kt]
key-decisions:
  - "Return `STALE_NODE_REFERENCE` only for nodeId `/click` failures whose attempted path is `stale_snapshot`."
  - "Keep ordinary nodeId misses and selector misses on `/click` mapped to `NODE_NOT_FOUND` until Plan 22-03 aligns wider docs and catalogs."
patterns-established:
  - "Use attemptedPath-specific mapping in LocalApiServer when the underlying execution layer already exposes the narrow stale/miss distinction."
requirements-completed: [PATCH-03, PATCH-05, PATCH-06]
duration: 5 min
completed: 2026-04-02
---

# Phase 22 Plan 02: Stale Click Classification Summary

**`/click` now distinguishes expired snapshot-scoped nodeId references from ordinary misses without widening node identity guarantees beyond the current same-snapshot contract.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-04-02T11:44:24Z
- **Completed:** 2026-04-02T11:49:24Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments
- Added a distinct `STALE_NODE_REFERENCE` `/click` error code and message for nodeId failures caused by `stale_snapshot`.
- Preserved existing `NODE_NOT_FOUND` semantics for selector misses and ordinary nodeId misses.
- Added regression coverage for the stale-vs-ordinary miss mapping in `LocalApiServerDisclosureTest`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Classify stale node references distinctly in `/click` nodeId failures** - `f2362b3` (test)
2. **Task 1: Classify stale node references distinctly in `/click` nodeId failures** - `17d1872` (feat)

_Note: This plan used TDD for the single task, so the task produced a RED commit and a GREEN commit._

## Files Created/Modified
- `app/src/main/java/com/folklore25/ghosthand/LocalApiServer.kt` - maps stale nodeId `/click` misses to a dedicated public error code/message and adds a same-snapshot disclosure.
- `app/src/test/java/com/folklore25/ghosthand/LocalApiServerDisclosureTest.kt` - verifies stale nodeId failures classify separately from ordinary misses.
- `.planning/phases/22-patch-stabilization-for-1-2-1/22-02-SUMMARY.md` - records plan execution, decisions, and verification.

## Decisions Made
- Used the existing `attemptedPath == "stale_snapshot"` signal to classify stale nodeId `/click` failures, avoiding any broader node identity redesign.
- Kept the change in `LocalApiServer` so the patch stays scoped to the public `/click` contract instead of expanding cross-route execution semantics.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed test-local stale mapping shadowing**
- **Found during:** Task 1 (Classify stale node references distinctly in `/click` nodeId failures)
- **Issue:** The regression file briefly carried local helper logic that would have reimplemented the stale classification inside the test instead of exercising the production `LocalApiServer` mapping.
- **Fix:** Removed the test-local helper shadowing and re-ran the required verification against the production helper path.
- **Files modified:** `app/src/test/java/com/folklore25/ghosthand/LocalApiServerDisclosureTest.kt`
- **Verification:** `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.LocalApiServerDisclosureTest :app:assembleDebug`
- **Committed in:** `17d1872`

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** The auto-fix kept the regression honest and did not expand scope beyond the planned `/click` stale-reference classification.

## Issues Encountered
- None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Plan 22-03 can align `/commands` and docs with the new stale `/click` classification.
- No blockers remain for the stale-reference patch itself.

## Self-Check: PASSED

- Found `.planning/phases/22-patch-stabilization-for-1-2-1/22-02-SUMMARY.md`
- Found commit `f2362b3`
- Found commit `17d1872`
