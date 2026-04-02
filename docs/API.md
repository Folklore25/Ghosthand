那就直接定 `docs/API.md v0.1`。
先把协议钉死，后面 Codex 才不会乱长。

---

````md id="43zw1m"
# Ghosthand Local API v0.1

## 1. API Statement

Ghosthand exposes a **localhost-only local API** for OpenClaw and local operator tooling.

This API is designed to provide a stable capability surface for:

- health checking
- state observation
- UI-semantic control
- recovery/self-healing
- screenshot capture

Ghosthand API is **capability-oriented**, not Android-implementation-oriented.

That means callers ask Ghosthand to:

- find
- tap
- type
- ensure
- capture

They do **not** directly invoke:

- `settings`
- `pm`
- `am`
- `dumpsys`

Those remain internal implementation details.

---

## 2. Transport Rules

### Base URL
```text
http://127.0.0.1:5583
````

### Versioning

V1 uses pathless versioning for simplicity.

If a breaking V2 is introduced later, use:

```text
/v2/...
```

### Content Types

* Requests: `application/json`
* Responses: `application/json`
* Image capture endpoints return base64-encoded PNG data inside the JSON envelope

### API Scope

V1 is **loopback-only**.

It must:

* bind `127.0.0.1` only
* not expose public network listeners
* not depend on cloud relay

---

## 3. Response Model

### 3.1 Success Envelope

All JSON success responses should follow this structure:

```json
{
  "ok": true,
  "data": {},
  "meta": {
    "requestId": "req_123",
    "timestamp": "2026-03-27T05:30:00Z"
  }
}
```

### 3.2 Error Envelope

All JSON error responses should follow this structure:

```json
{
  "ok": false,
  "error": {
    "code": "ACCESSIBILITY_UNAVAILABLE",
    "message": "Accessibility service is not enabled.",
    "details": {}
  },
  "meta": {
    "requestId": "req_124",
    "timestamp": "2026-03-27T05:30:05Z"
  }
}
```

### 3.3 Visual vs Structured Surface Payloads

Ghosthand exposes two different surface-reading routes:

- `GET /screen`
  - structured actionable surface snapshot
  - used for selector planning, element geometry, and visible-surface state
- `GET /screenshot`
  - visual truth
  - used for debugging, verification, and resolving ambiguity when structured output looks stale or hard to interpret

They do not return the same payload shape and should not be treated as interchangeable.

### 3.4 Substrate Principle

Ghosthand should remain a robust, faithful substrate for AI rather than a narrowly curated success-path.

Platform-level implications:

- expose as much practical capability and interface as possible
- prefer additive capability exposure over subtractive hiding
- expose real node properties, relationships, and function as faithfully as practical
- keep heuristics bounded, inspectable, and non-destructive
- when shaped output exists, do not let it become the only truth surface by policy

Workflow-level implications:

- prompting defaults, escalation order, and execution discipline belong primarily in external skills, playbooks, and runbooks
- Phase 10 remains in the Ghosthand main repo as the canonical operator-validation, evidence, and acceptance framework for the platform
- the future `ghosthand-skill` repository should own operator steering, prompting, selector-choice defaults, task-specific workflow discipline, and ClawHub-facing packaging
- the future skill repo does not replace Phase 10 or absorb the platform repo's validation/evidence framework
- the API should not remove truthful structure merely because a narrower workflow is easier to steer

---

## 4. Error Model

### Standard Error Codes

#### Transport / Request Errors

* `BAD_REQUEST`
* `INVALID_ARGUMENT`
* `UNSUPPORTED_OPERATION`
* `NOT_FOUND`
* `METHOD_NOT_ALLOWED`

#### Runtime / Capability Errors

* `RUNTIME_NOT_READY`
* `ACCESSIBILITY_UNAVAILABLE`
* `ACCESSIBILITY_ACTION_FAILED`
* `CAPABILITY_POLICY_DENIED`
* `DEVICE_STATE_UNAVAILABLE`
* `SCREENSHOT_UNAVAILABLE`
* `SCREENSHOT_FAILED`
* `APP_LAUNCH_FAILED`
* `APP_STOP_FAILED`
* `NODE_NOT_FOUND`
* `NODE_NOT_EDITABLE`
* `TIMEOUT`
* `RECOVERY_FAILED`

#### Internal Errors

* `INTERNAL_ERROR`
* `STATE_INCONSISTENT`

### HTTP Status Mapping

* `200` success
* `400` bad request / invalid argument
* `403` capability blocked by app policy
* `404` not found
* `409` state conflict / inconsistent state
* `422` action understood but failed semantically
* `500` internal error
* `503` required capability unavailable

---

## 5. Common Conventions

### 5.1 Backend Selection

Some actions support multiple backends.

Allowed values:

* `auto`
* `accessibility`
Default:

* `auto`

Meaning:

* `auto`: Ghosthand selects best backend
* `accessibility`: require semantic UI backend

### 5.2 Timeout

Where supported, `timeoutMs` is caller-provided with sane limits.

V1 defaults:

* default: `3000`
* max: `15000`

### 5.3 Coordinates

Coordinates use physical screen pixels:

```json
{
  "x": 540,
  "y": 1200
}
```

### 5.4 Package Names

Package name fields must use full Android package names:

```json
{
  "packageName": "com.tencent.mm"
}
```

### 5.5 Capability Policy

Sensitive capabilities are gated by the app-level policy layer exposed on the Permissions page.

- accessibility-backed routes are blocked when Accessibility policy is off
- screenshot routes are blocked when Screenshot policy is off

When a route is blocked this way, Ghosthand returns:

- HTTP `403`
- error code `CAPABILITY_POLICY_DENIED`

This is separate from system capability unavailability. A capability can be granted by Android and still intentionally blocked for Ghosthand/OpenClaw use.

### 5.6 Product Update Surface

The Ghosthand app may show release/update state in the operator surface by comparing the installed app version against GitHub latest-release metadata.

### 5.7 Selector Failure Evidence

Selector-driven interaction failures should remain compact but machine-readable in the normal response path.

For selector misses, especially `POST /click`, Ghosthand may return bounded evidence such as:

- `failureCategory`
- `searchedSurface`
- `matchSemantics`
- `requestedSurface`
- `requestedMatchSemantics`
- `matchedSurface`
- `matchedMatchSemantics`
- `usedSurfaceFallback`
- `usedContainsFallback`
- `selectorMatchCount`
- `actionableMatchCount`

These fields distinguish:

- no selector match
- same-surface exact-vs-contains mismatch
- alternate-surface match opportunity
- label match found but no actionable target resolved

Ghosthand should expose this evidence truthfully without broad debug-mode expansion or dishonest matcher broadening.

- this is product UI state, not a localhost API installer path
- Ghosthand does not claim silent or seamless in-app APK installation
- when an update exists, the product hands the user off to the GitHub release page for a full APK update

---

## 6. Endpoint Overview

### Health / State

* `GET /ping`
* `GET /health`
* `GET /state`
* `GET /commands`

### Device

* `GET /device`
* `GET /foreground`
* `GET /windows`

### Read / Inspect (Stage 1)

* `GET /screen`
* `GET /screenshot`
* `GET /info`
* `GET /focused`

### Accessibility / UI

* `GET /tree`
* `POST /find`
* `POST /tap`
* `POST /click`
* `POST /swipe`
* `POST /type`
* `POST /input`
* `POST /setText`

### Gestures / System

* `POST /launch`
* `POST /scroll`
* `POST /longpress`
* `POST /gesture`
* `POST /back`
* `POST /home`
* `POST /recents`

### Sensing / Utility

* `GET /screenshot`
* `GET /clipboard`
* `POST /clipboard`
* `GET /wait`
* `POST /wait`
* `GET /notify`
* `POST /notify`
* `DELETE /notify`

### Recovery

* `POST /ensure`
* `POST /repair`

---

## 7. Endpoint Definitions

---

## 7.1 `GET /health`

### Purpose

Fast readiness probe.

### Success Response

```json
{
  "ok": true,
  "data": {
    "status": "ready",
    "runtimeReady": true,
    "apiServerReady": true,
    "accessibilityReady": true,
    "screenshotReady": true
  },
  "meta": {
    "requestId": "req_health_1",
    "timestamp": "2026-03-27T05:31:00Z"
  }
}
```

### Fallback-After-Write Response

On some devices, a successful `POST /clipboard` from Ghosthand's app process can be followed by an immediate empty system clipboard read once Ghosthand is backgrounded. In that narrow case, `GET /clipboard` may return the last successful Ghosthand-written value once with an explicit reason:

```json
{
  "ok": true,
  "data": {
    "text": "ghosthand clip path",
    "reason": "clipboard_cached_after_write"
  }
}
```

### Notes

This is a shallow health check.
It should be cheap and safe to poll frequently.

---

## 7.0 `GET /ping`

### Purpose

Minimal liveness probe for local agents.

### Success Response

```json
{
  "ok": true,
  "data": {
    "service": "ghosthand",
    "version": "1.0 (1)"
  }
}
```

---

## 7.0b `GET /commands`

### Purpose

Return the machine-readable Ghosthand capability catalog for local agents.

### Notes

This should be treated as the runtime contract source for local mobile agents.
It includes:

* `schemaVersion`
* `selectorAliases`
* `selectorStrategies`
* per-command:
  * `id`
  * `category`
  * `method`
  * `path`
  * `description`
  * `params`
  * `responseFields`
  * `selectorSupport`
  * `focusRequirement`
  * `delayedAcceptance`
  * `transportContract`
  * `stateTruth`
  * `changeSignal`
  * `operatorUses`
  * `referenceStability`
  * `snapshotScope`
  * `recommendedInteractionModel`
  * `stability`
  * `exampleRequest`
  * `exampleResponse`

### Contract Notes

`GET /commands` is the canonical self-description endpoint for on-device agents.

Field meaning:

* `params`: every param now exposes `location` as `query` or `body`
* `responseFields`: machine-readable list of top-level keys expected inside the `data` success payload
* `selectorSupport`: selector aliases, supported strategy names, primary strategy set, and bounded selector aids supported by that command, or `null`
* `focusRequirement`: one of `none` or `focused_editable`
* `delayedAcceptance`: one of `none`, `recommended`, or `required`
* `transportContract`: transport-level expectation, such as `prompt_completion`
* `stateTruth`: what kind of UI truth the command exposes, such as `observer_context`, `structured_actionable_surface_snapshot`, `visual_truth`, or `final_settled_state`
* `changeSignal`: for change-detecting commands, the meaning of the boolean change signal; otherwise `none`
* `operatorUses`: machine-readable practical roles for zero-context operator use, such as `visual_truth`, `debugging`, `verification`, `observer_context`, `selector_planning`, or `content_desc_selector`
* `referenceStability`: whether returned or accepted node references are stable, such as `snapshot_ephemeral` or `not_applicable`
* `snapshotScope`: whether a reference is valid only inside one snapshot context, such as `same_snapshot_only`
* `recommendedInteractionModel`: the intended reliable model, such as `selector_reresolution`
* `stability`: current contract stability marker, currently `stable`

### Canonical Selector Model

For selector-driven interaction, Ghosthand treats these as normal primary selector paths:

* `text`
* `contentDesc`
* `resourceId`

Practical rules:

* do not default to text-only reasoning
* use `desc` when the meaningful label lives in `contentDesc`
* use `id` when the stable app-facing handle is a resource id
* use bounded aids like `index` only to disambiguate a selector that already matched

### Stable Reference Policy

For node-bearing routes, Ghosthand treats `nodeId` as:

* snapshot-ephemeral
* same-snapshot only
* a bounded local aid, not a stable cross-snapshot handle

Normal interaction model:

* use `nodeId` only inside the same trusted snapshot context
* after any meaningful UI change, prefer selector-based re-resolution
* do not treat post-change `NODE_NOT_FOUND` as proof that the app is broken until stale node reuse is ruled out

### Example Response

```json
{
  "ok": true,
  "data": {
    "schemaVersion": "1.14",
    "selectorAliases": {
      "text": "text",
      "desc": "contentDesc",
      "id": "resourceId"
    },
    "selectorStrategies": [
      "text",
      "textContains",
      "resourceId",
      "contentDesc",
      "contentDescContains",
      "focused"
    ],
    "commands": [
      {
        "id": "click",
        "category": "interaction",
        "method": "POST",
        "path": "/click",
        "description": "Click by nodeId or first-class selector (text, contentDesc, resourceId); request must return promptly after dispatch or fail fast with an error",
        "params": [
          {
            "name": "desc",
            "type": "string",
            "location": "body",
            "required": false,
            "description": "Exact content description selector; use when the meaningful label lives in contentDesc",
            "allowedValues": []
          }
        ],
        "responseFields": ["performed", "backendUsed"],
        "transportContract": "prompt_completion",
        "stateTruth": "none",
        "changeSignal": "none",
        "operatorUses": ["text_selector", "content_desc_selector", "resource_id_selector"],
        "referenceStability": "snapshot_ephemeral",
        "snapshotScope": "same_snapshot_only",
        "recommendedInteractionModel": "selector_reresolution",
        "selectorSupport": {
          "aliases": ["text", "desc", "id"],
          "strategies": ["text", "resourceId", "contentDesc"],
          "primaryStrategies": ["text", "contentDesc", "resourceId"],
          "boundedAids": []
        },
        "focusRequirement": "none",
        "delayedAcceptance": "none",
        "stability": "stable",
        "exampleRequest": {
          "desc": "Settings"
        },
        "exampleResponse": {
          "ok": true,
          "data": {
            "performed": true
          }
        }
      }
    ]
  }
}
```

---

## 7.2 `GET /state`

### Purpose

Return unified Ghosthand state snapshot.

### Success Response

```json
{
  "ok": true,
  "data": {
    "runtime": {
      "ready": true,
      "runtimeUptimeMs": 182000,
      "appStartedAt": "2026-03-27T02:19:20Z",
      "buildVersion": "1.0 (1)",
      "installIdentity": "2026-03-27T02:18:40Z",
      "tapProbeUiBuildState": "Present in this build",
      "foregroundServiceRunning": true
    },
    "accessibility": {
      "implemented": true,
      "enabled": false,
      "connected": false,
      "healthy": false,
      "status": "disabled"
    },
    "device": {
      "screenOn": true,
      "locked": null,
      "rotation": 0,
      "batteryPercent": 83,
      "charging": true,
      "foregroundPackage": "com.tencent.mm"
    },
    "openclaw": {
      "apiServerReady": true,
      "port": 5583
    },
    "recovery": {
      "implemented": false,
      "lastAction": null,
      "lastResult": null
    },
    "permissions": {
      "implemented": true,
      "usageAccess": true,
      "accessibility": false,
      "notifications": null,
      "overlay": null,
      "writeSecureSettings": false,
      "capabilities": {
        "accessibility": {
          "system": {
            "authorized": true,
            "enabled": true,
            "connected": true,
            "dispatchCapable": true,
            "healthy": true,
            "status": "enabled_connected"
          },
          "policy": {
            "allowed": true
          },
          "effective": {
            "usableNow": true,
            "reason": "accessibility_connected"
          }
        },
        "screenshot": {
          "system": {
            "authorized": true,
            "accessibilityCaptureReady": true,
            "mediaProjectionGranted": false
          },
          "policy": {
            "allowed": false
          },
          "effective": {
            "usableNow": false,
            "reason": "policy_blocked"
          }
        }
      }
    }
  },
  "meta": {
    "requestId": "req_state_1",
    "timestamp": "2026-03-27T05:31:10Z"
  }
}
```

### Notes

`/state` is the main debugging endpoint.
It should prefer stability over exhaustiveness.
For install validation, compare `runtime.installIdentity` with `runtime.appStartedAt` after:
install -> force-stop -> explicit activity start -> curl `/state`.
If `installIdentity` is newer than `appStartedAt`, the running process predates the latest install.
`permissions.capabilities.*` exposes the two-layer capability model explicitly:

- `system.*` = read-only platform truth
- `policy.allowed` = Ghosthand app-level allow/deny policy
- `effective.usableNow` / `effective.reason` = whether Ghosthand may actually use the capability now
- `permissions.capabilities.screenshot.system` is backend-specific and distinguishes accessibility screenshot readiness and MediaProjection session truth

---

## 7.3 `GET /device`

### Purpose

Return device-only state.

### Success Response

```json
{
  "ok": true,
  "data": {
    "screenOn": true,
    "locked": null,
    "rotation": 0,
    "batteryPercent": 83,
    "charging": true,
    "foregroundPackage": "com.tencent.mm"
  },
  "meta": {
    "requestId": "req_device_1",
    "timestamp": "2026-03-27T05:31:20Z"
  }
}
```

### Notes

Initial P3 implementation is accessibility-only.
Only `backend=auto` and `backend=accessibility` are supported.

---

## 7.4 `GET /foreground`

### Purpose

Return current foreground app/activity summary.

### Semantics

- `/foreground` is observer context.
- On this device path, do not treat `/foreground` as the sole truth source for the currently visible UI surface.
- When visible-surface truth matters for action planning, prefer `/screen`.
- When visual confirmation or debugging matters, prefer `/screenshot`.

### Success Response

```json
{
  "ok": true,
  "data": {
    "packageName": "com.tencent.mm",
    "activity": "com.tencent.mm.ui.LauncherUI",
    "label": "WeChat",
    "timestamp": "2026-03-27T05:31:25Z"
  },
  "meta": {
    "requestId": "req_fg_1",
    "timestamp": "2026-03-27T05:31:25Z"
  }
}
```

---

## 7.5 `GET /windows`

### Purpose

Return high-level window stack snapshot.

### Success Response

```json
{
  "ok": true,
  "data": {
    "windows": [
      {
        "id": 1,
        "type": "APPLICATION",
        "packageName": "com.tencent.mm",
        "focused": true,
        "title": "WeChat"
      },
      {
        "id": 2,
        "type": "INPUT_METHOD",
        "packageName": "com.google.android.inputmethod.latin",
        "focused": false,
        "title": "Keyboard"
      }
    ]
  },
  "meta": {
    "requestId": "req_windows_1",
    "timestamp": "2026-03-27T05:31:30Z"
  }
}
```

### Notes

V1 can keep this coarse.
Do not over-design window metadata yet.

---

## 7.6 `GET /tree`

### Purpose

Return current accessibility tree snapshot.

### Query Parameters

* `mode` = `flat` | `raw`
* default: `flat`

### Example

```text
GET /tree?mode=flat
```

### Success Response (`flat`)

```json
{
  "ok": true,
  "data": {
    "packageName": "com.tencent.mm",
    "activity": "com.tencent.mm.ui.LauncherUI",
    "warnings": [],
    "invalidBoundsCount": 0,
    "lowSignalCount": 0,
    "foregroundStableDuringCapture": true,
    "partialOutput": false,
    "returnedNodeCount": 1,
    "nodes": [
      {
        "nodeId": "n1",
        "text": "通讯录",
        "contentDesc": null,
        "resourceId": "com.tencent.mm:id/icon_tv",
        "className": "android.widget.TextView",
        "clickable": true,
        "enabled": true,
        "boundsValid": true,
        "actionableBounds": true,
        "lowSignal": false,
        "bounds": {
          "left": 100,
          "top": 2100,
          "right": 250,
          "bottom": 2200
        }
      }
    ]
  },
  "meta": {
    "requestId": "req_tree_1",
    "timestamp": "2026-03-27T05:31:40Z"
  }
}
```

### Notes

V1 supports `flat` first.
`raw` is explicitly unsupported for now and should return `UNSUPPORTED_OPERATION`.
If Accessibility is enabled but not connected, `/tree` should return `ACCESSIBILITY_UNAVAILABLE` instead of an empty tree.
Returned node identifiers are snapshot-ephemeral. After UI drift, prefer selector-based re-resolution instead of reusing a stale tree node id.
When invalid bounds are present on complex surfaces, `/tree` keeps the nodes but signals the risk explicitly through `warnings`, `invalidBoundsCount`, and per-node `boundsValid` / `actionableBounds`.
`foregroundStableDuringCapture = false` means the foreground summary changed during snapshot capture, so the returned tree should be treated with stale-risk caution even if it is the best available final attempt.
`lowSignalCount` and per-node `lowSignal` flag low-information passive structural nodes so operators do not mistake deep container noise for equally useful surface evidence.
`partialOutput = false` means the current `/tree` payload is structurally full in this implementation, even if warnings still indicate geometry, freshness, or low-signal caveats.

---

## 7.7 `POST /find`

### Purpose

Find a node by semantic query.

### Request Body

```json
{
  "strategy": "contentDesc",
  "query": "设置",
  "timeoutMs": 3000,
  "index": 0
}
```

### Allowed `strategy`

* `text`
* `textContains`
* `resourceId`
* `contentDesc`
* `contentDescContains`
* `focused`

### Notes

V1 accepts `timeoutMs`, but the initial implementation performs an immediate snapshot query only.
`found: false` is only valid when a real tree snapshot exists.
If the accessibility tree is unavailable, `/find` should return `ACCESSIBILITY_UNAVAILABLE` instead of pretending nothing matched.
`contentDesc` is a normal primary selector path, not a fallback behind `text`.
Use `index` only as a bounded aid to disambiguate multiple matches from an already meaningful selector.
Exact strategies stay exact. Contains matching only happens when the caller explicitly uses `textContains` or `contentDescContains`.
On meaningful misses, `/find` may return compact miss-only hinting such as `searchedSurface`, `matchSemantics`, `requestedSurface`, `requestedMatchSemantics`, `matchedSurface`, `matchedMatchSemantics`, `usedSurfaceFallback`, `usedContainsFallback`, `suggestedAlternateSurfaces`, `suggestedAlternateStrategies`, plus bounded `disclosure` guidance. These fields are there to distinguish surface mismatch, exact-vs-contains mismatch, and real absence without silently broadening default `/find` behavior.

### Success Response

```json
{
  "ok": true,
  "data": {
    "found": true,
    "node": {
      "nodeId": "n42",
      "text": "设置",
      "contentDesc": null,
      "resourceId": "com.example:id/settings",
      "className": "android.widget.TextView",
      "clickable": true,
      "enabled": true,
      "bounds": {
        "left": 100,
        "top": 500,
        "right": 260,
        "bottom": 580
      }
    }
  },
  "meta": {
    "requestId": "req_find_1",
    "timestamp": "2026-03-27T05:31:50Z"
  }
}
```

### Not Found Response

Return `200` with `found: false`, not `404`.

```json
{
  "ok": true,
  "data": {
    "found": false,
    "node": null
  },
  "meta": {
    "requestId": "req_find_2",
    "timestamp": "2026-03-27T05:31:55Z"
  }
}
```

---

## 7.8 `POST /tap`

### Purpose

Tap a point or node.

### Request Body by Coordinates

```json
{
  "target": {
    "type": "point",
    "x": 540,
    "y": 1200
  },
  "backend": "auto"
}
```

### Request Body by Node

```json
{
  "target": {
    "type": "node",
    "nodeId": "n42"
  },
  "backend": "auto"
}
```

### Success Response

```json
{
  "ok": true,
  "data": {
    "performed": true,
    "backendUsed": "accessibility"
  },
  "meta": {
    "requestId": "req_tap_1",
    "timestamp": "2026-03-27T05:32:00Z"
  }
}
```

### Notes

V1 should support:

* node tap
* point tap
* backend selection

Initial P2 implementation is accessibility-only.
Only `backend=auto` and `backend=accessibility` are supported.

---

## 7.9 `POST /swipe`

### Purpose

Perform swipe gesture.

### Request Body

```json
{
  "from": { "x": 500, "y": 1800 },
  "to": { "x": 500, "y": 600 },
  "durationMs": 300,
  "backend": "auto"
}
```

Accepted alias form for discoverability:

```json
{
  "x1": 500,
  "y1": 1800,
  "x2": 500,
  "y2": 600,
  "durationMs": 300
}
```

### Success Response

```json
{
  "ok": true,
  "data": {
    "performed": true,
    "backendUsed": "accessibility",
    "requestShape": "from_to",
    "contentChanged": true,
    "beforeSnapshotToken": "snap_a",
    "afterSnapshotToken": "snap_b",
    "finalPackageName": "com.reddit.frontpage",
    "finalActivity": "MainActivity"
  },
  "meta": {
    "requestId": "req_swipe_1",
    "timestamp": "2026-03-27T05:32:05Z"
  }
}
```

### Notes

Canonical request shape is `from` / `to` point objects.
`x1` / `y1` / `x2` / `y2` are accepted as backward-compatible aliases for zero-context discoverability.
`contentChanged` is the primary same-activity effect signal for whether Ghosthand observed a structured-surface change after the swipe.

For MIUI acceptance on the target device, do not treat an immediate post-dispatch reread as the final `/swipe` result.

Verification should use:

* probe signal before the swipe
* immediate post-dispatch reread for diagnostics
* delayed reread after about `300-600ms` as the actual effect check

If the delayed reread shows the probe signal changed, `/swipe` is considered successful even when the immediate reread was unchanged.

---

## 7.10 `POST /type`

### Purpose

Input text into current focused field.

### Request Body

```json
{
  "text": "hello world",
  "clearBefore": false,
  "backend": "auto"
}
```

### Success Response

```json
{
  "ok": true,
  "data": {
    "performed": true,
    "backendUsed": "accessibility"
  },
  "meta": {
    "requestId": "req_type_1",
    "timestamp": "2026-03-27T05:32:10Z"
  }
}
```

### Notes

V1 assumes current focus is already correct.
Do not overload `/type` with focus-finding logic.
`/type` is verified only for an already focused editable field.

---

## 7.13 `POST /ensure`

### Purpose

Check and optionally repair critical Ghosthand/runtime conditions.

### Request Body

```json
{
  "targets": [
    "accessibility",
    "foreground_service"
  ],
  "repair": true
}
```

### Allowed Targets

* `accessibility`
* `foreground_service`
* `api_server`
* `permissions`

### Success Response

```json
{
  "ok": true,
  "data": {
    "results": [
      {
        "target": "accessibility",
        "healthy": true,
        "repaired": false
      },
      {
        "target": "foreground_service",
        "healthy": true,
        "repaired": true
      }
    ]
  },
  "meta": {
    "requestId": "req_ensure_1",
    "timestamp": "2026-03-27T05:32:35Z"
  }
}
```

### Notes

`/ensure` is the main “make sure critical things are okay” endpoint.
It should stay generic and safe.

---

## 7.14 `POST /repair`

### Purpose

Request a specific recovery action.

### Request Body

```json
{
  "issue": "ACCESSIBILITY_DOWN"
}
```

### Allowed `issue`

* `ACCESSIBILITY_DOWN`
* `FOREGROUND_SERVICE_DOWN`
* `API_SERVER_DOWN`
* `PERMISSION_MISSING`
* `STATE_INCONSISTENT`

### Success Response

```json
{
  "ok": true,
  "data": {
    "issue": "ACCESSIBILITY_DOWN",
    "attempted": true,
    "repaired": true,
    "verificationPassed": true
  },
  "meta": {
    "requestId": "req_repair_1",
    "timestamp": "2026-03-27T05:32:45Z"
  }
}
```

### Notes

`/repair` is more explicit than `/ensure`.
Use it when the caller already knows the failure class.

---

## 7.15 `GET /screenshot`

### Purpose

Capture the current screenshot as base64 PNG.

### Semantics

- `/screenshot` is the visual-truth route.
- Use `/screenshot` when `/screen` or `/tree` looks stale, bounds look invalid, deep structure is unreadable, or action/result interpretation is ambiguous.
- `/screenshot` is the preferred debugging and verification fallback for zero-context operator use.
- `/screenshot` is not the route to use for selector planning or action geometry.

### Success Response

```json
{
  "ok": true,
  "data": {
    "image": "data:image/png;base64,...",
    "width": 1080,
    "height": 2400
  }
}
```

### Notes

Current preferred baseline is accessibility screenshot capability. MediaProjection remains the secondary compatibility path.

---

## 7.16 `GET /screen`

### Purpose

Return the current visible-surface read with explicit source provenance. Accessibility remains the default structured path, while explicit OCR or hybrid fallback can be requested when the accessibility surface is operationally insufficient.

### Query Parameters

* `editable` — *(optional)* filter to editable elements only
* `scrollable` — *(optional)* filter to scrollable elements only
* `clickable` — *(optional)* filter to clickable elements only
* `package` — *(optional)* restrict results to one package name
* `source` — *(optional)* `accessibility` | `ocr` | `hybrid`; defaults to `accessibility`

### Success Response

```json
{
  "ok": true,
  "data": {
    "packageName": "com.android.settings",
    "activity": "com.android.settings.MiuiSettings",
    "snapshotToken": "abcd1234",
    "capturedAt": "2026-03-28T00:00:00Z",
    "foregroundStableDuringCapture": true,
    "partialOutput": false,
    "candidateNodeCount": 1,
    "returnedElementCount": 1,
    "warnings": [],
    "omittedInvalidBoundsCount": 0,
    "omittedLowSignalCount": 0,
    "omittedNodeCount": 0,
    "omittedCategories": [],
    "omittedSummary": null,
    "invalidBoundsPresent": false,
    "lowSignalPresent": false,
    "source": "accessibility",
    "accessibilityElementCount": 1,
    "ocrElementCount": 0,
    "usedOcrFallback": false,
    "elements": [
      {
        "nodeId": "p0.0.1@tabcd1234",
        "text": "WLAN",
        "desc": "",
        "id": "android:id/title",
        "clickable": true,
        "editable": false,
        "scrollable": false,
        "bounds": "[211,1270][359,1338]",
        "centerX": 285,
        "centerY": 1304,
        "source": "accessibility"
      }
    ]
  },
  "meta": {
    "requestId": "req_screen_1",
    "timestamp": "2026-03-28T00:00:00Z"
  }
}
```

### Error Response

```json
{
  "ok": false,
  "error": {
    "code": "ACCESSIBILITY_UNAVAILABLE",
    "message": "Accessibility service is unavailable or not connected."
  }
}
```

### Notes

- `/screen` is the structured actionable surface route. It is not image capture.
- `/screen?source=accessibility` is the primary structured truth source for selector planning and action geometry on this device path.
- `/screen?source=ocr` returns OCR-derived output explicitly marked as OCR-derived. It does not pretend OCR blocks are native accessibility nodes.
- `/screen?source=hybrid` tries accessibility first and only supplements the payload with OCR-derived elements when the accessibility surface is empty or badly truncated enough to be operationally insufficient.
- `/screen` should be paired with `/screenshot` when structured output looks stale, geometry looks invalid, or the visible surface is hard to interpret.
- `snapshotToken` is a freshness marker for the current visible accessibility tree and should change when the visible surface changes materially.
- `elements[].nodeId` is snapshot-ephemeral and should only be reused inside the same trusted snapshot context.
- OCR-derived `elements[]` may have `nodeId = null` and always carry `source = "ocr"`.
- After the UI changes, prefer selector-based re-resolution instead of reusing an older `nodeId`.
- `/screen` omits nodes whose bounds are not actionable and reports that omission explicitly through `warnings` and `omittedInvalidBoundsCount`.
- This keeps `/screen` aligned with its role as the actionable surface route instead of pretending invalid geometry is usable.
- `/screen` now retries before accepting a capture when the foreground changes during the capture window.
- `foregroundStableDuringCapture = false` means Ghosthand returned the best available final attempt but could not fully guarantee freshness across the capture window.
- `/screen` also omits low-signal passive nodes by default and reports that omission through `warnings` and `omittedLowSignalCount`.
- Unlabeled but actionable nodes are retained; low-signal suppression is not allowed to hide genuine interaction structure.
- This keeps `/screen` readable on complex surfaces by foregrounding visible/actionable signal instead of deep container noise.
- `partialOutput = true` means `/screen` is a reduced actionable subset, not an exhaustive structured surface dump.
- `candidateNodeCount`, `returnedElementCount`, and `omittedNodeCount` make that reduction explicit so operators do not misread omission as absence.
- `omittedCategories`, `omittedSummary`, `invalidBoundsPresent`, and `lowSignalPresent` provide a compact explanation of what class of information was omitted without dumping the omitted nodes themselves.
- `source`, `accessibilityElementCount`, `ocrElementCount`, and `usedOcrFallback` make OCR provenance explicit instead of silently merging OCR and accessibility into one indistinguishable truth surface.
- `editable`, `scrollable`, and `clickable` filters only apply to `source=accessibility`. OCR-derived output does not claim those accessibility semantics.

---

## 7.17 `GET /info`

### Purpose

Return a structured device and window summary — foreground app, activity, screen state, and tree availability — in a single lightweight response.

### Success Response

```json
{
  "ok": true,
  "data": {
    "package": "com.tencent.mm",
    "activity": "com.tencent.mm.ui.LauncherUI",
    "label": "WeChat",
    "screen": {
      "on": true,
      "rotation": 0,
      "batteryPercent": 83,
      "charging": true
    },
    "tree": {
      "available": true,
      "reason": null
    }
  },
  "meta": {
    "requestId": "req_info_1",
    "timestamp": "2026-03-28T00:00:05Z"
  }
}
```

### Notes

Consolidates data from `DeviceSnapshotProvider`, `ForegroundAppProvider`, and `AccessibilityTreeSnapshotProvider`. Lighter than `/state` for callers that only need foreground context.

---

## 7.18 `GET /focused`

### Purpose

Return the currently focused accessibility node — the input focus or accessibility focus target — as a first-class endpoint.

### Success Response

```json
{
  "ok": true,
  "data": {
    "available": true,
    "node": {
      "nodeId": "n42",
      "text": "Search",
      "contentDesc": null,
      "resourceId": "com.example:id/search_input",
      "className": "android.widget.EditText",
      "clickable": false,
      "enabled": true,
      "focused": true,
      "bounds": {
        "left": 100,
        "top": 500,
        "right": 980,
        "bottom": 600
      }
    },
    "reason": null
  },
  "meta": {
    "requestId": "req_focused_1",
    "timestamp": "2026-03-28T00:00:10Z"
  }
}
```

### Error / No Focus Response

```json
{
  "ok": true,
  "data": {
    "available": false,
    "node": null,
    "reason": "accessibility_unavailable"
  }
}
```

### Notes

Returns `200` even when no focus is available (see above). Use `available: false` to distinguish no-focus from no-tree-access.
If a focused node exposes `nodeId`, treat it as same-snapshot only.

---

## 7.19 `POST /click`

### Purpose

Click an accessibility node by its `nodeId` or by a first-class selector path. For selector-based click, Ghosthand resolves to an actionable clickable target by default so nested text can still activate its wrapper, retries a bounded contains-based match for text/desc before failing, and reports how selector resolution landed on the dispatched target.

### Request Body

```json
{
  "desc": "设置"
}
```

Other normal selector forms:

```json
{
  "text": "Settings"
}
```

```json
{
  "id": "com.example:id/settings"
}
```

### Success Response

```json
{
  "ok": true,
  "data": {
    "performed": true,
    "backendUsed": "accessibility",
    "attemptedPath": "node_click",
    "stateChanged": false,
    "beforeSnapshotToken": "snap-before",
    "afterSnapshotToken": "snap-after",
    "finalPackageName": "com.example.target",
    "finalActivity": "TargetActivity",
    "resolution": {
      "requestedStrategy": "contentDesc",
      "effectiveStrategy": "contentDesc",
      "usedContainsFallback": false,
      "matchedNodeId": "p0.0.1@tabcd1234",
      "matchedNodeClickable": true,
      "resolvedNodeId": "p0.0.1@tabcd1234",
      "resolutionKind": "matched_node",
      "ancestorDepth": null
    }
  },
  "meta": {
    "requestId": "req_click_1",
    "timestamp": "2026-03-28T00:00:15Z"
  }
}
```

### Error Codes

* `400` / `INVALID_ARGUMENT` — no usable `nodeId` or selector provided
* `422` / `NODE_NOT_FOUND` — node not found in current tree
* `422` / `ACCESSIBILITY_ACTION_FAILED` — click dispatched but did not succeed
* `503` / `ACCESSIBILITY_UNAVAILABLE` — accessibility service not connected

Selector-driven click failures may also include bounded error details such as:

* `failureCategory`
* `selectorMatchCount`
* `actionableMatchCount`

### Notes

Differs from `POST /tap` with `type=node` in that `/click` is node-semantic only (no coordinate support) and uses `ACTION_CLICK` + clickable-parent fallback. Use `/tap` for coordinate-based tapping.
For zero-context operator use, do not assume meaningful labels only live in visible text. `desc` / `contentDesc` is a normal primary click path when that is where the app exposes the actionable label.
When the UI has already changed, prefer selector-based re-resolution over reusing an older `nodeId`.
Selector-based `/click` now defaults to actionable-target resolution. That means visible text on a child node can still activate a clickable wrapper or parent without requiring an explicit `clickable=true` hint.
For `text` and `desc` selectors, `/click` also retries a bounded fallback chain across exact/contains and `text`/`contentDesc` surfaces before failing, which reduces brittleness on feed/card surfaces where the visible label is truncated, nested, or only exposed on another semantic surface.
`attemptedPath` exposes the dispatch path that actually executed, such as `node_click` or `clickable_parent_click`.
`stateChanged`, `beforeSnapshotToken`, `afterSnapshotToken`, `finalPackageName`, and `finalActivity` provide a bounded observed-effect summary. `performed=true` means dispatch succeeded; it does not prove the intended visible effect happened.
`resolution` exposes the selector-resolution path before dispatch:

- `requestedStrategy`: the selector strategy requested by the caller
- `effectiveStrategy`: the strategy that actually matched, which may show the bounded contains or cross-surface fallback
- `requestedSurface`: the selector surface originally requested by the caller
- `matchedSurface`: the selector surface that actually matched
- `requestedMatchSemantics`: the requested match mode, such as `exact`
- `matchedMatchSemantics`: the match mode that actually matched, such as `contains`
- `usedSurfaceFallback`: whether Ghosthand had to cross to a different selector surface
- `usedContainsFallback`: whether bounded contains retry was needed
- `matchedNodeId`: the node that matched the selector text/desc/id
- `matchedNodeClickable`: whether that matched node was itself clickable
- `resolvedNodeId`: the node Ghosthand dispatched to after bounded actionable-target resolution
- `resolutionKind`: `matched_node` or `clickable_ancestor`
- `ancestorDepth`: ancestor distance when a clickable wrapper/ancestor was used

This convenience behavior is intentionally bounded. It should not become silent semantic guessing across unrelated ancestors or a substitute for exposing truthful node/action relationships elsewhere in the platform.
The current accepted conclusion is that inspectable wrapper resolution is now good enough at the platform layer; remaining friction on Reddit is increasingly about choosing the right selector surface (`text` vs `desc` vs `id`) rather than broad substrate opacity.

---

## 7.20 `POST /input`

### Purpose

Perform explicit focused-input operations without conflating text mutation and key dispatch.

### Request Body

```json
{
  "textAction": "set",
  "text": "hello world",
  "key": "enter"
}
```

Supported text mutation modes:

* `set`
* `append`
* `clear`

Supported keys:

* `enter`

### Success Response

```json
{
  "ok": true,
  "data": {
    "performed": true,
    "textChanged": true,
    "keyDispatched": true,
    "textMutation": {
      "requested": true,
      "performed": true,
      "action": "set",
      "previousText": "",
      "text": "hello world",
      "backendUsed": "accessibility"
    },
    "keyDispatch": {
      "requested": true,
      "performed": true,
      "key": "enter",
      "backendUsed": "accessibility"
    }
  },
  "meta": {
    "requestId": "req_input_1",
    "timestamp": "2026-03-28T00:00:20Z"
  }
}
```

### Error Codes

* `400` / `INVALID_ARGUMENT` — invalid or incomplete explicit input operation request
* `422` / `ACCESSIBILITY_ACTION_FAILED` — no focused editable target or one of the requested operations failed

### Notes

`/input` now treats text mutation and key dispatch as separate explicit operations.
Sending Enter must be expressed as `key: "enter"` and does not implicitly clear or replace text.
If both text mutation and key dispatch are requested, Ghosthand performs them in sequence and reports each result separately.
`POST /type` remains the keyboard-simulation route when character-by-character typing is specifically desired.

---

## 7.21 `POST /setText`

### Purpose

Set text on a specific accessibility node by `nodeId`. The node must be editable and enabled.

### Request Body

```json
{
  "nodeId": "n42",
  "text": "new value"
}
```

### Notes

`nodeId` here is snapshot-ephemeral and same-snapshot only. If focus or surface state may have changed, prefer re-resolving the target through `/screen` or `/find` before using `/setText`.

### Success Response

```json
{
  "ok": true,
  "data": {
    "performed": true,
    "backendUsed": "accessibility"
  },
  "meta": {
    "requestId": "req_settext_1",
    "timestamp": "2026-03-28T00:00:25Z"
  }
}
```

### Error Codes

* `400` / `INVALID_ARGUMENT` — `nodeId` or `text` missing
* `422` / `NODE_NOT_FOUND` — node not found in current tree
* `422` / `NODE_NOT_EDITABLE` — node is not editable or not enabled
* `422` / `ACCESSIBILITY_ACTION_FAILED` — action dispatched but did not succeed
* `503` / `ACCESSIBILITY_UNAVAILABLE` — accessibility service not connected

### Notes

Unlike `/input` (which targets whatever is focused), `/setText` targets a specific node by ID. Useful for pre-filled forms or non-focused fields.

---

## 7.22 `POST /scroll`

### Purpose

Scroll a node in a direction by computing a proportional swipe gesture from the node's bounding box.

### Request Body

```json
{
  "nodeId": "n42",
  "direction": "up"
}
```

Allowed directions: `up`, `down`, `left`, `right`.

### Success Response

```json
{
  "ok": true,
  "data": {
    "performed": true,
    "count": 1,
    "direction": "down",
    "attemptedPath": "repeated_scroll",
    "contentChanged": false,
    "surfaceChanged": false,
    "beforeSnapshotToken": "snap_a",
    "afterSnapshotToken": "snap_a",
    "finalPackageName": "com.reddit.frontpage",
    "finalActivity": "MainActivity"
  }
}
```

### Notes

Swipe vector is computed as 35% of node bounds in the target direction. Duration: 300ms.
`performed = true` means the gesture dispatched.
`contentChanged` is the primary operator-facing clue for whether Ghosthand actually observed a structured-surface change from the before/after snapshots.
`surfaceChanged` is retained as supporting detail for backward compatibility.

Interpretation rule:

- `performed = true` with `contentChanged = false` is not proof that content advanced
- verify with `/screen`
- use `/wait` as the preferred settle path where applicable

---

## 7.23 `POST /longpress`

### Purpose

Perform a long-press gesture at a coordinate.

### Request Body

```json
{
  "x": 540,
  "y": 1200,
  "durationMs": 500
}
```

`durationMs` is optional (default: 500). Range: 100–10000.

### Success Response

```json
{
  "ok": true,
  "data": { "performed": true }
}
```

---

## 7.24 `POST /gesture`

### Purpose

Dispatch an arbitrary multi-stroke gesture.

### Request Body

```json
{
  "strokes": [
    {
      "points": [{ "x": 540, "y": 1200 }, { "x": 540, "y": 600 }],
      "durationMs": 300
    }
  ]
}
```

### Success Response

```json
{
  "ok": true,
  "data": { "performed": true }
}
```

---

## 7.25 `POST /launch`

### Purpose

Launch an installed app by package name through the standard Android package launch intent path.

### Request Body

```json
{
  "packageName": "com.android.settings"
}
```

### Success Response

```json
{
  "ok": true,
  "data": {
    "launched": true,
    "packageName": "com.android.settings",
    "label": "Settings",
    "strategy": "package_launch_intent",
    "reason": "launched"
  }
}
```

### Failure Cases

- HTTP `404` + `PACKAGE_NOT_FOUND`
  - the package is not installed
- HTTP `422` + `NO_LAUNCH_INTENT`
  - the package is installed but does not expose a standard launch intent
- HTTP `503` + `LAUNCH_FAILED`
  - Ghosthand found a launch intent but the launch attempt failed

Failure responses include `error.details` with:

- `launched`
- `packageName`
- `label`
- `strategy`
- `reason`
- `error` when a launch attempt throws

### Notes

- This route is intentionally narrow: package-name launch only.
- It does not accept component names, deep links, extras, or a generic intent DSL.
- It does not fake success when a package is missing or has no launcher intent.

## 7.26 `POST /back`

Perform `GLOBAL_ACTION_BACK`. No request body required.

Successful responses also expose bounded observed-effect fields:

* `attemptedPath`
* `stateChanged`
* `beforeSnapshotToken`
* `afterSnapshotToken`
* `finalPackageName`
* `finalActivity`

## 7.27 `POST /home`

Perform `GLOBAL_ACTION_HOME`. No request body required.

Successful responses also expose bounded observed-effect fields:

* `attemptedPath`
* `stateChanged`
* `beforeSnapshotToken`
* `afterSnapshotToken`
* `finalPackageName`
* `finalActivity`

## 7.28 `POST /recents`

Perform `GLOBAL_ACTION_RECENTS`. No request body required.

---

## 7.29 `GET /clipboard`

### Purpose

Read the current primary clipboard text from Ghosthand's app process.

### Success Response

```json
{
  "ok": true,
  "data": {
    "text": "copied text"
  },
  "meta": {
    "requestId": "req_clipboard_read_1",
    "timestamp": "2026-03-28T00:00:35Z"
  }
}
```

### Empty Clipboard Response

```json
{
  "ok": true,
  "data": {
    "text": null,
    "reason": "clipboard_empty"
  }
}
```

### Notes

This endpoint returns `200` for present, empty, and narrow fallback-after-write states. Callers should inspect `data.text`.

`reason = "clipboard_cached_after_write"` means Ghosthand returned the last successful in-process clipboard write once because Android reported the clipboard empty immediately afterward. This is a best-effort fallback for agent continuity, not a general historical clipboard store.

---

## 7.30 `POST /clipboard`

### Purpose

Write text into the primary clipboard.

### Request Body

```json
{
  "text": "copied from Ghosthand"
}
```

### Success Response

```json
{
  "ok": true,
  "data": {
    "written": true
  },
  "meta": {
    "requestId": "req_clipboard_write_1",
    "timestamp": "2026-03-28T00:00:40Z"
  }
}
```

### Notes

A successful write can enable the one-read `clipboard_cached_after_write` fallback on the next `GET /clipboard` if Android immediately reports the clipboard empty while Ghosthand is backgrounded.

### Error Codes

* `400` / `INVALID_ARGUMENT` — `text` missing
* `422` / `CLIPBOARD_WRITE_FAILED` — clipboard write failed in the app process

---

## 7.30 `POST /wait`

### Purpose

Poll the current accessibility tree until a condition is satisfied or a timeout expires.

### Request Body

```json
{
  "condition": {
    "strategy": "text",
    "query": "Settings"
  },
  "timeoutMs": 5000,
  "intervalMs": 200
}
```

Supported strategies match `/find`, including `focused`.

### Success Response

```json
{
  "ok": true,
  "data": {
    "satisfied": true,
    "conditionMet": true,
    "stateChanged": false,
    "timedOut": false,
    "elapsedMs": 800,
    "node": {
      "nodeId": "n42",
      "text": "Settings",
      "contentDesc": null,
      "resourceId": "com.example:id/title"
    }
  },
  "meta": {
    "requestId": "req_wait_1",
    "timestamp": "2026-03-28T00:00:45Z"
  }
}
```

### Timeout Response

```json
{
  "ok": true,
  "data": {
    "satisfied": false,
    "conditionMet": false,
    "stateChanged": true,
    "timedOut": true,
    "elapsedMs": 5000,
    "reason": "timeout"
  }
}
```

### Error Codes

* `400` / `INVALID_ARGUMENT` — missing `condition`, missing `condition.strategy`, negative `timeoutMs`, or `intervalMs < 50`
* `422` / `UNSUPPORTED_OPERATION` — unsupported wait strategy

---

## 7.30b `GET /wait`

### Purpose

Wait for a foreground or tree change event and return the final observed UI state.

### Query Parameters

* `timeout` — optional timeout in ms
* `intervalMs` — optional polling interval in ms

### Success Response

```json
{
  "ok": true,
  "data": {
    "changed": true,
    "conditionMet": null,
    "stateChanged": true,
    "timedOut": false,
    "elapsedMs": 1024,
    "snapshotToken": "abcd1234",
    "packageName": "com.android.settings",
    "activity": "com.android.settings.MiuiSettings"
  }
}
```

### Semantics

- `changed = true` means Ghosthand observed a transition during the wait window.
- `changed = false` means Ghosthand did not observe a transition during that wait window.
- `conditionMet` is always `null` for `GET /wait` because no selector condition is involved.
- `stateChanged` tells you explicitly whether Ghosthand observed a UI-state transition.
- `timedOut` tells you explicitly whether the wait window expired before such a transition was observed.
- `packageName`, `activity`, and `snapshotToken` always describe the final observed settled state at the end of the wait.
- Agents should treat `packageName`, `activity`, and `snapshotToken` as the final-state truth source for `/wait`, not `changed` alone.

On this ROM/device, `packageName` and `activity` are the more trustworthy post-action truth source when diagnosing whether the UI already landed on the expected screen before or near the timeout boundary.

### Operator Guidance

- After visible-state-changing actions, `/wait` should be the standard settle path before using arbitrary fixed sleeps.
- If `timedOut = false` and final `packageName` / `activity` / `snapshotToken` match the expected target, treat the action as settled even when `stateChanged = false`.
- On same-activity surfaces, follow final `/wait` settled fields with a subsequent `/screen` to confirm that the visible content actually changed.
- Use a bounded fixed sleep only when `/wait` cannot express the relevant settle condition, and report that as a caveat rather than treating it as normal practice.
- Coordinate fallback should be treated as an exception path, not the default interaction model.
- Use coordinate fallback only when selector/action paths are insufficient for a concrete reason and the target region is justified by `/screen`, `/screenshot`, or both.
- After coordinate fallback, confirm the result with `/wait` or equivalent settled-state evidence rather than treating the tap itself as proof.
- On desc-heavy or container-driven surfaces, use a selector-to-action escalation ladder:
  - direct selector action
  - then `/find` with the strongest selector for that surface
  - then coordinate fallback only if the resolved target is still too broad for direct action

---

## 7.31 `POST /notify`

### Purpose

Post a local Ghosthand notification through Android's `NotificationManager`.

### Request Body

```json
{
  "title": "Ghosthand",
  "text": "Task complete"
}
```

`title` is optional. `text` is required.

### Success Response

```json
{
  "ok": true,
  "data": {
    "posted": true,
    "notificationId": 1001
  },
  "meta": {
    "requestId": "req_notify_post_1",
    "timestamp": "2026-03-28T00:00:50Z"
  }
}
```

### Error Codes

* `400` / `INVALID_ARGUMENT` — `text` missing
* `503` / `NOTIFICATION_FAILED` — post failed, including missing Android notification permission

### Cancel

`DELETE /notify`

```json
{
  "notificationId": 1001
}
```

Success response:

```json
{
  "ok": true,
  "data": {
    "canceled": true
  }
}
```

Error codes:

* `400` / `INVALID_ARGUMENT` — `notificationId` missing
* `422` / `NOTIFICATION_CANCEL_FAILED` — cancel failed

---

## 7.31b `GET /notify`

### Purpose

Read the buffered notification list for local agents.

### Query Parameters

* `package` — optional include filter
* `exclude` — optional comma-separated package list to suppress noise

### Success Response

```json
{
  "ok": true,
  "data": {
    "notifications": [
      {
        "package": "com.folklore25.ghosthand",
        "title": "Ghosthand",
        "text": "notify test",
        "tag": "ghosthand_notify",
        "id": 1001,
        "postedAt": "2026-03-28T10:00:00Z"
      }
    ]
  }
}
```

---

## 8. Internal Mapping Principles

### `/tap`

Maps to domain action:

* `TapNode`
* `TapPoint`

### `/swipe`

Maps to:

* `SwipeGesture`

### `/type`

Maps to:

* `InputText`

### `/ensure`

Maps to:

* `EnsureAccessibilityEnabled`
* `EnsureForegroundServiceAlive`
* `EnsurePermissionState`

### `/repair`

Maps to:

* `RepairKnownBrokenState`

Important:
Transport models stay in `feature/openclaw`.
Domain actions stay in `domain`.

---

## 9. Things V1 Explicitly Does Not Expose

The following must **not** be public API endpoints in V1:

* arbitrary shell execution
* arbitrary `su` command execution
* raw `pm` / `settings` / `cmd` passthrough
* unrestricted file read/write
* unrestricted process kill
* unrestricted package manager control

These are implementation details behind typed capability endpoints.

---

## 10. Logging Expectations

Every request should log at least:

* requestId
* route
* result (`ok` / `error`)
* latencyMs
* backendUsed if relevant

Dangerous actions should additionally log:

* action type
* target package or capability
* whether repair was attempted

Do not log:

* full screenshot bytes
* sensitive text payloads in full
* arbitrary secrets

---

## 11. Open Questions Kept for v0.2

These are intentionally deferred:

1. whether `/tree` should support filtered output
2. whether `/find` should support compound selectors
3. whether `/screenshot` should support ROI or JPEG
4. whether local auth token is needed in V1
5. whether `/ensure` should include dry-run mode
6. whether streaming or event subscription is needed

---

## 12. Final API Definition

Ghosthand Local API v0.1 is a localhost-only capability API for OpenClaw.

It provides:

* health/state inspection
* UI-semantic control
* device observation
* recovery actions
* screenshot capture

It does **not** expose raw Android or raw privileged-system primitives directly.

```
