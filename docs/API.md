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
- optional device/system support when root exists
- screenshot capture

Ghosthand API is **capability-oriented**, not Android-implementation-oriented.

That means callers ask Ghosthand to:

- find
- tap
- type
- launch
- ensure
- capture

They do **not** directly invoke:

- `su`
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

### 3.3 Image Capture Payload

`GET /screen` and `POST /screenshot` both return:

```json
{
  "ok": true,
  "data": {
    "format": "png",
    "width": 1080,
    "height": 2400,
    "data": "<base64-encoded PNG>"
  }
}
```

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
* `ROOT_UNAVAILABLE`
* `ROOT_ACTION_DENIED`
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
* `root`

Default:

* `auto`

Meaning:

* `auto`: Ghosthand selects best backend
* `accessibility`: require semantic UI backend
* `root`: require root fallback backend

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

---

## 6. Endpoint Overview

### Health / State

* `GET /health`
* `GET /state`

### Device

* `GET /device`
* `GET /foreground`
* `GET /windows`

### Read / Inspect (Stage 1)

* `GET /screen`
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

* `POST /scroll`
* `POST /longpress`
* `POST /gesture`
* `POST /back`
* `POST /home`
* `POST /recents`

### Sensing / Utility

* `POST /screenshot`
* `GET /clipboard`
* `POST /clipboard`
* `POST /wait`
* `POST /notify`
* `DELETE /notify`

### App / System Control

