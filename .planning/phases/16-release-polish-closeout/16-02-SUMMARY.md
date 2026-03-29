---
phase: 16-release-polish-closeout
plan: 02
subsystem: resources
tags: [android, strings, copy-cleanup, release-polish]
completed: 2026-03-30
status: passed
---

# Phase 16 Plan 02 Summary

Removed the remaining stale update-era resource residue and tightened the home-surface product copy without restarting the design direction.

## Completed

- Removed obsolete static update/GitHub string keys and URLs that no longer participate in the release-metadata flow.
- Added the explicit `home_update_action_checking` copy for the disabled checking state.
- Tightened remaining home/update/diagnostics copy so it reads as final product UI instead of transition language.
- Kept EN and zh-CN aligned.

## Key Files

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh-rCN/strings.xml`

## Verification

- `rg -n "home_update_button|home_github_button|home_update_url|home_github_url|closer to 1.0|产品界面" app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml`
- `./gradlew :app:testDebugUnitTest :app:assembleDebug`

