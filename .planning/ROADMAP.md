# Roadmap: Ghosthand

## Overview

Ghosthand is moving from an accepted runtime and operator-validation baseline into a release-quality product surface. The immediate roadmap keeps the platform substrate truthful while closing remaining operator and productization gaps around UI, permissions, and governed capability use.

## Phases

**Phase Numbering:**
- Integer phases are planned milestone work.
- Decimal phases are urgent inserted work.

- [x] **Phase 9: Capability Parity Mainline** - Freeze the verified local-agent runtime baseline and acceptance plans.
- [x] **Phase 10: Agent Integration Scenario Hardening** - Establish the canonical operator validation, evidence, and acceptance framework.
- [ ] **Phase 11: Product Friction Remediation** - Reduce zero-context operator friction without weakening platform truth.
- [ ] **Phase 12: Release 1.0 Productization** - Rebuild governed capability consent and restructure the operator UI for a truthful 1.0 surface.
- [x] **Phase 13: Remove Root From Ghosthand Product Surface And Runtime** - Remove root entirely so the app, runtime, and UI stay focused on the supported non-root local path. (completed 2026-03-29)
- [x] **Phase 14: Release 1.0 Polish And Update Architecture** - Add real GitHub-release update state, reusable feature explanations, and final product-copy polish without reopening the major UI architecture. (completed 2026-03-30)
- [x] **Phase 14.1: Release 1.0 Technical Debt Cleanup** - Clean the lingering copy/resource debt, unify the explanation system, thin MainActivity further, and keep diagnostics-only fields out of product permission language. (completed 2026-03-30)
- [x] **Phase 15: Stability And Engineering Hardening** - Harden backup boundaries, local API resource limits, async policy I/O, diagnostics logging, test coverage, and engineering hygiene without changing product direction. (completed 2026-03-29)
- [x] **Phase 16: Release Polish Closeout** - Close the remaining update-interaction loop, delete stale product copy/resources, and finish the final 1.0 release-surface polish without reopening the accepted architecture. (completed 2026-03-30)
- [ ] **Phase 17: Agent Perspective Reconciliation 01** - Reconcile the latest zero-context OpenClaw exploratory evaluation against accepted repo truth, preserve the useful signal, reject misclassifications, and narrow the next platform-owned direction honestly.

## Phase Details

### Phase 9: Capability Parity Mainline
**Goal**: Lock in the accepted Ghosthand runtime surface, route contracts, and runtime acceptance baseline.
**Depends on**: Phase 3
**Success Criteria** (what must be TRUE):
  1. The verified local-agent routes compile and run from the Android app baseline.
  2. Runtime acceptance evidence exists for the baseline route surface.
  3. The platform can move forward without reopening core route parity by default.
**Plans**: 2 plans

Plans:
- [x] 09-01: Capability parity and execution-core consolidation
- [x] 09-02: Runtime acceptance and baseline lock

### Phase 10: Agent Integration Scenario Hardening
**Goal**: Keep validation, evidence, and acceptance discipline in the main repo so Ghosthand can prove its runtime truth through operator-path evidence.
**Depends on**: Phase 9
**Success Criteria** (what must be TRUE):
  1. Canonical operator playbooks, runbook, evaluation, audit, and evidence packaging artifacts exist in the main repo.
  2. Accepted OpenClaw/operator-path scenarios are recorded as repo truth.
  3. Phase 10 remains the platform’s canonical validation/evidence layer rather than moving to an external skill repo.
**Plans**: 1 plan

Plans:
- [x] 10-01: Scenario hardening, evidence framework, and closeout artifacts

### Phase 11: Product Friction Remediation
**Goal**: Reduce real operator friction on the accepted substrate while keeping Ghosthand additive, inspectable, and substrate-faithful.
**Depends on**: Phase 10
**Success Criteria** (what must be TRUE):
  1. The runtime contract is clearer for zero-context operator use on real surfaces.
  2. Reddit-derived friction fixes improve action interpretation without reducing platform truth.
  3. Remaining friction is narrowed honestly and recorded in repo truth.
**Plans**: TBD

Plans:
- [ ] 11-01: Bounded P1 remediation and platform-truth alignment

