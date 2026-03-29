# Phase 18 Research — Launch Handoff Audit 01

## Live findings before planning

### Present in current live runtime/catalog

- `/home` exists in `GhosthandCommandCatalog.kt` and `LocalApiServer.kt`
- `/commands` exposes the live command catalog
- there is no live `/launch` or `/stop` command in the current command catalog
- there is no live `/launch` handling in the current `LocalApiServer.kt`

### Historical/stale references

- `docs/Architecture.md` still mentions `launchApp`, `POST /launch`, and `POST /stop`
- `.planning/phases/09-capability-parity-mainline/CAPABILITY-PARITY-PLAN.md` still lists `/launch` and `/stop`

## Planning implication

The audit does not support “already exists, discoverability weak”.
The live runtime does not currently expose a clean launch/open primitive through `/commands` or `LocalApiServer`.

The remaining classification question is:
- partially exists and under-expressive
- or genuinely absent

The strongest evidence so far points to **genuinely absent in the live runtime**, with only historical references and unrelated internal Android app intents remaining.
