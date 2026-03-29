# Phase 18: Launch Handoff Audit 01 - Context

**Gathered:** 2026-03-30
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase is a truth audit only. It must determine whether app launch/open capability is:
- already present but poorly exposed
- partially present and under-expressive
- genuinely absent

No runtime implementation belongs in this phase.
</domain>

<decisions>
## Locked Decisions

- Live runtime/catalog/API truth outranks historical planning or architecture notes.
- Historical references to `/launch` are not enough to prove the current runtime still has launch capability.
- The audit must produce exactly one final classification.
- A next implementation target may be proposed only if the classification justifies it.
- The next target must stay narrow and inspectable.
</decisions>

<canonical_refs>
## Canonical References

- `app/src/main/java/com/folklore25/ghosthand/GhosthandCommandCatalog.kt`
- `app/src/main/java/com/folklore25/ghosthand/LocalApiServer.kt`
- `docs/API.md`
- `docs/Runtime-Verification-Workflow.md`
- `docs/Architecture.md`
- `.planning/phases/17-agent-perspective-reconciliation-01/17-EXPLORATORY-RECONCILIATION-01.md`
</canonical_refs>
