---
phase: 19-home-surface-copy-and-affordance-polish-01
verified: 2026-03-30T00:00:00Z
status: passed
score: 7/7 must-haves verified
---

# Phase 19 Verification Report

## Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | All visible feature/module info buttons use a custom outlined circular affordance. | ✓ VERIFIED | `ic_info_affordance.xml`, `Widget_Ghosthand_InfoButton`, and touched layouts were updated. |
| 2 | The home title/version/update area is calmer and more balanced. | ✓ VERIFIED | `activity_main.xml` now uses title plus quiet version text on the left and a lightweight update trigger on the right. |
| 3 | Update interaction opens a dedicated modal. | ✓ VERIFIED | `UpdateDialogFragment.kt` and `dialog_update.xml` implement the modal flow. |
| 4 | The modal shows current version, latest version, and state-aware action behavior. | ✓ VERIFIED | `dialog_update.xml` plus `UpdateDialogFragment.kt` bind installed/latest/status rows and state-aware actions. |
| 5 | Update-available uses a solid yellow bottom action. | ✓ VERIFIED | `UpdateDialogFragment.kt` promotes the bottom action to solid yellow only for `UPDATE_AVAILABLE`. |
| 6 | The permissions top-left back button is removed and top bars are cleaner. | ✓ VERIFIED | `activity_permissions.xml` and `PermissionsActivity.kt` no longer use `permissionsBackButton`; diagnostics was aligned in the same direction. |
| 7 | Visible awkward/dev-ish copy in the touched surfaces is cleaned and the project still compiles. | ✓ VERIFIED | touched strings were normalized and the full build/test bundle passed. |

## Verification Commands

- `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.HomeSurfaceLayoutContractTest --tests com.folklore25.ghosthand.PermissionsSurfaceLayoutContractTest --tests com.folklore25.ghosthand.ExplanationContentTest --tests com.folklore25.ghosthand.ScreenUiStateMapperTest --tests com.folklore25.ghosthand.UpdateDialogLayoutContractTest`
- `./gradlew :app:testDebugUnitTest :app:assembleDebug`

