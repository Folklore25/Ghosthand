---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
last_updated: "2026-04-02T01:50:21.043Z"
progress:
  total_phases: 16
  completed_phases: 10
  total_plans: 37
  completed_plans: 26
---

# STATE.md — Ghosthand Project

## Project Reference

Ghosthand — Android accessibility-automation server + premium operator UI.
Accessibility-first device automation on the local app path only.
Current focus: **Phase 21 planning for Ghosthand 1.2.0** — the next bounded delivery hardens agent interaction through input/action/result/launch usability fixes rather than a platform rewrite.

## Current Position

Phase: 21 (agent-interaction-hardening-for-1-2-0) — EXECUTING
Plan: 2 of 5

- **Phase:** 21
- **Implementation baseline:** committed — Android app and Gradle project now tracked in git
- **Verified route set:** `/ping`, `/screen`, `/tree`, `/info`, `/focused`, `/find`, `/tap`, `/click`, `/input`, `/setText`, `/scroll`, `/swipe`, `/longpress`, `/gesture`, `/back`, `/home`, `/recents`, `/screenshot`, `/notify`, `/wait`, `/clipboard`, `/commands`, `/launch`
- **Status:** Ready to execute

## Progress

- Phase 09 implementation: **DONE** — route surface committed as the tracked Android baseline
- Runtime acceptance pass: **DONE FOR VERIFIED ROUTES** — device-shell verification completed for the current local-agent route set
- Consolidation/reporting: **DONE** — implementation baseline, planning refresh, and acceptance record exist
- Phase 10 Stage 1: **DONE** — `/commands` canonical contract hardening committed
- Phase 10 Stage 2: **DONE** — Scenarios 01 through 04 are accepted on-device through the device-shell baseline
- Phase 10 Stage 3: **DONE FOR FIRST OPERATOR SET** — `scenario-settings-search-back`, `scenario-settings-home-screenshot`, the core chain of `scenario-settings-clipboard-input`, and `scenario-notification-navigation` are accepted on the OpenClaw validation path
- Phase 10 Stage 4: **DONE** — accepted Stage 3 scenarios are now documented as reusable operator playbooks with prompt templates and a standard result schema
- Phase 10 Stage 5: **DONE** — operator preflight, execution discipline, and result normalization are codified into a canonical runbook
- Phase 10 Stage 6: **DONE** — operator evaluation rubric, run-quality classification, and cross-run comparison rules are codified into a canonical guide
- Phase 10 Stage 7: **DONE** — acceptance-audit checklist, evidence bundle structure, decision classes, and repo-truth promotion rules are codified into a canonical guide
- Phase 10 Stage 8: **DONE** — evidence package structure, storage rules, packaging rules, and archival discipline are codified into a canonical guide and template
- Phase 10 Stage 9: **DONE** — evidence index structure, milestone summary structure, and update rules are codified into a canonical guide and templates
- Phase 14: **DONE IN CODE** — update/version state, explanation affordances, and bounded polish landed on the accepted 1.0 surface
- Phase 14.1: **DONE IN CODE** — remaining technical debt on copy, explanation unification, MainActivity weight, and product-vs-diagnostics permission language was cleaned
- Phase 15: **DONE IN CODE** — stability hardening landed across backup boundaries, loopback API bounds, async capability-policy I/O, diagnostics logging, and tests
- Phase 16: **DONE IN CODE** — update interaction, version visibility, stale update-era resources, and final release-polish cleanup are closed
- Phase 17: **DONE IN DOCS** — exploratory OpenClaw feedback is now reconciled into repo truth through explicit issue classification instead of reactive defect inflation
- Phase 18: **PLANNED** — launch/open handoff will be audited against the live runtime/catalog/docs before any new primitive is proposed
- Phase 19: **DONE IN CODE** — home-surface copy and affordance polish refined the accepted 1.0 UI without reopening architecture or permission logic
- Phase 20: **PLANNED FOR V1.1.0** — close the visible-but-unreachable gap through multi-surface selector reachability, explicit OCR fallback, and explicit `/wait` outcome separation
- Phase 21: **PLANNED FOR V1.2.0** — harden interaction semantics and route-level truth for input, action feedback, selector failures, partial-output interpretation, and launch reliability
- Phase 22: **DONE IN CODE** — `/find` miss responses now expose searched selector surface, exact-vs-contains semantics, and bounded next-step hints for zero-context exact misses

