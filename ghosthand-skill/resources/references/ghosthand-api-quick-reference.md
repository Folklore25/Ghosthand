# Ghosthand API Quick Reference

This file is a compact route reference for the Ghosthand skill.

## Runtime and state

- `GET /ping` — basic service/version check
- `GET /health` — runtime readiness and listener state
- `GET /state` — runtime, accessibility, device, capability, and permission summary
- `GET /capabilities` — capability catalog plus live availability
- `GET /device` — device state only
- `GET /foreground` — current foreground app/activity
- `GET /commands` — machine-readable Ghosthand capability catalog
- `GET /events` — bounded recent observation/event window

## UI inspection

- `GET /screen` — compact actionable surface; supports `source=accessibility|ocr|hybrid`, `summaryOnly`, and accessibility-only filters such as `editable`, `scrollable`, `clickable`, and `package`
- `GET /tree` — fuller accessibility tree; useful when `/screen` is shaped or partial
- `GET /focused` — currently focused node

## Interaction

- `POST /find` — selector lookup; supports `text`, `desc`, `id`, explicit strategies (`text`, `textContains`, `contentDesc`, `contentDescContains`, `resourceId`, `focused`), `clickable`, and `index`
- `POST /click` — semantic click by `nodeId`, `text`, `desc`, or `id`; selector clicks default to actionable-target resolution
- `POST /tap` — coordinate tap
- `POST /input` — explicit focused-field text mutation and/or Enter dispatch
- `POST /type` — simpler focused text entry
- `POST /setText` — set text on a specific editable node by `nodeId`
- `POST /scroll` — semantic container scroll
- `POST /swipe` — coordinate swipe
- `POST /longpress` — coordinate long press
- `POST /gesture` — composite or named gesture
- `POST /back`, `POST /home`, `POST /recents` — global navigation

## Sensing and transport

- `GET /screenshot` / `POST /screenshot` — screenshot retrieval when visual truth is needed; width and height must be provided together or omitted together
- `GET /wait` — wait for UI change and inspect settled state
- `POST /wait` — wait for a selector condition
- `GET /clipboard` / `POST /clipboard` — clipboard read/write
- `GET /notify` / `POST /notify` / `DELETE /notify` — notification read/post/cancel

## Rules that matter in practice

- Always treat `/commands` as the source of truth when you are unsure about route shape.
- Use `/state` for the best current runtime truth and `/capabilities` for fuller live capability availability details.
- `nodeId` is snapshot-scoped. Re-resolve after a fresh observation.
- `partialOutput=true` on `/screen` means the compact view omitted something. Escalate to `/tree`, `source=hybrid`, or `/screenshot` before making strong claims.
- `changed=false` from `GET /wait` is not proof that the prior action failed.
- Capability use depends on both system authorization and Ghosthand policy.
- Prefer `/click` before `/tap` when a stable selector exists.
