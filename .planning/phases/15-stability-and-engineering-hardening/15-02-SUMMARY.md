---
phase: 15-stability-and-engineering-hardening
plan: 02
subsystem: api
tags: [android, loopback-api, http, concurrency, testing]
requires:
  - phase: 15-01
    provides: backup-policy cleanup and engineering-hygiene baseline
provides:
  - bounded LocalApiServer worker execution with explicit queue and saturation handling
  - malformed-request and oversized-body rejection with bounded parsing
  - shutdown waits for sockets and executors to release before service teardown finishes
affects: [15-03, docs/API.md, runtime-stability]
tech-stack:
  added: []
  patterns: [bounded executor plus request protocol helper, shutdown resource registry with await termination]
key-files:
  created: [app/src/test/java/com/folklore25/ghosthand/LocalApiServerRequestParsingTest.kt]
  modified:
    - app/src/main/java/com/folklore25/ghosthand/LocalApiServer.kt
    - app/src/main/java/com/folklore25/ghosthand/GhosthandHttp.kt
    - app/src/main/java/com/folklore25/ghosthand/GhosthandForegroundService.kt
    - app/src/test/java/com/folklore25/ghosthand/GhosthandHttpTest.kt
key-decisions:
  - "Use a fixed-size queue-backed client executor so overload is explicit and rejectable instead of silently unbounded."
  - "Map malformed parsing cases to safe HTTP errors (400, 408, 413, 431, 503) without changing endpoint semantics."
  - "Wait briefly for executor termination during stop so service teardown reflects real shutdown, not just socket close intent."
patterns-established:
  - "Loopback request parsing should go through a pure protocol helper so malformed-input behavior is JVM-testable."
  - "Local server shutdown should track active clients explicitly and await executor termination from lifecycle teardown."
requirements-completed: [STAB-02, STAB-05]
duration: 3 min
completed: 2026-03-29
---

# Phase 15 Plan 02: Local API Hardening Summary

**Bounded LocalApiServer execution with strict HTTP request limits, malformed-input rejection, and teardown that waits for sockets and executors to stop**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-29T18:11:00Z
- **Completed:** 2026-03-29T18:13:58Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Replaced the unbounded cached client pool with a fixed-size worker pool and bounded queue, including explicit 503 saturation handling.
- Added bounded HTTP parsing for request lines, headers, and bodies, with defensive `Content-Length` validation and client read timeouts.
- Made loopback shutdown track active clients, wait for executor termination, and keep foreground-service teardown resilient if shutdown throws.

## Task Commits

Each task was committed atomically:

1. **Task 1: Bound LocalApiServer resource usage and request size** - `256ce44` (test), `5ac5f68` (feat)
2. **Task 2: Guarantee shutdown cleanup for sockets, clients, and executors** - `1abcc8c` (fix)

## Files Created/Modified
- `app/src/main/java/com/folklore25/ghosthand/LocalApiServer.kt` - Added bounded executor/resource management, protocol-driven parsing, timeout handling, and awaited shutdown.
- `app/src/main/java/com/folklore25/ghosthand/GhosthandHttp.kt` - Added bounded line/body reads and explicit malformed `Content-Length` handling.
- `app/src/main/java/com/folklore25/ghosthand/GhosthandForegroundService.kt` - Hardened service teardown so registry/state cleanup still happens if server stop fails.
- `app/src/test/java/com/folklore25/ghosthand/LocalApiServerRequestParsingTest.kt` - Added JVM tests for malformed headers, oversized bodies, and shutdown cleanup hooks.
- `app/src/test/java/com/folklore25/ghosthand/GhosthandHttpTest.kt` - Added invalid `Content-Length` and truncated body coverage.

## Decisions Made

- Fixed client concurrency at `4` workers with a `16`-request queue to bound local overload instead of expanding threads indefinitely.
- Rejected malformed input at the protocol layer before route dispatch so existing endpoints keep their semantics while bad requests fail early.
- Kept shutdown waiting inside `LocalApiServer.stop()` rather than the service, so the server owns its own release guarantees.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- A duplicate local-server helper block in `GhosthandHttp.kt` conflicted with the new protocol seam during the first compile pass; it was removed before final verification and did not change the planned behavior.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 15-03 can build on a bounded and test-covered loopback server baseline.
- No blocking issues remain from this plan after `:app:testDebugUnitTest` and `:app:assembleDebug` passed.

## Self-Check: PASSED

---
*Phase: 15-stability-and-engineering-hardening*
*Completed: 2026-03-29*
