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
- [ ] **Phase 14: Release 1.0 Polish And Update Architecture** - Add real GitHub-release update state, reusable feature explanations, and final product-copy polish without reopening the major UI architecture.

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
- [ ] 13-03-PLAN.md — Remove root from docs, planning truth, and final verification

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
- [ ] 14-02: Product copy cleanup and reusable explanation pattern
- [ ] 14-03: Bounded 1.0 surface polish and implementation cleanup

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 9. Capability Parity Mainline | 2/2 | Complete | 2026-03-28 |
| 10. Agent Integration Scenario Hardening | 1/1 | Complete | 2026-03-29 |
| 11. Product Friction Remediation | 0/1 | In progress | - |
| 12. Release 1.0 Productization | 3/3 | In progress | - |
| 13. Remove Root From Ghosthand Product Surface And Runtime | 2/3 | Complete    | 2026-03-29 |
| 14. Release 1.0 Polish And Update Architecture | 1/3 | In Progress|  |
