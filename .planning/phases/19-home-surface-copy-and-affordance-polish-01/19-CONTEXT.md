# Phase 19: Home Surface Copy And Affordance Polish 01 - Context

**Gathered:** 2026-03-30
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase is a bounded UI polish pass over already-accepted product structure.

It owns:
- custom info affordance design
- title/version/update visual rebalance
- dedicated update modal interaction
- permissions top-bar cleanup
- visible copy normalization

It does not own:
- runtime/platform redesign
- permissions governance logic changes
- root decisions
- generic Android intent/update framework work
</domain>

<decisions>
## Locked Decisions

- Use a custom info affordance; do not keep `android:drawable/ic_dialog_info`.
- Update interaction moves into a dedicated modal/dialog/sheet.
- The home surface should feel calmer; version and update should stop competing with the title.
- The permissions page top-left back button must be removed.
- Visible product copy should be calm, precise, and final-product in tone.
- Runtime/platform logic should only change if a tiny UI-support hook is strictly required.
</decisions>

<canonical_refs>
## Canonical References

- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_permissions.xml`
- `app/src/main/res/layout/activity_diagnostics.xml`
- `app/src/main/java/com/folklore25/ghosthand/HomeScreenBinder.kt`
- `app/src/main/java/com/folklore25/ghosthand/HomeScreenUiState.kt`
- `app/src/main/java/com/folklore25/ghosthand/UpdateUiState.kt`
- `app/src/main/java/com/folklore25/ghosthand/ModuleExplanationDialogFragment.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh-rCN/strings.xml`
</canonical_refs>
