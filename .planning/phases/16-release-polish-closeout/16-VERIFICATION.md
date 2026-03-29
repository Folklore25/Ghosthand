---
phase: 16-release-polish-closeout
verified: 2026-03-30T00:00:00Z
status: passed
score: 6/6 must-haves verified
---

# Phase 16: Release Polish Closeout Verification Report

**Phase Goal:** Finish the last release-polish defects by closing the update interaction loop, refining version/update behavior, deleting stale update-era copy/resources, and making only the bounded cleanup needed to support that closeout.
**Verified:** 2026-03-30T00:00:00Z
**Status:** passed

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Update UI behavior is complete across the meaningful product states. | ✓ VERIFIED | `UpdateUiState`, `GitHubReleaseCheckResult.Checking`, `HomeScreenUiState`, and `HomeScreenBinder` now model and bind checking, up-to-date, update-available, and failed states explicitly. |
| 2 | Up-to-date still allows re-check and failed-check still allows retry. | ✓ VERIFIED | `HomeScreenUiStateFactory` maps both `UP_TO_DATE` and `CHECK_FAILED` to a visible refresh action, and `HomeScreenActions` routes that action back to `refreshReleaseInfo()`. |
| 3 | Update-available still clearly hands off to GitHub full update. | ✓ VERIFIED | `UPDATE_AVAILABLE` maps to `UpdateActionMode.OPEN_RELEASE` with the GitHub release URL and no fake in-app install path. |
| 4 | Installed version and latest release are surfaced cleanly on the update card. | ✓ VERIFIED | Installed version is preserved even while checking, and latest release stays explicit or falls back to `Not available yet` when unknown. |
| 5 | Remaining weird/dev/process-like copy and obsolete update-era resources are removed or consolidated. | ✓ VERIFIED | `strings.xml` and `values-zh-rCN/strings.xml` no longer contain the stale static update/GitHub keys or the targeted transition wording. |
| 6 | The accepted 1.0 front-end direction is preserved and the project still builds. | ✓ VERIFIED | The work stayed inside the existing home update module and the full verification bundle passed. |

## Verification Commands

- `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.UpdateUiStateMapperTest --tests com.folklore25.ghosthand.ScreenUiStateMapperTest`
- `rg -n "home_update_button|home_github_button|home_update_url|home_github_url|closer to 1.0|产品界面" app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml`
- `./gradlew :app:testDebugUnitTest :app:assembleDebug`

## Gaps Summary

No Phase 16 blocker remains in the scoped release-polish surface.

