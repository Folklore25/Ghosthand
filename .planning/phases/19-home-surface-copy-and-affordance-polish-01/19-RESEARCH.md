# Phase 19 Research — Home Surface Copy And Affordance Polish 01

## Current UI issues confirmed in code

### Info affordance

- Remaining info buttons still use the default Android info icon drawable.
- Button sizing was reduced in a previous quick task, but the icon asset itself still reads as system-default rather than product-grade.

### Title / version / update

- The home top area still uses a left title column and a right-side version/update control cluster.
- Update interaction is still exposed inline on the home surface rather than through a dedicated modal.

### Permissions top bar

- `activity_permissions.xml` still contains `permissionsBackButton`.
- `PermissionsActivity.kt` still wires that explicit back button.

### Copy

- `home_subtitle` currently reads `Human, this app is not built for YOU.` in both locales.
- That line is clearly not acceptable release copy and should be normalized in this phase.

## Planning implication

The phase should be split into:
1. reusable info-affordance resources
2. home/update modal redesign
3. permissions top-bar and copy cleanup with verification