### Phase 12: Release 1.0 Productization
**Goal**: Rebuild Ghosthand’s governed capability model so sensitive/manual capabilities have real system truth, real app-policy control, real runtime enforcement, and a product-grade 1.0 operator surface built around that model.
**Depends on**: Phase 11
**Success Criteria** (what must be TRUE):
  1. [REL-01] The permissions page shows real system authorization, real persisted app policy, and real effective usability for Accessibility, Screenshot, and Root.
  2. [REL-02] Ghosthand/OpenClaw capability use is blocked when app policy is off even if the underlying system authorization exists.
  3. [REL-03] The home surface is restructured into a product-grade hierarchy with permissions as the governance surface, diagnostics as secondary, and root reduced to one bottom red advanced entry.
  4. [REL-04] MainActivity and PermissionsActivity render from cleaner centralized state models instead of decorative mirrored controls.
  5. [REL-05] The project still compiles after the architectural correction and UI overhaul slice.
**Plans**: 3 plans

Plans:
- [x] 12-01-PLAN.md — Governed capability core consolidation
- [x] 12-02-PLAN.md — Runtime/API truth alignment and contract correction
- [x] 12-03-PLAN.md — Operator surface restructuring and bounded 1.0 polish

### Phase 13: Remove Root From Ghosthand Product Surface And Runtime
**Goal**: Remove root from Ghosthand entirely so the product, runtime, and UI are focused on the non-root local app path only.
**Depends on**: Phase 12
**Success Criteria** (what must be TRUE):
  1. [ROOT-01] Root is removed from governed capability state, persistence, runtime snapshots, and route gating.
  2. [ROOT-02] Root UI and product affordances are removed from Home, Permissions, Diagnostics, and related resources.
  3. [ROOT-03] Root-specific runtime code, tests, and docs are removed or simplified so the app no longer implies supported root behavior.
  4. [ROOT-04] The non-root app path still compiles and remains the supported product baseline.
**Plans**: 3 plans

Plans:
- [x] 13-01-PLAN.md — Remove root from governed capability/runtime/API and tests
- [x] 13-02-PLAN.md — Remove root from UI and screen-state surfaces
- [x] 13-03-PLAN.md — Remove root from docs, planning truth, and final verification

### Phase 14: Release 1.0 Polish And Update Architecture
**Goal**: Polish the accepted 1.0 product surface with real GitHub-release update state, reusable in-app explanations, cleaned product copy, and bounded implementation cleanup without reopening the major UI or permission architecture.
**Depends on**: Phase 13
**Success Criteria** (what must be TRUE):
  1. [POL-01] The app compares the installed version against GitHub latest-release metadata and shows a truthful in-product update state.
  2. [POL-02] The update flow guides the user to GitHub for a full APK update handoff without implying fake seamless updating.
  3. [POL-03] Product-facing copy is cleaned up, obsolete string variants are removed, and EN plus zh-CN stay aligned.
  4. [POL-04] Relevant product modules expose a reusable explanation affordance with a consistent help surface.
  5. [POL-05] The accepted 1.0 front-end direction is tightened through bounded polish and cleaner screen-state driven implementation, and the project still compiles.
**Plans**: TBD

Plans:
- [x] 14-01: Update/version architecture and runtime release-state integration
- [x] 14-02: Product copy cleanup and reusable explanation pattern
- [x] 14-03: Bounded 1.0 surface polish and implementation cleanup

### Phase 14.1: Release 1.0 Technical Debt Cleanup
**Goal**: Remove the remaining Phase 14 technical debt by cleaning the active string system, unifying the explanation/help pattern, further reducing MainActivity binding weight, and keeping diagnostics-only permission detail out of product governance language.
**Depends on**: Phase 14
**Success Criteria** (what must be TRUE):
  1. [TD-01] Versioned and process-style string leftovers are deleted so the active product copy set has one final name per string.
  2. [TD-02] A unified reusable explanation entry pattern exists across the relevant product modules instead of one-off help calls.
  3. [TD-03] MainActivity no longer accumulates direct product binding and entry-point behavior unnecessarily.
  4. [TD-04] User-facing permission governance language is kept separate from diagnostics-only engineering fields such as `usageAccess`, `notifications`, `overlay`, and `writeSecureSettings`.
  5. [TD-05] The project still compiles after the cleanup pass.
**Plans**: TBD

Plans:
- [x] 14.1-01: Copy/resource consolidation and unified explanation system cleanup
- [x] 14.1-02: MainActivity thinning and product-vs-diagnostics permission-surface cleanup

