# Roadmap: Ghosthand

## Overview

Ghosthand is moving from an accepted runtime and operator-validation baseline into a more legible, trustworthy mobile-agent substrate.
The near-term roadmap keeps the platform truthful while improving how agents read state, trust action outcomes, and choose the right observation mode without drifting into workflow steering.

## Phases

**Phase Numbering:**
- Integer phases are planned milestone work.
- Decimal phases are urgent inserted work.

- [ ] **Phase 20: Close The Visible-But-Unreachable Gap For 1.1.0** - Improve semantic reachability across text/contentDescription surfaces, add explicit OCR fallback for empty-tree content surfaces, and split `/wait` outcome semantics without broadening the platform.
- [ ] **Phase 21: Agent Interaction Hardening For 1.2.0** - Reduce avoidable agent friction by hardening input semantics, action-effect feedback, selector failure reasons, `/screen` partial-output summaries, and `/launch` reliability without broadening the platform.
- [ ] **Phase 22: Patch Stabilization For 1.2.1** - Land the narrow 1.2.1 patch scope around OCR fallback discoverability, stale-node failure classification, and modal-transition accessibility guidance without reopening 1.2 feature work.
- [ ] **Phase 23: State Legibility Mainline For 1.3.0** - Make Ghosthand easier for agents to trust between actions through post-action state summaries, `/screen` summary mode, clearer render/readability signals, and lightweight visual preview access.
- [ ] **Phase 24: Maintainability Convergence For 1.3.1** - Refactor the 1.x runtime into cleaner maintainable modules, converge state and contract layers, normalize vocabulary, strengthen test ownership, and prepare a clean future 2.0 root-plane seam without implementing root functionality.
- [ ] **Phase 24.1: Maintainability Convergence Review Fix Pass** - Finish the failed 1.3.1 maintainability convergence work by materially thinning `LocalApiServer` and `StateCoordinator`, enforcing real layer ownership, introducing real package/domain structure, converging test ownership, and strengthening the non-root to future-root seam without adding features.

## Phase Details

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
- [ ] 20-01: Multi-surface semantic targeting for `/find` and `/click`
- [ ] 20-02: Explicit OCR fallback for empty-tree and truncated-tree surfaces
- [ ] 20-03: `/wait` outcome semantic split

### Phase 21: Agent Interaction Hardening For 1.2.0
**Goal**: Reduce avoidable agent friction by hardening input semantics, action-effect feedback, selector failure reasons, and `/screen` partial-output interpretation without broadening the platform.
**Depends on**: Phase 20
**Success Criteria** (what must be TRUE):
  1. [AIH-01] `/input` can separate text mutation from key dispatch so Enter-like submission no longer clears text unless explicitly requested.
  2. [AIH-02] Major action routes expose bounded effect feedback that distinguishes dispatch success from visible-state change or unknown effect.
  3. [AIH-03] Selector-driven failures, especially on `/click`, expose materially clearer bounded failure reasons without weakening selector honesty.
  4. [AIH-04] `/screen` partial-output responses become more interpretable through compact omitted-summary improvements without pretending exhaustiveness.
  5. [AIH-05] Secondary cleanup items improve `/wait` discoverability, `/state` capability summary readability, and reference semantics only where they fit cleanly.
  6. [AIH-06] No broad UI rewrite, report endpoint, tree rewrite, node-identity redesign, scroll DSL expansion, or skill-layer behavior is added.
  7. [AIH-07] The Android project still compiles after the phase lands.
**Plans**: TBD

Plans:
- [ ] 21-01: `/input` semantics split and explicit key-vs-text behavior
- [ ] 21-02: Action-effect feedback and selector failure reason hardening
- [ ] 21-03: `/screen` partial-output summary hardening
- [ ] 21-04: Secondary contract cleanup where still in scope

### Phase 22: Patch Stabilization For 1.2.1
**Goal**: Improve narrow 1.2.1 agent-facing reliability and discoverability by adding OCR fallback hinting in justified accessibility-empty or accessibility-insufficient cases, distinguishing stale node references from true misses, and clarifying transient modal-transition accessibility drops in the live contract.
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
- [ ] 22-01: OCR fallback discoverability hinting
- [ ] 22-02: Stale `/click` node-reference classification
- [ ] 22-03: Contract and docs clarification for modal-transition retry guidance

### Phase 23: State Legibility Mainline For 1.3.0
**Goal**: Make Ghosthand easier for agents to trust between actions by improving post-action state legibility, compact surface summaries, render/readability signaling, and lightweight visual confirmation without turning the platform into a workflow planner.
**Depends on**: Phase 22
**Success Criteria** (what must be TRUE):
  1. [LEG-01] Major action routes expose a truthful, compact post-action state summary that materially reduces immediate follow-up orientation calls.
  2. [LEG-02] `/screen` offers a lightweight summary mode for low-cost orientation without replacing the full structured route.
  3. [LEG-03] Render mode, surface readability, and bounded fallback hints make it easier to choose between accessibility, OCR/hybrid, and lightweight visual confirmation.
  4. [LEG-04] Lightweight visual preview access exists without embedding full screenshot payloads by default in normal `/screen` responses.
  5. [LEG-05] No action recommendation engine, task-specific choreography, app-specific heuristics, launch reopening, node-identity redesign, or workflow-planner behavior is added.
  6. [LEG-06] The Android project still compiles after the phase lands.
