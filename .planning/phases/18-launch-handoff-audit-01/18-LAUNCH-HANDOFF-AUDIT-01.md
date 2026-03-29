# Phase 18 Launch Handoff Audit 01

## Files Inspected

- `app/src/main/java/com/folklore25/ghosthand/GhosthandCommandCatalog.kt`
- `app/src/main/java/com/folklore25/ghosthand/LocalApiServer.kt`
- `app/src/main/java/com/folklore25/ghosthand/GhosthandApiPayloads.kt`
- `docs/API.md`
- `docs/Runtime-Verification-Workflow.md`
- `docs/Architecture.md`
- `.planning/phases/17-agent-perspective-reconciliation-01/17-EXPLORATORY-RECONCILIATION-01.md`
- `.planning/phases/09-capability-parity-mainline/CAPABILITY-PARITY-PLAN.md`

## Live launch/open-related capability found

### Found

- `POST /home` exists as a launcher-home navigation primitive.
- `/commands` exists and exposes the live command catalog.
- internal Android app `startActivity(...)` use exists for Ghosthand’s own UI flows such as opening Permissions or Diagnostics.
- `NotificationDispatcher.kt` uses `packageManager.getLaunchIntentForPackage(context.packageName)` for relaunching Ghosthand itself from a notification.

### Not found

- no live `POST /launch` route in the current `GhosthandCommandCatalog.kt`
- no live `POST /launch` request handling in the current `LocalApiServer.kt`
- no `/commands` exposure of a clean app-launch/open primitive
- no live public API contract in `docs/API.md` for launching/opening another app by package/component

## Final Classification

**Genuinely absent, worth implementing**

## Reasoning

- The live runtime/catalog/API surface does not currently expose a clean app launch/open primitive.
- The existing `home` primitive is not app launch/open; it only navigates to launcher home.
- The Ghosthand app contains internal Android intent-launch usage, but only for Ghosthand’s own UI and notification flows. That is not the same thing as a public local-agent substrate primitive.
- Historical references to `/launch` in `docs/Architecture.md` and older planning artifacts prove prior intent, not current availability.
- Because the live `/commands` contract is missing any launch/open primitive, this is not primarily a discoverability problem.

## Narrowest justified next implementation target

If implemented next, the narrowest acceptable substrate primitive is:

- one explicit app-launch/open route that launches an app by package name through the normal Android launch intent path

Boundaries:
- package-name based first
- exposed in `/commands`
- documented in `docs/API.md`
- no broad magical intent system
- no generic intent-construction DSL
- no hidden skill-layer steering embedded in the platform
