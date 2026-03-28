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
3. Call routes through the local HTTP server from the device shell
4. Confirm route success against real UI state transitions and live foreground app state

This was preferred over host-side timing because host orchestration produced false negatives for long-lived or state-sensitive requests.

---

## Key Environment Caveats

- During **testing after APK reinstalls**, this ROM can require rewriting secure accessibility settings to rebind Ghosthand's accessibility service.
- That rebind behavior is an environment/testing caveat, not the intended user-facing baseline.
- The settled screenshot baseline should be accessibility-first, not dependent on manual root grant or manual screenshot permission grant by users.
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