**Plans**: TBD

Plans:
- [ ] 23-01: Post-action state summary for key interaction routes
- [ ] 23-02: `/screen` summary mode for low-cost orientation
- [ ] 23-03: Render-mode, readability, and fallback signal standardization
- [ ] 23-04: Lightweight visual preview access without full screenshot bloat

### Phase 24: Maintainability Convergence For 1.3.1
**Goal**: Refactor Ghosthand 1.x into a cleaner, more maintainable substrate architecture while preserving current behavior, clarifying state and contract layering, and opening a clean architecture seam for a future 2.0 root-backed line without implementing it now.
**Depends on**: Phase 23
**Success Criteria** (what must be TRUE):
  1. [MAIN-01] The current oversized core files are materially decomposed into clearer runtime ownership areas instead of continuing to centralize route, state, payload, and catalog logic.
  2. [MAIN-02] Action effect, post-action summary, `/screen` summary, full `/screen`, and disclosure or fallback layers have explicit non-overlapping responsibilities with reduced duplication and drift.
  3. [MAIN-03] Agent-facing vocabulary is normalized so one concept keeps one canonical name and semantics across runtime, catalog, tests, and minimal contract docs.
  4. [MAIN-04] The test suite is easier to navigate by behavior domain and preserves or improves confidence through the refactor.
  5. [MAIN-05] The runtime gains a clean present-value seam between route or contract logic and execution or observation backends that prepares for a future 2.0 root-backed line without adding root functionality now.
  6. [MAIN-06] No new runtime capabilities, app-specific heuristics, root codepaths, launch reopening, workflow steering, or broad UI or docs expansion is pulled into the phase.
  7. [MAIN-07] The Android project still compiles after the refactor lands.
**Plans**: TBD

Plans:
- [x] 24-01: Architecture note and bounded refactor sequence for 1.3.1
- [x] 24-02: Decompose `LocalApiServer` and `StateCoordinator` by runtime domain ownership
- [x] 24-03: Decompose `GhosthandApiPayloads` and `GhosthandCommandCatalog` by contract ownership
- [ ] 24-04: Converge action effect, state summary, screen summary, full screen, and disclosure layers
- [ ] 24-05: Normalize vocabulary and prepare the non-root to future-root execution seam
- [ ] 24-06: Converge test structure and perform minimal contract alignment

### Phase 24.1: Maintainability Convergence Review Fix Pass
**Goal**: Complete the maintainability convergence work that the initial 1.3.1 refactor started but did not finish, with strict focus on the rejected architecture failures rather than new feature work.
**Depends on**: Phase 24
**Success Criteria** (what must be TRUE):
  1. [FIX-01] `LocalApiServer` is materially reduced to a smaller orchestration shell, with concrete route handling and related response/disclosure responsibilities moved into domain-owned handlers.
  2. [FIX-02] `StateCoordinator` becomes a materially thinner composition layer, with screen/state/preview/capability responsibilities moved into real helpers or modules instead of remaining in one giant coordinator.
  3. [FIX-03] Action effect, post-action summary, `/screen` summary, full `/screen`, and hint/disclosure/fallback responsibilities are structurally clear and no longer read like overlapping summary variants.
  4. [FIX-04] Runtime code is no longer effectively one flat package; package structure expresses real ownership such as server, routes, payload, screen, state, interaction, catalog, preview, wait, or capability domains where useful.
  5. [FIX-05] Test ownership is materially converged by behavior domain rather than remaining a mostly flat directory of unrelated runtime tests.
  6. [FIX-06] The present-value seam between route/contract logic, execution logic, and observation/read logic is strong enough that a future root-backed plane could land cleanly without speculative root implementation now.
  7. [FIX-07] No new product capabilities, root implementation, workflow steering, app-specific heuristics, or unrelated feature work is added.
  8. [FIX-08] The Android project still compiles after the fix pass lands.
**Plans**: TBD

Plans:
- [x] 24.1-01: Architecture-fix note and risk-controlled corrective sequence
- [x] 24.1-02: Thin `LocalApiServer` into route-domain handlers and server orchestration only
- [x] 24.1-03: Thin `StateCoordinator` into domain composition modules
- [x] 24.1-04: Enforce state/summary/hint/preview layer ownership boundaries
- [x] 24.1-05: Introduce real package/domain structure and strengthen the execution/observation seam
- [ ] 24.1-06: Converge tests by runtime behavior domain and align minimal contract surfaces
