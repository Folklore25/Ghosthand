# Ghosthand Runtime Verification Workflow

## Purpose

This is the project-facing runtime verification workflow for Ghosthand.

It describes how to verify the current build on the device in a repeatable way.
Project progression and acceptance state still belong in `.planning/`.

## Entry Point

Use:

```bash
scripts/ghosthand-verify-runtime.sh
```

## Core Commands

### `install-current-build`

Streams the local debug APK into `pm install -r -S` on the device.

### `restore-runtime`

Restores the foreground service and accessibility binding after install or runtime drift.
On this ROM, the stable path may also require bringing Ghosthand to foreground and using the app-owned runtime start control before `/ping` is available.

### `smoke`

Quick health check:
- `/ping`
- `/foreground`
- `/commands`

### `core`

High-value interaction chain:
- restore runtime
- smoke
- `/commands` schema check
- `tree -> find -> click`
- focused + input
- selector click
- clipboard
- swipe

Stops at the first failing step and prints that failure point.

### `focused-input`

Targeted check for:
- `/focused`
- `/input`

### `selector-click`

Targeted check for `/click` by:
- text
- resource id
- content description

### `tree-find-click`

Targeted end-to-end chain check for:
- current `/tree` package alignment
- `/find` locating a known Ghosthand target
- `/click` performing against that selector

### `screenshot-check`

Targeted check for `/screenshot`.

### `notify-check`

Targeted check for `/notify` post/read.

### `commands-schema-check`

Targeted runtime check that `/commands` still exposes the expected schema fields.

### `wait-home`

Device-shell orchestration for `/wait` followed by `/home`.

### `full`

Runs:
1. install current build
2. restore runtime
3. core
4. screenshot-check
5. notify-check
6. wait-home

## Current Rules

- Prefer device-shell orchestration over host-side timing-sensitive verification.
- Treat root here as a testing recovery aid, not the product baseline.
- Treat `/commands` as the runtime contract source for agents.
- Treat the summary matrix as authoritative for the first narrow failing step.

## Caveat

On this ROM, repeated reinstalls may require explicit accessibility rebinding through secure settings during testing.
Clipboard verification is runner-scoped through the Ghosthand-foreground path used on the target device.
