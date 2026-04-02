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
- [x] **Phase 17: Agent Perspective Reconciliation 01** - Reconcile the latest zero-context OpenClaw exploratory evaluation against accepted repo truth, preserve the useful signal, reject misclassifications, and narrow the next platform-owned direction honestly. (completed 2026-03-30)
- [ ] **Phase 18: Launch Handoff Audit 01** - Audit whether Ghosthand actually lacks a clean app launch/open primitive, or whether launch capability already exists but is only under-expressive or poorly exposed.
- [x] **Phase 19: Home Surface Copy And Affordance Polish 01** - Polish the current 1.0 UI with a refined info-affordance system, calmer title/version/update presentation, dedicated update modal flow, permissions top-bar cleanup, and final product copy cleanup without reopening the accepted architecture. (completed 2026-03-30)
- [ ] **Phase 20: Close The Visible-But-Unreachable Gap For 1.1.0** - Improve semantic reachability across text/contentDescription surfaces, add explicit OCR fallback for empty-tree content surfaces, and split `/wait` outcome semantics without broadening the platform.
- [ ] **Phase 21: Agent Interaction Hardening For 1.2.0** - Reduce avoidable agent friction by hardening input semantics, action-effect feedback, selector failure reasons, `/screen` partial-output summaries, and `/launch` reliability without broadening the platform.
- [ ] **Phase 22: Patch Stabilization For 1.2.1** - Land the narrow 1.2.1 patch scope around OCR fallback discoverability, stale-node failure classification, and modal-transition accessibility guidance without reopening 1.2 feature work.

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
- [x] 17-01: Exploratory report reconciliation note and four-bucket issue classification
- [x] 17-02: Planning/state truth update and narrow next-direction definition

### Phase 18: Launch Handoff Audit 01
**Goal**: Determine truthfully whether Ghosthand currently has a clean app launch/open primitive, whether it exists only partially or weakly exposed, or whether it is genuinely absent. Current repo truth: non-root `/launch` is intentionally removed from the 1.x product surface and future launch work is deferred to a root-backed 2.0 line.
**Depends on**: Phase 17
**Success Criteria** (what must be TRUE):
  1. [LHA-01] The live runtime/API/catalog/docs are audited for launch/open-related capability rather than relying on historical assumptions.
  2. [LHA-02] Launch/open status is classified exactly once as one of: already exists with weak discoverability, partially exists and under-expressive, or genuinely absent.
  3. [LHA-03] Historical/stale references are kept distinct from live runtime truth.
  4. [LHA-04] A narrow next implementation target is defined only if the audit justifies it.
  5. [LHA-05] The audit does not broaden into generic app-control architecture.
**Plans**: 2 plans

Plans:
- [ ] 18-01: Live launch/open capability audit and final classification
- [ ] 18-02: Planning/state truth update and narrow next-target definition

### Phase 19: Home Surface Copy And Affordance Polish 01
**Goal**: Tighten the current 1.0 product surface through bounded UI polish only: refined info affordances, a cleaner title/version/update presentation, dedicated update modal interaction, permissions top-bar cleanup, and final visible copy normalization.
**Depends on**: Phase 18
**Success Criteria** (what must be TRUE):
  1. [UIP-01] All visible feature/module explanation buttons use a custom small outlined circular info affordance rather than the default Android system icon.
  2. [UIP-02] The home title/version/update area feels calmer and more balanced than the current title + badge + action cluster.
  3. [UIP-03] Update interaction opens a dedicated modal/dialog/sheet that shows current version, latest version, and a state-appropriate bottom action.
  4. [UIP-04] The update modal uses a solid yellow bottom action only when an update is actually available.
  5. [UIP-05] The permissions page top-left back button is removed and the page header remains visually balanced.
  6. [UIP-06] Visible developer-ish or awkward copy in the touched surfaces is cleaned up in EN and zh-CN.
  7. [UIP-07] The project still compiles after the polish pass.
**Plans**: 3 plans

Plans:
- [x] 19-01: Custom info-affordance system and reusable style resources
- [x] 19-02: Home title/version/update redesign with dedicated update modal
- [x] 19-03: Permissions top-bar cleanup, copy cleanup, and final UI verification

### Phase 19.1: Non-root onboarding crash hardening (INSERTED)

**Goal**: Eliminate first-run onboarding crashes on newer Android non-root devices by making runtime start, accessibility-settings launch, and related bootstrap refresh paths fail visibly and recoverably instead of terminating the app process.
**Requirements** (what must be TRUE):
  1. [BOOT-01] Exact crash evidence is captured before code changes for both `Start Runtime` and `Accessibility permission` on the affected non-root fresh-install path.
  2. [BOOT-02] `Start Runtime` and `Accessibility permission` no longer crash the app process on the supported non-root onboarding path, even when the platform denies or restricts the requested action.
  3. [BOOT-03] Foreground-service start failures, settings-launch failures, missing permissions, and bootstrap/provider refresh failures are converted into observable degraded state plus bounded logs rather than uncaught exceptions.
  4. [BOOT-04] The final implementation distinguishes fact, inference, and any remaining hypothesis, and includes focused verification plus residual-risk notes for ROM or Android-version variance.
**Depends on:** Phase 19
**Plans:** 3 plans

Plans:
- [ ] 19.1-01: Reproduce onboarding crashes and classify the failing bootstrap chain
- [ ] 19.1-02: Harden onboarding entry points and bootstrap failure handling
- [ ] 19.1-03: Verify non-root recovery behavior and publish the fix report