Overall: Ghosthand is operating from an accepted Phase 09 baseline, a canonical Phase 10 operator-validation framework, and a release-quality 1.0 surface with `/find` discoverability now tightened further through bounded exact-miss hinting.

## Decisions

## Recent Decisions

| Decision | Rationale |
|---|---|
| Ghosthand stays accessibility-first on the local app path | Product simplification removed the privileged capability line from the app |
| Stage 1 first, not Stage 2 or 3 | Maximum code reuse, no new permissions, lowest risk |
| Port/adapt proven external patterns | Don't re-derive already solved mechanics |
| Planning-only before coding | User explicitly requested staged plan first |
| 16 endpoints across 3 stages | Matches the reference implementation's remaining parity gap cleanly |
| `/notify` uses `NotificationManager` post/cancel, not notification interception | No extra listener access required for the current capability set |
| `GET /commands` is part of the runtime surface for local agents | Agents on-device need discoverable route/method/param metadata from Ghosthand itself |
| Screenshot baseline uses accessibility screenshot capability when available | Manual MediaProjection consent is not acceptable as the default baseline for the settled local-agent path |
| Device-shell orchestration is the trustworthy acceptance method | Host-side timing introduced false negatives during route verification |
| Phase 09 is frozen unless a real regression appears | Phase 10 should harden scenarios and agent usability, not reopen accepted runtime work |
| Start Phase 10 scenarios on low-variance system surfaces | Settings and launcher flows are repeatable and reduce false negatives during scenario hardening |
| OpenClaw validation is manual and user-driven | Codex should prepare the build, contract, and prompt, but not try to discover or invoke OpenClaw itself |
| OpenClaw screenshot confirmation outranks the earlier Stage 3 runner mismatch for Scenario 02 | The operator path confirmed Ghosthand screenshots were genuinely different across Settings and Home |
| Scenario 03 should be recorded as a core-chain acceptance, not a full launcher-to-Settings replay | The clipboard-input behavior was re-proved; the starting Settings surface was inherited from prior accepted validation |
| Stage 3 should close on a bounded accepted operator-path set, not on endless scenario growth | The first four scenarios plus contract/transport hardening are sufficient to move forward deliberately |
| Stage 4 should convert accepted scenarios into reusable playbooks instead of adding more scenario count immediately | The next value is operator reuse and consistent prompting, not more raw validations |
| Stage 5 should define strict operator execution rules instead of relying on ad hoc OpenClaw behavior | Future validations need comparable runs and normalized reports |
| Stage 6 should grade and compare future runs instead of reviewing them informally | Future operator validations need stable quality labels and comparison rules |
| Stage 7 should define how runs become acceptance evidence instead of relying on session memory for repo-truth updates | Future operator runs need a standard audit and promotion path |
| Stage 8 should standardize evidence packaging and archival instead of relying on chat formatting | Future operator runs need reusable, storable evidence bundles |
| Stage 9 should summarize accumulated evidence without requiring direct inspection of raw packages | Future sessions need a quick milestone-truth surface |
| Stage 10 should finish Phase 10 by defining how evidence is consumed and published, then close the phase from repo truth | No further meta-process layers should be added after Stage 10 |
| Phase 11 should pivot to product-friction remediation from real operator evidence | The next value is reducing zero-context friction, not adding more meta-process layers |
| Screenshot-first remediation should focus on discoverability, contract role, and operator workflow before implementation rework | `/screenshot` already works; the immediate gap is first-class usability and truth-model clarity |
| desc/contentDesc should be treated as a normal primary selector path, not a secondary fallback behind text | Zero-context operators need a selector model that matches real app surfaces |
| `nodeId` should be treated as snapshot-ephemeral and same-snapshot only | Zero-context operators should rely on selector re-resolution after UI changes, not stale node reuse |
| `/screen` should not silently present unusable geometry as actionable | On complex surfaces, invalid bounds must be filtered or signaled explicitly to preserve trust |
| `/screen` and `/tree` should not silently accept captures taken across foreground drift as fully fresh | On transitions or noisy surfaces, best-effort final captures must be marked with stale-risk |
| `/screen` should not let low-signal passive structural nodes dominate the operator view | On complex surfaces, readable actionable signal matters more than exhaustive container noise, but unlabeled actionable nodes must remain visible |
| `/screen` should not silently look exhaustive when it is actually reduced | Operators need explicit partial-output signals before inferring absence or completion |
| Xiaohongshu should not remain in the active validation matrix | Its anti-automation / anti-AI enforcement risk would blur Ghosthand product conclusions with external platform risk |
| `/wait` should be the standard post-action settle path for operator use | Reddit validation showed fixed sleeps are still an operator habit and should become an explicit exception |
| Coordinate fallback should be an explicit exception path, not an ad hoc habit | Some real surfaces are visibly tappable but not accessibility-clickable, but fallback use still needs justification and settled-state confirmation |
| Selector-to-action escalation should be explicit before coordinate fallback | Reddit validation showed operators still have to improvise when moving from `/click` to `/find` to bounded coordinate fallback |
| Same-activity surfaces need explicit settle interpretation guidance | Reddit validation showed that `/wait.changed = false` can still be compatible with a successful content transition when `/screen` confirms the new content |
| Lighter-touch validation should drive the next optimization target | Ghosthand is meant to work for AI with less scaffolding, so the next defects should come from more natural-use runs |
| `/scroll` should not read as a black-box `performed:true` when visible-state change is the real question | The lighter-touch Reddit run showed that action dispatch and observed content advance still need clearer separation |
| `/swipe` should be discoverable without parameter-shape guessing | The lighter-touch Reddit run showed that natural operator assumptions can still miss the actual request model |
| Same-activity content advance should be easier to read than three weak signals | The lighter-touch Reddit run showed operators still had to infer content movement from `performed`, `surfaceChanged`, and snapshot tokens together |
| Selector-based click should reconcile nested visible text with actionable wrappers more naturally | Real surfaces still expose text on child nodes while the actual click target lives on a parent or wrapper |
| Child-text to clickable-wrapper reconciliation should be stronger | The latest Reddit evidence narrowed the remaining friction from broad clickable-gate issues to feed/card text living on non-clickable children |
| Phase 11 remediation should stay substrate-faithful | Short-term success-rate gains should not justify capability reduction, hidden heuristics, or over-curated structure when truthful exposure is possible |
| Wrapper-driven `/click` remediation should expose relationship truth before adding more heuristics | Inspectable resolution metadata keeps the substrate additive and less magical while preserving bounded click ergonomics |
| Inspectable wrapper resolution is accepted P1 evidence | The latest Reddit validation proved that child-text to clickable-wrapper resolution is now transparent enough for the platform layer |
| Remaining click friction is increasingly about selector-surface choice and future skill discipline | The broad clickable-gate problem is no longer the main issue after inspectable wrapper resolution landed |
| Phase 10 stays in the main repo as canonical validation/evidence truth | The accepted operator-path baseline, playbooks, runbook, evaluation, audit, packaging, and indexing artifacts are platform truth, not external skill logic |
| The future skill repo stays separate and narrower | It should handle prompting, selector-choice defaults, and task steering, but it must not replace Phase 10 or Phase 11 in the main repo |

