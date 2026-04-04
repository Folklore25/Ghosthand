---
phase: 26-visual-observation-regression-correction-for-1-4-1
plan: 01
subsystem: testing
tags: [android, screenshot, mediaprojection, accessibility, regression, testing]
requires:
  - phase: 25.6
    provides: accepted 1.4.0 baseline and current screenshot pipeline ownership
provides:
  - diagnosis artifact for the screenshot truth regression
  - failing route and backend regression tests for empty-image success
  - recorded red-test evidence for the next corrective plan
affects: [26-02, screenshot, preview, observation]
tech-stack:
  added: []
  patterns: [diagnosis-first regression lock-in, source-contract assertions for route and projection truth]
key-files:
  created:
    - .planning/phases/26-visual-observation-regression-correction-for-1-4-1/26-01-DIAGNOSIS.md
    - .planning/phases/26-visual-observation-regression-correction-for-1-4-1/26-01-SUMMARY.md
    - app/src/test/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlersTest.kt
    - app/src/test/java/com/folklore25/ghosthand/interaction/execution/ScreenshotDispatchResultTruthTest.kt
    - app/src/test/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProviderTest.kt
  modified:
    - .planning/phases/26-visual-observation-regression-correction-for-1-4-1/26-01-DIAGNOSIS.md
key-decisions:
  - "Keep 26-01 diagnosis-only: add no runtime screenshot fixes, only lock the broken truth boundary."
  - "Use one actual backend-selection regression test plus source-level route and projection guards to keep the red suite narrow and stable."
patterns-established:
  - "A screenshot backend is not trustworthy just because available=true; usable bytes and positive dimensions must be proven."
  - "Projection capture must prove frame acquisition before reporting screenshot success or failure semantics."
requirements-completed: [VIS-03, VIS-05, VIS-06]
duration: 5min
completed: 2026-04-04
---

# Phase 26 Plan 01: Visual Observation Regression Correction For 1.4.1 Summary

**Diagnosis artifact plus intentionally failing screenshot truth tests that pin empty-image success, invalid backend selection, and projection first-frame weakness before any runtime fix lands**

## Performance

- **Duration:** 5 min
- **Started:** 2026-04-04T18:08:55Z
- **Completed:** 2026-04-04T18:13:35Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Wrote a bounded diagnosis tracing `/screenshot` from route serialization through screenshot backend selection and both capture backends.
- Added red regression coverage for route-level empty-image success and backend selection that accepts unusable accessibility results.
- Added projection-path source checks and recorded the exact intentional failures in the diagnosis artifact for Plan 26-02.

## Task Commits

Each task was committed atomically, with Task 2 split across multiple red-test commits because git index-lock contention interrupted the first staging attempt:

1. **Task 1: Write the failing-path diagnosis from route contract through both capture backends** - `5eb6131` (`feat`)
2. **Task 2: Add failing regression tests that lock the broken screenshot truth boundary** - `cedd496`, `f40b752`, `ff06ba6` (`test`)

## Files Created/Modified
- `.planning/phases/26-visual-observation-regression-correction-for-1-4-1/26-01-DIAGNOSIS.md` - exact broken path, failure hypotheses, and recorded red-test failures
- `app/src/test/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlersTest.kt` - route contract guards for available-only success and blank base64 serialization
- `app/src/test/java/com/folklore25/ghosthand/interaction/execution/ScreenshotDispatchResultTruthTest.kt` - backend-selection tests proving unusable accessibility results still block usable projection captures
- `app/src/test/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProviderTest.kt` - source-level guards proving projection still lacks frame-wait and byte-validation hardening

## Decisions Made
- Keep the plan diagnosis-only even though the new tests expose obvious fix paths; runtime behavior stays untouched in 26-01.
- Treat the route contract and screenshot selection seam as the primary truth boundary, not just backend-specific failure cases.

## Deviations from Plan

None - plan scope stayed diagnosis-only and did not introduce runtime screenshot fixes.

## Issues Encountered

- Repeated `.git/index.lock` contention during staging caused Task 2’s red tests to land in multiple small commits instead of a single test commit. The resulting committed content still matches the plan and no runtime files were touched.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 26-02 can now implement the screenshot fix against a concrete diagnosis and a reproducible red suite.
- The red suite is intentionally failing; those failures are screenshot-specific and already documented in the diagnosis artifact.

## Self-Check: PASSED

- Found `.planning/phases/26-visual-observation-regression-correction-for-1-4-1/26-01-DIAGNOSIS.md`
- Found `.planning/phases/26-visual-observation-regression-correction-for-1-4-1/26-01-SUMMARY.md`
- Found commit `5eb6131`
- Found commit `cedd496`
- Found commit `f40b752`
- Found commit `ff06ba6`