* `POST /launch`
* `POST /stop`

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
    "rootReady": true
  },
  "meta": {
    "requestId": "req_health_1",
    "timestamp": "2026-03-27T05:31:00Z"
  }
}
```

### Notes

This is a shallow health check.
It should be cheap and safe to poll frequently.

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
    "root": {
      "implemented": false,
      "available": null,
      "healthy": null
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
      "implemented": false,
      "usageAccess": true,
      "accessibility": false,
      "notifications": null,
      "overlay": null,
      "writeSecureSettings": false
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
`backend=root` is accepted syntactically but should fail honestly until root support exists.

---

## 7.4 `GET /foreground`

### Purpose

Return current foreground app/activity summary.

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
    "nodes": [
      {
        "nodeId": "n1",
        "text": "通讯录",
        "contentDesc": null,
        "resourceId": "com.tencent.mm:id/icon_tv",
        "className": "android.widget.TextView",
        "clickable": true,
        "enabled": true,
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
If Accessibility is enabled but not connected, or no active root is available, `/tree` should return `ACCESSIBILITY_UNAVAILABLE` instead of an empty tree.

---

## 7.7 `POST /find`

### Purpose

Find a node by semantic query.

### Request Body

```json
{
  "strategy": "text",
  "query": "设置",
  "timeoutMs": 3000
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
`backend=root` is accepted syntactically but should fail honestly until root support exists.

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

### Success Response

```json
{
  "ok": true,
  "data": {
    "performed": true,
    "backendUsed": "accessibility"
  },
  "meta": {
    "requestId": "req_swipe_1",
    "timestamp": "2026-03-27T05:32:05Z"
  }
}
```

### Notes

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

## 7.11 `POST /launch`

### Purpose

Launch an app.

### Request Body

```json
{
  "packageName": "com.tencent.mm"
}
```

### Optional Activity-Specific Launch

```json
{
  "packageName": "com.tencent.mm",
  "activity": "com.tencent.mm.ui.LauncherUI"
}
```

### Success Response

```json
{
  "ok": true,
  "data": {
    "launched": true,
    "packageName": "com.tencent.mm"
  },
  "meta": {
    "requestId": "req_launch_1",
    "timestamp": "2026-03-27T05:32:20Z"
  }
}
```

### Notes

Implementation may use optional root-backed support internally, but API must not expose shell semantics and must not assume root exists.

---

## 7.12 `POST /stop`

### Purpose

Force-stop an app.

### Request Body

```json
{
  "packageName": "com.tencent.mm"
}
```

### Success Response

```json
{
  "ok": true,
  "data": {
    "stopped": true,
    "packageName": "com.tencent.mm"
  },
  "meta": {
    "requestId": "req_stop_1",
    "timestamp": "2026-03-27T05:32:25Z"
  }
}
```

### Notes

`/stop` may use optional root-backed support internally, but root is not a baseline user requirement and missing root must be surfaced truthfully.

---

## 7.13 `POST /ensure`

### Purpose

Check and optionally repair critical Ghosthand/runtime conditions.

### Request Body

```json
{
  "targets": [
    "accessibility",
    "foreground_service",
    "root"
  ],
  "repair": true
}
```

### Allowed Targets

* `accessibility`
* `foreground_service`
* `api_server`
* `root`
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

## 7.15 `POST /screenshot`

### Purpose

Capture the full device screen through MediaProjection after the user grants screenshot permission in the Ghosthand app.

### Request Body

```json
{
  "width": 1080,
  "height": 2400
}
```

Both fields are optional. When omitted or `0`, the provider uses the device's current screen size.

### Success Response

```json
{
  "ok": true,
  "data": {
    "format": "png",
    "width": 1080,
    "height": 2400,
    "data": "<base64-encoded PNG>"
  },
  "meta": {
    "requestId": "req_screenshot_1",
    "timestamp": "2026-03-28T00:00:00Z"
  }
}
```

### Error Codes

* `400` / `BAD_REQUEST` — request body is not valid JSON
* `503` / `PROJECTION_NOT_GRANTED` — MediaProjection permission has not been granted yet
* `503` / `SCREENSHOT_UNAVAILABLE` — capture failed after permission was granted

### Notes

This is the full-screen capture path. Users must tap the in-app screenshot-permission button once before the endpoint can succeed.

---

## 7.16 `GET /screen`

### Purpose

Capture the current window as a PNG image, returned as base64-encoded JSON.

### Query Parameters

* `width` — *(optional)* desired output width in pixels. Default: window width.
* `height` — *(optional)* desired output height in pixels. Default: window height.

### Success Response

```json
{
  "ok": true,
  "data": {
    "format": "png",
    "width": 1080,
    "height": 2400,
    "data": "<base64-encoded PNG>"
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
    "code": "SCREENSHOT_UNAVAILABLE",
    "message": "Screenshot is not available. Reason: root_unavailable"
  }
}
```

### Notes

`/screen` is the lightweight accessibility-core capture path. Use `/screenshot` when full-screen MediaProjection capture is required.

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

---

## 7.19 `POST /click`

### Purpose

Click an accessibility node by its `nodeId`. The node's clickable parent is used as the click target if direct click fails.

### Request Body

```json
{
  "nodeId": "n42"
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
    "requestId": "req_click_1",
    "timestamp": "2026-03-28T00:00:15Z"
  }
}
```

### Error Codes

* `400` / `INVALID_ARGUMENT` — `nodeId` missing or empty
* `422` / `NODE_NOT_FOUND` — node not found in current tree
* `422` / `ACCESSIBILITY_ACTION_FAILED` — click dispatched but did not succeed
* `503` / `ACCESSIBILITY_UNAVAILABLE` — accessibility service not connected

### Notes

Differs from `POST /tap` with `type=node` in that `/click` is node-semantic only (no coordinate support) and uses `ACTION_CLICK` + clickable-parent fallback. Use `/tap` for coordinate-based tapping.

---

## 7.20 `POST /input`

### Purpose

Set text in the currently focused editable field via `ACTION_SET_TEXT`. No keyboard simulation.

### Request Body

```json
{
  "text": "hello world"
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
    "requestId": "req_input_1",
    "timestamp": "2026-03-28T00:00:20Z"
  }
}
```

### Error Codes

* `400` / `INVALID_ARGUMENT` — `text` missing or not a string
* `422` / `ACCESSIBILITY_ACTION_FAILED` — no focused editable target or action failed

### Notes

Differs from `POST /type` in that `/input` uses `ACTION_SET_TEXT` directly on the focused node (fast, no keyboard). `POST /type` is retained for character-by-character keyboard simulation use cases.

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
  "data": { "performed": true }
}
```

### Notes

Swipe vector is computed as 35% of node bounds in the target direction. Duration: 300ms.

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

## 7.25 `POST /back`

Perform `GLOBAL_ACTION_BACK`. No request body required.

## 7.26 `POST /home`

Perform `GLOBAL_ACTION_HOME`. No request body required.

## 7.27 `POST /recents`

Perform `GLOBAL_ACTION_RECENTS`. No request body required.

---

## 7.28 `GET /clipboard`

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

This endpoint returns `200` for both present and empty clipboard states. Callers should inspect `data.text`.

---

## 7.29 `POST /clipboard`

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
    "elapsedMs": 5000,
    "reason": "timeout"
  }
}
```

### Error Codes

* `400` / `INVALID_ARGUMENT` — missing `condition`, missing `condition.strategy`, negative `timeoutMs`, or `intervalMs < 50`
* `422` / `UNSUPPORTED_OPERATION` — unsupported wait strategy

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

### `/launch`

Maps to:

* `LaunchApp`

### `/ensure`

Maps to:

* `EnsureAccessibilityEnabled`
* `EnsureForegroundServiceAlive`
* `EnsureRootAvailable`
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
* whether root path was used
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
* app launch/stop
* recovery actions
* screenshot capture

It does **not** expose raw Android or raw root primitives directly.

```