### Phase 20: Close The Visible-But-Unreachable Gap For 1.1.0
**Goal**: Materially improve real-world semantic operability when visible Android app content is present but not reliably reachable through the current accessibility-first interaction model.
**Depends on**: Phase 19, Phase 22
**Success Criteria** (what must be TRUE):
  1. [VUG-01] `/click` and `/find` materially reduce misses caused by meaningful labels living on `contentDescription` instead of `text`.
  2. [VUG-02] `/click` responses remain truthful about requested selector surface, matched selector surface, fallback-surface use, and wrapper/ancestor resolution.
  3. [VUG-03] `/find` preserves exact-vs-contains honesty while making surface mismatch, mode mismatch, and real absence distinguishable.
  4. [VUG-04] Ghosthand gains an explicit OCR fallback path for WebView, empty-tree, or badly truncated-tree content surfaces.
  5. [VUG-05] OCR-derived output is explicitly marked with source provenance and is not silently merged into accessibility truth.
  6. [VUG-06] `/wait` separates condition satisfaction, observed state change, and timeout semantics without replacing the route.
  7. [VUG-07] No unrelated endpoint growth, generic debug mode, stable-node redesign, scroll DSL expansion, or skill-layer behavior is added as part of this phase.
  8. [VUG-08] The Android project still compiles after the phase lands.
**Plans**: TBD

Plans:
- [x] 20-01: Multi-surface semantic targeting for `/find` and `/click`
- [ ] 20-02: Explicit OCR fallback for empty-tree and WebView-heavy `/screen` reads
- [ ] 20-03: Split `/wait` outcome semantics for condition, state change, and timeout truth

### Phase 21: Agent Interaction Hardening For 1.2.0
**Goal**: Make Ghosthand materially easier and less error-prone for agents to use in real mobile workflows by hardening interaction semantics, result truth, and bounded contract discoverability.
**Depends on**: Phase 20
**Success Criteria** (what must be TRUE):
  1. [AIH-01] `/input` can separate text mutation from key dispatch so Enter-like submission no longer clears text unless explicitly requested.
  2. [AIH-02] Major action routes expose bounded effect feedback that distinguishes dispatch success from visible-state change or unknown effect.
  3. [AIH-03] Selector-driven failures, especially on `/click`, expose materially clearer bounded failure reasons without weakening selector honesty.
  4. [AIH-04] `/screen` partial-output responses become more interpretable through compact omitted-summary improvements without pretending exhaustiveness.
  5. [AIH-05] `/launch` succeeds more reliably for installed launchable packages across real device environments while keeping the route narrow and truthful.
  6. [AIH-06] Secondary cleanup items improve `/wait` discoverability, `/state` capability summary readability, and reference semantics only where they fit cleanly within scope.
  7. [AIH-07] No broad UI rewrite, report endpoint, tree rewrite, node-identity redesign, scroll DSL expansion, or skill-layer behavior is added.
  8. [AIH-08] The Android project still compiles after the phase lands.
**Plans**: TBD

Plans:
- [ ] 21-01: `/input` semantics split and explicit key-vs-text behavior
- [x] 21-02: Action-effect feedback for `/back`, `/home`, `/click`, and aligned selector failure reasons
- [ ] 21-03: `/screen` partial-output summary hardening
- [ ] 21-04: `/launch` compatibility improvements
- [ ] 21-05: Secondary contract cleanup for `/wait`, `/state`, and reference semantics if still cleanly within scope

### Phase 22: Patch Stabilization For 1.2.1
**Goal**: Improve narrow 1.2.1 agent-facing reliability and discoverability by adding OCR fallback hinting in justified accessibility-empty cases, distinguishing stale node references from true misses, and clarifying transient modal-transition accessibility drops in the live contract.
**Depends on**: Phase 21
**Success Criteria** (what must be TRUE):
  1. [PATCH-01] Accessibility-first `/screen` responses expose a bounded OCR or hybrid retry hint only when accessibility output is empty or operationally insufficient.
  2. [PATCH-02] OCR hinting does not trigger merely because `partialOutput=true`.
  3. [PATCH-03] Stale node references are distinguishable from true selector or no-match failures through a truthful failure classification path.
  4. [PATCH-04] Live contract surfaces document modal-transition accessibility drops as a transient retry-oriented operating condition instead of a terminal platform failure.
  5. [PATCH-05] No OCR automation expansion, node identity redesign, `/launch` reopening, `/tree` rewrite, or UI/front-end work is pulled into the patch.
  6. [PATCH-06] The Android project still compiles after the patch lands.
**Plans**: TBD

Plans:
- [ ] 22-01: OCR fallback discoverability hinting on accessibility-empty or accessibility-insufficient `/screen`
- [x] 22-02: Stale-node failure classification for expired snapshot references
- [ ] 22-03: Modal-transition accessibility contract clarification and narrow doc/catalog alignment

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
| 17. Agent Perspective Reconciliation 01 | 2/2 | Complete | 2026-03-30 |
| 18. Launch Handoff Audit 01 | 0/2 | Planned | - |
| 19. Home Surface Copy And Affordance Polish 01 | 3/3 | Complete | 2026-03-30 |
| 20. Close The Visible-But-Unreachable Gap For 1.1.0 | 1/3 | In Progress|  |
| 21. Agent Interaction Hardening For 1.2.0 | 1/5 | In Progress|  |
| 22. Patch Stabilization For 1.2.1 | 1/3 | In Progress|  |