- [Phase 14-release-1-0-polish-and-update-architecture]: Release status stays outside RuntimeStateStore and is merged at the home-screen state layer.
- [Phase 14-release-1-0-polish-and-update-architecture]: GitHub latest-release metadata is used only for update truth and external handoff, never silent installation.
- [Phase 15]: Set android:allowBackup to false because no concrete migration requirement justified app-data backup.
- [Phase 15]: Removed manifest references to backup rule resources so the disabled-backup stance is the only active strategy.
- [Phase 15]: Blocked __MACOSX alongside .DS_Store to keep packaging debris out of the repo.
- [Phase 15]: Loopback malformed-request handling now fails early with bounded parsing and safe 400/408/413/431 responses before endpoint dispatch.
- [Phase 21]: Reused existing before/after snapshot observation to report action effects without claiming user intent.
- [Phase 21]: Kept selector-failure classification small and normal-path friendly: category, surface/match semantics, and selector/actionability counts.

## Performance Metrics

| Phase | Plan | Duration | Tasks | Files | Recorded |
| --- | --- | --- | --- | --- | --- |

## Blockers / Concerns

- Main remaining risks are operator-path refinement risks, not route-surface risks:
  - repeated APK reinstalls on this ROM can require accessibility rebind through secure settings during testing
  - runtime restore on this ROM can require the app-owned runtime start control before `/ping` becomes available
  - OpenClaw execution results must be manually provided by the user
  - the repo still has local, uncommitted Phase 10 stage updates and icon/resource changes
