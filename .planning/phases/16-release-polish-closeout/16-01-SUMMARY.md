---
phase: 16-release-polish-closeout
plan: 01
subsystem: ui
tags: [android, updates, github-release, home-surface, state-model]
completed: 2026-03-30
status: passed
---

# Phase 16 Plan 01 Summary

Closed the update interaction loop on the accepted home surface without reopening the broader 1.0 architecture.

## Completed

- Preserved installed-version visibility while the release check is running by carrying installed app version into the `Checking` state.
- Added explicit update action behavior on the home surface:
  - `CHECKING` shows a disabled checking action
  - `UP_TO_DATE` keeps a re-check action
  - `CHECK_FAILED` keeps a retry action
  - `UPDATE_AVAILABLE` keeps the GitHub release handoff action
- Moved update-button behavior from `actionUrl == null` inference to explicit `UpdateActionMode`.

## Key Files

- `app/src/main/java/com/folklore25/ghosthand/GitHubReleaseInfo.kt`
- `app/src/main/java/com/folklore25/ghosthand/UpdateUiState.kt`
- `app/src/main/java/com/folklore25/ghosthand/HomeScreenUiState.kt`
- `app/src/main/java/com/folklore25/ghosthand/HomeScreenBinder.kt`
- `app/src/main/java/com/folklore25/ghosthand/RuntimeStateViewModel.kt`
- `app/src/test/java/com/folklore25/ghosthand/UpdateUiStateMapperTest.kt`
- `app/src/test/java/com/folklore25/ghosthand/ScreenUiStateMapperTest.kt`

## Verification

- `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.UpdateUiStateMapperTest --tests com.folklore25.ghosthand.ScreenUiStateMapperTest`

