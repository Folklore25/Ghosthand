# Phase 17: Agent Perspective Reconciliation 01 - Context

**Gathered:** 2026-03-30
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase is not an implementation rewrite. It is a truth-reconciliation pass over a real zero-context OpenClaw exploratory evaluation.

The phase owns:
- evidence preservation
- issue classification
- repo-truth correction
- next-direction narrowing

The phase does not own:
- broad runtime feature work
- skill-repo behavior changes
- reinterpreting accepted platform capabilities as missing

</domain>

<decisions>
## Locked Decisions

### Accepted repo truth remains authoritative
- If accepted evidence already proves a capability exists, exploratory failure to discover it is not proof of absence.
- `/wait` is accepted platform truth and must not be recorded as missing.
- Wrapper-driven `/click` transparency already exists and must not be regressed back into a broad opacity claim.

### Classification discipline
- Every issue from the exploratory run must land in one of four buckets:
  - substrate defect
  - discoverability / natural-consumption weakness
  - app/platform constraint
  - agent misclassification

### Platform principle
- Prefer additive exposure and clearer discoverability over capability reduction.
- Do not move platform truth into skill logic by accident.

### Narrow next direction only after classification
- The next platform-owned direction should only be named after the exploratory issues are split cleanly.
- Likely candidates are launch/open handoff, scroll/snapshot coherence, or selector-surface discoverability, but only if classification supports them.
</decisions>

<canonical_refs>
## Canonical References

### Accepted operator truth
- `.planning/phases/10-agent-integration-scenario-hardening/STAGE3-OPENCLAW-VALIDATION-01.md`
- `.planning/phases/10-agent-integration-scenario-hardening/STAGE3-OPENCLAW-VALIDATION-02.md`
- `.planning/phases/10-agent-integration-scenario-hardening/STAGE3-OPENCLAW-VALIDATION-03.md`
- `.planning/phases/10-agent-integration-scenario-hardening/STAGE3-OPENCLAW-VALIDATION-04.md`
- `.planning/phases/10-agent-integration-scenario-hardening/STAGE4-OPERATOR-PLAYBOOKS.md`
- `.planning/phases/10-agent-integration-scenario-hardening/STAGE5-OPERATOR-RUNBOOK.md`

### Phase 11 friction truth
- `.planning/phases/11-product-friction-remediation/PLATFORM-PRINCIPLES.md`
- `.planning/phases/11-product-friction-remediation/REDDIT-VALIDATION-05.md`

### Public contract / runtime truth
- `docs/API.md`
- `.planning/STATE.md`
- `.planning/ROADMAP.md`
</canonical_refs>

<specifics>
## Specific Ideas

- Preserve the exploratory run as evidence, but reject false “missing endpoint” conclusions where repo truth already proves the endpoint/capability exists.
- Use the exploratory run to improve natural discoverability and platform-owned clarity, not to flatten the platform into a narrower agent-success surface.
</specifics>