- Working tree currently contains the uncommitted Phase 10 scenario/contract updates plus Stage 3 validation notes

| Phase 14-release-1-0-polish-and-update-architecture P01 | 401 | 2 tasks | 6 files |
| Phase 15 P01 | 7min | 2 tasks | 4 files |
| Phase 15 P02 | 3 min | 2 tasks | 5 files |
| Phase 21 P02 | 9min | 2 tasks | 12 files |

## Session Continuity

Last session: 2026-04-02T01:50:21.041Z
Next action: return to the next bounded platform phase only after a new concrete operator-path defect is confirmed

## Accumulated Context

### Roadmap Evolution

- Phase 19.1 inserted after Phase 19: Non-root onboarding crash hardening (URGENT)

## Context Notes

- `LocalApiServer` now serves the full local-agent route surface plus `GET /commands`
- `GhostCoreAccessibilityService` is the primary stable execution core; screenshot now works through declared accessibility screenshot capability on the device
- `AccessibilityTreeSnapshotProvider`, `AccessibilityNodeLocator`, and `AccessibilityNodeFinder` now provide fresh snapshots, snapshot-scoped node identity, and action-ready selector results
- Acceptance evidence is recorded in `docs/Phase09-Runtime-Acceptance.md`
- Project-specific guidance is now kept outside `docs/ghosthanddev/`, which is reference-only
- Phase 10 checkpoint lives in `.planning/phases/10-agent-integration-scenario-hardening/.continue-here.md`
- Stage 3 manual validation note lives in `.planning/phases/10-agent-integration-scenario-hardening/STAGE3-OPENCLAW-VALIDATION-01.md`
- Stage 3 screenshot validation note lives in `.planning/phases/10-agent-integration-scenario-hardening/STAGE3-OPENCLAW-VALIDATION-02.md`
- Stage 3 clipboard validation note lives in `.planning/phases/10-agent-integration-scenario-hardening/STAGE3-OPENCLAW-VALIDATION-03.md`
- Stage 3 notification validation note lives in `.planning/phases/10-agent-integration-scenario-hardening/STAGE3-OPENCLAW-VALIDATION-04.md`
- Stage 3 closeout note lives in `.planning/phases/10-agent-integration-scenario-hardening/STAGE3-CLOSEOUT.md`
- Stage 4 operator playbook note lives in `.planning/phases/10-agent-integration-scenario-hardening/STAGE4-OPERATOR-PLAYBOOKS.md`
- Stage 5 operator runbook note lives in `.planning/phases/10-agent-integration-scenario-hardening/STAGE5-OPERATOR-RUNBOOK.md`
- Stage 6 operator evaluation note lives in `.planning/phases/10-agent-integration-scenario-hardening/STAGE6-OPERATOR-EVALUATION.md`
- Stage 7 acceptance-audit note lives in `.planning/phases/10-agent-integration-scenario-hardening/STAGE7-ACCEPTANCE-AUDIT.md`
- Stage 8 evidence-packaging note lives in `.planning/phases/10-agent-integration-scenario-hardening/STAGE8-EVIDENCE-PACKAGING.md`
- Stage 8 evidence template lives in `.planning/phases/10-agent-integration-scenario-hardening/EVIDENCE-BUNDLE-TEMPLATE.json`
- Stage 9 evidence-indexing note lives in `.planning/phases/10-agent-integration-scenario-hardening/STAGE9-EVIDENCE-INDEXING.md`
- Stage 9 evidence-index template lives in `.planning/phases/10-agent-integration-scenario-hardening/EVIDENCE-INDEX-TEMPLATE.json`
- Stage 9 milestone-summary template lives in `.planning/phases/10-agent-integration-scenario-hardening/PHASE10-MILESTONE-SUMMARY-TEMPLATE.md`
- Phase 11 checkpoint lives in `.planning/phases/11-product-friction-remediation/.continue-here.md`
- Phase 11 platform-principles note lives in `.planning/phases/11-product-friction-remediation/PLATFORM-PRINCIPLES.md`
- Phase 11 Reddit validation note lives in `.planning/phases/11-product-friction-remediation/REDDIT-VALIDATION-05.md`
- Full planning lineage:
  - `.planning/phases/09-capability-parity-mainline/CAPABILITY-PARITY-PLAN.md`
  - `.planning/phases/09-capability-parity-mainline/RUNTIME-ACCEPTANCE-PLAN.md`