### Phase 15: Stability And Engineering Hardening
**Goal**: Tighten the Android loopback API and foreground-service baseline into a robust engineering substrate by fixing backup defaults, bounding local API resources, removing blocking policy I/O from hot paths, improving diagnostics, adding meaningful tests, and cleaning engineering debris.
**Depends on**: Phase 14.1
**Success Criteria** (what must be TRUE):
  1. [STAB-01] Backup and data-extraction behavior are explicit engineering decisions with no sample boilerplate or unsafe defaults.
  2. [STAB-02] The loopback local API uses bounded resources, rejects malformed or oversized input safely, and fully shuts down sockets/executors.
  3. [STAB-03] No `runBlocking` remains in UI or state-refresh hot paths for capability policy I/O.
  4. [STAB-04] Failures in the API/runtime/update/permission path log enough diagnostic context to classify the cause.
  5. [STAB-05] Meaningful tests cover the hardening work, including comparator logic, request parsing, invalid content-length, oversized body handling, capability policy logic, and key state mapping branches.
  6. [STAB-06] Source/resource directories are clean of packaging artifacts, placeholder rule files, meaningless TODO boilerplate, and similar debris.
**Plans**: TBD

Plans:
- [x] 15-01: Backup-boundary and engineering-hygiene hardening
- [x] 15-02: Local API resource-bounds, malformed-input handling, and shutdown correctness
- [x] 15-03: Async capability-policy I/O, observability hardening, tests, and final verification

### Phase 16: Release Polish Closeout
**Goal**: Close the remaining release-level polish issues by finishing the update card interaction loop, refining version/update surface behavior, deleting stale copy/resources, and doing only the bounded code cleanup needed to support those polish fixes.
**Depends on**: Phase 15
**Success Criteria** (what must be TRUE):
  1. [RPC-01] Update UI behavior is complete across all meaningful states.
  2. [RPC-02] Up-to-date state still allows re-check and failed-check state still allows retry.
  3. [RPC-03] Update-available state clearly hands off to GitHub full update and surfaces installed/latest versions cleanly.
  4. [RPC-04] Remaining weird/dev/process-like copy and obsolete update-era resources are removed or consolidated.
  5. [RPC-05] The accepted 1.0 front-end direction is preserved and the result is more coherent, not more improvised.
  6. [RPC-06] Build verification is completed as part of the phase.
**Plans**: TBD

Plans:
- [x] 16-01: Update-state interaction closeout and version surface refinement
- [x] 16-02: Resource/copy cleanup and bounded polish support cleanup

### Phase 17: Agent Perspective Reconciliation 01
**Goal**: Process the latest zero-context OpenClaw exploratory run as agent-perspective evidence without letting exploratory misunderstandings overwrite accepted platform truth.
**Depends on**: Phase 16
**Success Criteria** (what must be TRUE):
  1. [APR-01] The exploratory report is preserved in repo truth as useful evidence instead of a raw complaint dump.
  2. [APR-02] False missing-capability claims are corrected when accepted repo truth already proves the capability exists.
  3. [APR-03] Real substrate defects are separated cleanly from discoverability weaknesses and app/platform constraints.
  4. [APR-04] The next platform-owned improvement direction is narrower and more truthful than the raw exploratory report.
  5. [APR-05] Phase 17 does not reopen a broad architecture rewrite or drift into skill-repo implementation.
**Plans**: 2 plans

Plans:
- [ ] 17-01: Exploratory report reconciliation note and four-bucket issue classification
- [ ] 17-02: Planning/state truth update and narrow next-direction definition

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 9. Capability Parity Mainline | 2/2 | Complete | 2026-03-28 |
| 10. Agent Integration Scenario Hardening | 1/1 | Complete | 2026-03-29 |
| 11. Product Friction Remediation | 0/1 | In progress | - |
| 12. Release 1.0 Productization | 3/3 | In progress | - |
| 13. Remove Root From Ghosthand Product Surface And Runtime | 2/3 | Complete    | 2026-03-29 |
| 14. Release 1.0 Polish And Update Architecture | 3/3 | Complete | 2026-03-30 |
| 14.1. Release 1.0 Technical Debt Cleanup | 2/2 | Complete | 2026-03-30 |
| 15. Stability And Engineering Hardening | 3/3 | Complete | 2026-03-30 |
| 16. Release Polish Closeout | 2/2 | Complete | 2026-03-30 |
| 17. Agent Perspective Reconciliation 01 | 0/2 | Planned | - |
