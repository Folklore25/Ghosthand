---
phase: 14-release-1-0-polish-and-update-architecture
plan: 01
subsystem: ui
tags: [android, github-release, update-state, livedata, xml]
requires:
  - phase: 13
    provides: accepted home surface and runtime-state model for the local app path
provides:
  - real GitHub latest-release fetch and parse contracts
  - version comparison mapped into truthful update UI state
  - home update card that only hands off to GitHub when a release URL exists
affects: [phase-14-plan-02, copy-cleanup, explanation-surface]
tech-stack:
  added: [none]
  patterns: [bounded release repository, viewmodel-level update state merge, github handoff only]
key-files:
  created:
    - app/src/main/java/com/folklore25/ghosthand/GitHubReleaseInfo.kt
    - app/src/main/java/com/folklore25/ghosthand/GitHubReleaseRepository.kt
    - app/src/main/java/com/folklore25/ghosthand/UpdateUiState.kt
  modified:
    - app/src/test/java/com/folklore25/ghosthand/UpdateUiStateMapperTest.kt
    - app/src/main/java/com/folklore25/ghosthand/MainActivity.kt
    - app/src/main/res/layout/activity_main.xml
key-decisions:
  - "Kept release metadata outside RuntimeStateStore and merged it at the home-screen state layer."
  - "Used GitHub latest-release metadata as a truth source and limited the CTA to an external GitHub handoff."
patterns-established:
  - "Release checks are modeled as explicit checking/up-to-date/update-available/failed states."
  - "Home-surface CTAs render only when the derived UI state exposes a destination URL."
requirements-completed: [POL-01, POL-02]
duration: 7min
completed: 2026-03-29
---

# Phase 14 Plan 01: Release Update Architecture Summary

**GitHub latest-release metadata now drives truthful home update state and a bounded GitHub APK handoff for Ghosthand 1.0.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-29T17:07:49Z
- **Completed:** 2026-03-29T17:14:30Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Added explicit GitHub release and installed-version contracts plus a repository that fetches `/releases/latest` without adding a new network stack.
- Covered the release-state mapper with RED/GREEN TDD around up-to-date, update-available, and failed-check outcomes.
- Bound the home update card so GitHub handoff only appears when the mapped update state exposes a release URL.

## Task Commits

1. **Task 1 RED: add failing update mapper coverage** - `34e3636` (`test`)
2. **Task 1 GREEN: implement GitHub release update contracts** - `43cb926` (`feat`)
3. **Task 2: add GitHub release update surface** - `d66ce1c` (`feat`)
4. **Task 2 follow-up: bind home update card CTA visibility to release state** - `9ce399a` (`feat`)

## Files Created/Modified
- `app/src/main/java/com/folklore25/ghosthand/GitHubReleaseInfo.kt` - Release metadata, installed-version model, and version comparison helper.
- `app/src/main/java/com/folklore25/ghosthand/GitHubReleaseRepository.kt` - GitHub latest-release fetch, parse, and update-result mapping.
- `app/src/main/java/com/folklore25/ghosthand/UpdateUiState.kt` - Truthful update state contract used by the home surface.
- `app/src/test/java/com/folklore25/ghosthand/UpdateUiStateMapperTest.kt` - TDD coverage for update-state mapping.
- `app/src/main/java/com/folklore25/ghosthand/MainActivity.kt` - Home update CTA binding limited to mapped GitHub destinations.
- `app/src/main/res/layout/activity_main.xml` - Home update card cleanup and CTA visibility baseline.

## Decisions Made
- Release status remains outside `RuntimeStateStore`; the home layer merges runtime truth with release-check truth in the screen-state path.
- GitHub is used only for release metadata and user handoff, never for silent or in-place installation claims.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- The worktree already contained an untracked `UpdateUiStateMapperTest.kt`. I treated it as the RED test artifact, verified it failed first, then committed it before the implementation commit.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 14 Plan 02 can build on the established update-state contract and home card without reopening runtime or permission architecture.
- Remaining Phase 14 work should focus on copy cleanup and the reusable explanation surface.

## Self-Check

PASSED

- Summary file exists at `.planning/phases/14-release-1-0-polish-and-update-architecture/14-01-SUMMARY.md`.
- Task commits `34e3636`, `43cb926`, `d66ce1c`, and `9ce399a` are present in git history.
- Placeholder resource defaults found in `activity_main.xml` remain standard pre-bind view defaults, not plan-blocking stubs.

---
*Phase: 14-release-1-0-polish-and-update-architecture*
*Completed: 2026-03-29*
