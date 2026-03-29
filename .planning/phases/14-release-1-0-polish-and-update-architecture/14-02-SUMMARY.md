---
phase: 14-release-1-0-polish-and-update-architecture
plan: 02
subsystem: copy-and-help
tags: [copy-cleanup, explanations, dialogs, product-surface]
requires:
  - phase: 14-01
    provides: home update surface and release-state baseline
provides:
  - reusable module explanation catalog and dialog surface
  - final active product copy names for current surfaces
  - info affordances on home, permissions, and diagnostics modules
affects: [ui, strings, tests]
tech-stack:
  added: [DialogFragment]
  patterns: [shared explanation registry, bounded copy consolidation]
key-files:
  created: [app/src/main/java/com/folklore25/ghosthand/ModuleExplanation.kt, app/src/main/java/com/folklore25/ghosthand/ModuleExplanationDialogFragment.kt, app/src/test/java/com/folklore25/ghosthand/ExplanationContentTest.kt]
  modified: [app/src/main/java/com/folklore25/ghosthand/MainActivity.kt, app/src/main/java/com/folklore25/ghosthand/PermissionsActivity.kt, app/src/main/java/com/folklore25/ghosthand/DiagnosticsActivity.kt, app/src/main/res/layout/activity_main.xml, app/src/main/res/layout/activity_permissions.xml, app/src/main/res/layout/activity_diagnostics.xml, app/src/main/res/values/strings.xml, app/src/main/res/values-zh-rCN/strings.xml]
key-decisions:
  - "Used one shared dialog fragment for explanations instead of per-screen toasts or ad hoc dialogs."
  - "Promoted the final active copy into the base string names and removed the active v2/v3 variants from the current surfaces."
patterns-established:
  - "Product-module help is reusable, localized, and attached through explicit affordances."
requirements-completed: [POL-03, POL-04]
duration: inline-session
completed: 2026-03-30
---

# Phase 14 Plan 02 Summary

**Ghosthand now has one reusable in-app explanation pattern and one final active copy set for the current product surface.**

## Accomplishments

- Added a shared module explanation catalog and dialog fragment.
- Added info affordances to the key home, permissions, and diagnostics modules.
- Consolidated the active product copy away from the current `v2` and `v3` resource paths.
- Added explanation-content and layout-contract coverage for the new help surface.

## Verification

- `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.ExplanationContentTest`
- `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.HomeSurfaceLayoutContractTest --tests com.folklore25.ghosthand.PermissionsSurfaceLayoutContractTest :app:assembleDebug`
- `rg -n "_v2|_v3|closer to 1.0|更接近 1.0" app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml`

## Notes

- This slice intentionally uses a lightweight reusable dialog to stay within the accepted front-end direction.
- Broader layout/binder cleanup remains deferred to Plan 14-03.
