# Phase 09 Runtime Acceptance

## Scope

This record closes Phase 09 as the accessibility-first capability-parity runtime acceptance phase.

It covers:
- the active runtime baseline
- the verified route set
- the canonical acceptance method
- the environment caveats that still matter operationally

It does not cover:
- future feature expansion
- root-first execution
- broader UI or architecture work

## Active Runtime Baseline

- Execution core: `GhostCoreAccessibilityService`
- Runtime/control path: `GhosthandForegroundService` -> `LocalApiServer` -> `StateCoordinator`
- Listener address: `127.0.0.1:5583`
- Primary product path: accessibility-first local execution
- Root role: constrained testing and recovery aid only

## Verified Route Set

### Read / Inspect

- `GET /ping`
- `GET /screen`
- `GET /tree`
- `GET /info`
- `GET /focused`
- `GET /screenshot`

### Interaction

- `POST /tap`
- `POST /click`
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

### Sensing / Utility

- `GET /notify`
- `POST /notify`
- `GET /wait`
- `GET /clipboard`
- `POST /clipboard`

### Agent-Facing

- `GET /commands`

## Canonical Acceptance Method

Acceptance is based on the repeatable runtime runner:

1. `scripts/ghosthand-verify-runtime.sh smoke`
2. `scripts/ghosthand-verify-runtime.sh core`
3. `scripts/ghosthand-verify-runtime.sh full`

Acceptance method details:
- build locally with Gradle
- install on the target device
- restore runtime through the runner
- verify routes through device-shell orchestration against `127.0.0.1:5583`
- treat the runner summary matrix as the authoritative first-failing-step report

This is preferred over host-side ad hoc timing because host orchestration produced false negatives for state-sensitive routes.

## Environment Caveats

- On this ROM, repeated reinstalls can require accessibility rebinding during testing.
- On this ROM, runtime restore may need the app-owned runtime start control before `/ping` becomes available.
- Clipboard verification is accepted through the Ghosthand-foreground runner path used on the target device.
- Device-shell orchestration remains the authoritative runtime verification method for this project.

## Final Acceptance Statement

**Phase 09 is runtime accepted on evidence for the verified route set above, using the accessibility-first Ghosthand mainline, with the listed environment caveats.**
