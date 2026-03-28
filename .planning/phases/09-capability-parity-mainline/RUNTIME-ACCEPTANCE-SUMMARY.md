# Phase 09 — Runtime Acceptance Summary

**Phase:** 09 — Mainline capability-parity runtime acceptance  
**Recorded:** 2026-03-28  
**Method:** Deliberate device-shell verification on the target Android device

---

## Acceptance Scope

This summary records the currently verified local-agent route set for Ghosthand's accessibility-first mainline.

It does **not** cover:
- future agent-hardening work
- regression automation depth
- any new route expansion
- any root-first execution direction

---

## Active Runtime Baseline

- **Execution core:** `GhostCoreAccessibilityService`
- **Coordinator path:** `GhosthandForegroundService` -> `LocalApiServer` -> `StateCoordinator`
- **Listener address:** `127.0.0.1:5583`
- **Primary product path:** accessibility-first local execution
- **Root role:** testing/recovery aid only, not the accepted mainline execution path

---

## Verified Routes

Read / inspect:
- `GET /ping`
- `GET /screen`
- `GET /tree`
- `GET /info`
- `GET /focused`
- `GET /screenshot`

Interaction:
- `POST /tap`
- `POST /click` by `text`
- `POST /click` by `desc`
- `POST /click` by `id`
- `POST /find`
- `POST /input`
- `POST /setText`
- `POST /scroll`
- `POST /swipe`
- `POST /longpress`
- `POST /gesture`
- `POST /back`
- `POST /home`
- `POST /recents`

Sensing / utility:
- `GET /notify`
- `POST /notify`
- `GET /wait`
- `GET /clipboard`
- `POST /clipboard`

Agent-facing introspection:
- `GET /commands`

---

## Acceptance Method

Routes were verified through live device-shell orchestration against Ghosthand's local server on `127.0.0.1:5583`.

Verification pattern:
1. Restore runtime after APK install
2. Rebind accessibility service when needed on this ROM
3. Use the repeatable runner in `scripts/ghosthand-verify-runtime.sh`
4. Call routes through the local HTTP server from the device shell
5. Confirm route success against real UI state transitions and live foreground app state

This was preferred over host-side timing because host orchestration produced false negatives for long-lived or state-sensitive requests.

Verified repeatable runner modes:
- `smoke`
- `core`
- `full`

### Canonical Operator Workflow

1. Build and install the current debug APK
2. Restore runtime and accessibility binding
3. Run `smoke` to confirm runtime-up and contract visibility
4. Run `core` to verify the main interaction chain
5. Run `full` to verify screenshot, notification, and wait acceptance

This is the current canonical acceptance order for Ghosthand on the target device.

---

## Alignment Notes

### What Ghosthand already matches well

- Capability grouping is cleanly split into:
  - read / inspect
  - interaction
  - sensing / utility
- Read ergonomics are action-ready:
  - `/screen` returns `centerX` / `centerY`
  - `/find` returns geometry directly usable by `/tap`
- Interaction ergonomics are practical for agents:
  - selector-driven `/click`
  - focused-field `/input`
  - direct-node `/setText`
  - swipe / long-press / gesture support
- Sensing coverage is aligned with the accepted capability set:
  - screenshot without root as the baseline
  - buffered notifications
  - clipboard read/write
  - wait-for-change route
- Runtime self-description is first-class through `GET /commands`

### What remains intentionally Ghosthand-native

- Ghosthand keeps its own listener address and product naming
- `GET /commands` is the canonical runtime self-description contract for local agents
- Verification is acceptance-driven through the Ghosthand runner, not by mirroring another app's exact workflow
- `/wait` is accepted based on observed UI-change behavior on-device; this summary does not claim a different internal mechanism than the one Ghosthand actually ships
- Response payloads remain Ghosthand-native where the route is already accepted and ergonomic

---

## Key Environment Caveats

- During **testing after APK reinstalls**, this ROM can require rewriting secure accessibility settings to rebind Ghosthand's accessibility service.
- That rebind behavior is an environment/testing caveat, not the intended user-facing baseline.
- On this ROM, the stable runtime restore path can require bringing Ghosthand to foreground and triggering the app-owned runtime start control before `/ping` is available.
- The settled screenshot baseline should be accessibility-first, not dependent on manual root grant or manual screenshot permission grant by users.
- Clipboard verification is accepted through the app-foreground path used by the runner on the target device.
- Device-shell orchestration remains the most trustworthy runtime verification method for this project today.

---

## Mainline Assessment

Phase 09's route surface is:
- implemented
- build-verified
- substantially verified on the target device

The next highest-value mainline work is not more feature work.

It is:
1. repo/planning normalization
2. machine-readable `/commands` contract strengthening
3. regression coverage around:
   - snapshot freshness
   - selector resolution
   - route response shape

---

## Declaration Boundary

Phase 09 may be treated as runtime accepted for the verified route set recorded above, with the listed environment caveats.

Subsequent work should focus on consolidation and hardening, not route-surface expansion.
