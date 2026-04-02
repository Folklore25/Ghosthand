---
phase: 22-patch-stabilization-for-1-2-1
plan: 01
subsystem: api
tags: [android, accessibility, screen, ocr, payloads, testing]
requires:
  - phase: 21-agent-interaction-hardening-for-1-2-0
    provides: source-aware `/screen` payloads, partial-output signaling, and operational-insufficiency judgment
provides:
  - bounded `retryHint` metadata on accessibility `/screen` payloads
  - explicit `ocr` versus `hybrid` next-step guidance without changing runtime behavior
  - regression coverage for empty, insufficient, and ordinary partial-output accessibility reads
affects: [phase-22-02, phase-22-03, screen-contract, local-api]
tech-stack:
  added: []
  patterns: [bounded accessibility retry hinting, source-aware screen payload metadata, TDD for payload regressions]
key-files:
  created: [.planning/phases/22-patch-stabilization-for-1-2-1/22-01-SUMMARY.md]
  modified:
    - app/src/main/java/com/folklore25/ghosthand/GhosthandApiPayloads.kt
    - app/src/main/java/com/folklore25/ghosthand/ScreenReadModels.kt
    - app/src/test/java/com/folklore25/ghosthand/GhosthandApiPayloadsTest.kt
    - app/src/test/java/com/folklore25/ghosthand/StateCoordinatorScreenPayloadTest.kt
key-decisions:
  - "Retry guidance is payload metadata only; `/screen` keeps its requested source semantics and never auto-runs OCR."
  - "Empty accessibility output points to `ocr`, while operationally insufficient accessibility output points to `hybrid`."
  - "`partialOutput=true` alone stays non-actionable unless the existing operational-insufficiency rule is also true."
patterns-established:
  - "Accessibility `/screen` hints must reuse the existing insufficiency judgment instead of introducing broader fallback heuristics."
  - "Retry hints expose explicit provenance through a compact `{source, reason}` shape."
requirements-completed: [PATCH-01, PATCH-02, PATCH-05, PATCH-06]
duration: 8min
completed: 2026-04-02
---

# Phase 22 Plan 01: OCR Retry Hinting Summary

**Accessibility `/screen` now emits bounded `retryHint` metadata that points empty reads to `ocr` and degraded reads to `hybrid` without changing source behavior.**

## Performance

- **Duration:** 8 min
- **Started:** 2026-04-02T11:42:00Z
- **Completed:** 2026-04-02T11:50:28Z
- **Tasks:** 1
- **Files modified:** 4

## Accomplishments
- Added a compact `retryHint` shape to `ScreenReadPayload` and serialized it into `/screen` payload fields.
- Reused the existing accessibility operational-insufficiency judgment so ordinary `partialOutput=true` cases stay silent.
- Added regression tests for empty accessibility output, insufficient accessibility output, and source-aware payload serialization.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add compact OCR fallback hint fields to accessibility-first screen payloads** - `fc8c7fa` (test), `19d62c7` (feat)

## Files Created/Modified
- `app/src/main/java/com/folklore25/ghosthand/GhosthandApiPayloads.kt` - Computes bounded accessibility retry hints and includes them in serialized `/screen` payloads.
- `app/src/main/java/com/folklore25/ghosthand/ScreenReadModels.kt` - Adds the compact `ScreenReadRetryHint` model to the screen-read payload contract.
- `app/src/test/java/com/folklore25/ghosthand/GhosthandApiPayloadsTest.kt` - Covers empty, insufficient, and ordinary partial accessibility `/screen` outcomes.
- `app/src/test/java/com/folklore25/ghosthand/StateCoordinatorScreenPayloadTest.kt` - Verifies `retryHint` serialization alongside existing source-aware screen payload fields.

## Decisions Made
- Empty accessibility `/screen` output should recommend `ocr` because there is no accessibility signal to preserve.
- Accessibility output that is still present but operationally insufficient should recommend `hybrid` so callers retain the surviving accessibility signal.
- The new hint must remain optional payload metadata and not be mirrored as behavior changes in `StateCoordinator`.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- An initial targeted test run surfaced unrelated unit-test compilation noise before the requested payload assertions executed. The final required verification run completed successfully without expanding the implementation scope.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 22-02 can build on the new screen retry metadata without revisiting `/screen` source semantics.
- No blocker remains for the stale-node failure classification work planned next in Phase 22.

## Self-Check

PASSED
- Found `.planning/phases/22-patch-stabilization-for-1-2-1/22-01-SUMMARY.md`
- Verified task commits `fc8c7fa` and `19d62c7` in git history

---
*Phase: 22-patch-stabilization-for-1-2-1*
*Completed: 2026-04-02*
