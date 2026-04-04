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
- [x] **Phase 24.1: Maintainability Convergence Review Fix Pass** - Finish the failed 1.3.1 maintainability convergence work by materially thinning `LocalApiServer` and `StateCoordinator`, enforcing real layer ownership, introducing real package/domain structure, converging test ownership, and strengthening the non-root to future-root seam without adding features. (completed 2026-04-03)
- [ ] **Phase 24.2: Maintainability Convergence Fix 02** - Finish the still-unresolved 1.3.1 convergence work by thinning `StateCoordinator` further, breaking up the new large handler/payload monoliths, and establishing canonical ownership of shared state-legibility concepts without adding features.
- [ ] **Phase 24.3: Final StateCoordinator Thinning For Maintainability Convergence** - Resolve the last strict-review blocker by turning `StateCoordinator` into a true orchestration shell while preserving the already-passing LocalApiServer/package/test/canonical-ownership gains.
- [x] **Phase 24.4: 1.3.1 Clean Closeout** - Finish the 1.3.x line cleanly by making preview actually usable, reducing hint/field noise, cleaning transitional code, and aligning the final runtime/catalog contract without adding features. (completed 2026-04-03)
- [ ] **Phase 25: Agent-Native Interface Evolution For 1.4.0** - Evolve Ghosthand from a route collection into a more agent-native substrate by adding a bounded observation plane, strengthening the capability plane, normalizing action evidence, and only adding thin intent helpers if A/B/C earn them.
- [ ] **Phase 25.1: Architecture Convergence For 1.4.0** - Converge remaining root-package ownership into real domain folders and formalize Capability Plane v2 through a first-class `/capabilities` surface without broadening 1.4.0 scope.
- [x] **Phase 25.3: Architecture Convergence Correction Pass For 1.4.0** - Correct the failed strict architecture-convergence review by materially executing root-package convergence and a first-class Capability Plane v2 without broadening 1.4.0 scope. (completed 2026-04-04)
- [x] **Phase 25.4: Clean Finish And Capability Presentation Polish For 1.4.0** - Polish the accepted 1.4.0 architecture-convergence result so the capability plane, self-description surfaces, and closeout state feel clean, coherent, and presentation-ready without broadening scope. (completed 2026-04-04)
- [x] **Phase 25.5: Current-State Correction Pass For 1.4.0** - Correct the remaining correctness defects and maintenance debt in the accepted 1.4.0 baseline by fixing `/notify` cancellation truthfulness, aligning the screenshot foreground-service contract, removing wildcard-import pollution, making contained coordinator extractions, and continuing test-package convergence without broadening scope. (completed 2026-04-04)
- [x] **Phase 25.6: Second Current-State Cleanup Pass For 1.4.0** - Finish the still-incomplete engineering cleanup by removing the remaining UI wildcard imports, doing one more contained extraction pass on the largest coordination-heavy files, and reducing the remaining root-flat tests without reopening accepted fixes or broadening scope. (completed 2026-04-05)
- [x] **Phase 26: Visual Observation Regression Correction For 1.4.1** - Correct the real screenshot regression exposed by OpenClaw usage by restoring truthful, reliable, agent-usable visual observation through `/screenshot` and lightweight preview paths without broadening 1.4.1 scope. (completed 2026-04-04)

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
- [x] 24.1-06: Converge tests by runtime behavior domain and align minimal contract surfaces

### Phase 24.2: Maintainability Convergence Fix 02
**Goal**: Complete the remaining maintainability convergence work that strict review still rejects by materially shrinking `StateCoordinator`, reducing the newly created large domain monoliths, and establishing canonical ownership of state-legibility concepts.
**Depends on**: Phase 24.1
**Success Criteria** (what must be TRUE):
  1. [FIX2-01] `StateCoordinator` is materially thinner and no longer the practical control surface for unrelated screen/state/input/preview/capability behavior.
  2. [FIX2-02] The newly created large domain files, especially action-route, read-route, and payload-support files, are materially reduced so they no longer act as replacement monoliths.
  3. [FIX2-03] Core state-legibility concepts such as `renderMode`, `surfaceReadability`, `visualAvailable`, `previewAvailable`, and `focusedEditablePresent` have one clear canonical derivation path.
  4. [FIX2-04] Package and test structure gains from 24.1 are preserved and sharpened rather than flattened or bypassed.
  5. [FIX2-05] The route/contract vs execution vs observation seam remains stronger without adding root or libsu functionality.
  6. [FIX2-06] No new product capabilities, root code, workflow steering, or unrelated expansion is introduced.
  7. [FIX2-07] The Android project still compiles after the fix pass lands.
**Plans**: TBD

Plans:
- [x] 24.2-01: Architecture-fix note and corrective traceability for the remaining convergence failures
- [x] 24.2-02: Further thin `StateCoordinator` into screen/state/preview/capability/find/execution delegates
- [ ] 24.2-03: Reduce the new large route and payload monoliths by stable behavior family
- [ ] 24.2-04: Establish canonical ownership of render/readability and related state-legibility concepts
- [ ] 24.2-05: Preserve package/test convergence and run final verification gates

### Phase 24.3: Final StateCoordinator Thinning For Maintainability Convergence
**Goal**: Finish the last strict maintainability-convergence blocker by materially thinning `StateCoordinator` until it reads as a wiring and delegation shell instead of a mega-coordinator.
**Depends on**: Phase 24.2
**Success Criteria** (what must be TRUE):
  1. [FIX3-01] `StateCoordinator` is no longer reasonably describable as a mega-coordinator and is materially smaller than its current 720-line form.
  2. [FIX3-02] Concrete state/health/info/device/foreground, screen/tree/find/focused-node, screenshot/preview, and interaction or utility delegations move to clearer collaborator-owned modules.
  3. [FIX3-03] Clipboard, notification, wait, and gesture-related accessors are reduced so the coordinator no longer acts as the default home for unrelated domain behavior.
  4. [FIX3-04] The already-passing LocalApiServer, package layout, test structure, canonical render/readability ownership, and current non-root seam remain intact.
  5. [FIX3-05] No new runtime capabilities, root/libsu work, workflow steering, or unrelated scope is introduced.
  6. [FIX3-06] The Android project still compiles after the pass lands.
**Plans**: TBD

Plans:
- [x] 24.3-01: Final architecture-fix note for StateCoordinator thinning only
- [x] 24.3-02: Move remaining state/read/preview collaborators fully out of StateCoordinator
- [ ] 24.3-03: Move remaining execution, clipboard, notification, and wait or utility delegations out of StateCoordinator
- [ ] 24.3-04: Preserve passing areas and run final verification gates

### Phase 24.4: 1.3.1 Clean Closeout
**Goal**: Close the 1.3.x line cleanly by making preview access actually usable, converging noisy and redundant hint fields, cleaning transitional code/contract rough edges, and aligning the final 1.3.1 runtime surface without expanding scope.
**Depends on**: Phase 24.3
**Success Criteria** (what must be TRUE):
  1. [CLOSE-01] Preview is actually usable when advertised, with one clear lightweight retrieval path.
  2. [CLOSE-02] Fallback and hint fields are cleaner, less redundant, and less noisy in high-frequency `/screen` flows.
  3. [CLOSE-03] Meaningless contract clutter such as obviously useless always-null fields is removed.
  4. [CLOSE-04] Project/code structure is cleaner at the end of 1.3.x than after the refactor passes, without reopening architecture-scale redesign.
  5. [CLOSE-05] Command catalog, runtime payloads, and any present contract docs align to one final 1.3.1 surface.
  6. [CLOSE-06] No 1.4.0 work, new major capabilities, workflow-planner behavior, or root/libsu work is introduced.
  7. [CLOSE-07] The Android project still compiles after the closeout lands.
**Plans**: TBD

Plans:
- [x] 24.4-01: Preview mechanism cleanup and explicit retrieval-path alignment
- [x] 24.4-02: Field and hint convergence with noise reduction
- [x] 24.4-03: Project-structure and code cleanup for the 1.3.x line
- [x] 24.4-04: Final contract/catalog/docs alignment and closeout verification

### Phase 25: Agent-Native Interface Evolution For 1.4.0
**Goal**: Evolve Ghosthand beyond pure endpoint accumulation by keeping HTTP/JSON as the stable control base while adding a bounded observation plane, a stronger machine-readable capability plane, a more coherent action evidence plane, and only a thin intent layer if it clearly earns inclusion.
**Depends on**: Phase 24.4
**Success Criteria** (what must be TRUE):
  1. [IFACE-01] A real observation plane v1 exists through a bounded pollable HTTP/JSON event route with cursor-based retrieval, compact event families, and explicit retention semantics.
  2. [IFACE-02] `/commands` and current capability or state surfaces materially reduce agent guesswork about capability domains, preconditions, dynamic availability, failure classes, and truth type without becoming encyclopedic.
  3. [IFACE-03] Major action routes expose a more coherent normalized action evidence family so agents can read performed truth, observed change, post-action state, ambiguity, and observation-mode hints faster.
  4. [IFACE-04] Any intent helpers added remain thin, generic, inspectable, subordinate to primitives, and may be explicitly deferred if A/B/C do not justify them.
  5. [IFACE-05] No root/libsu work, giant WebSocket/SSE streaming system, world-model store, app-specific heuristics, planner behavior, or endpoint sprawl is added.
  6. [IFACE-06] The Android project still compiles and the new observation, capability, and action-evidence behavior is covered by focused regression tests.
**Plans**: TBD

Plans:
- [x] 25-01: Observation plane v1 with cursor-based `/events` retrieval and bounded event retention
- [x] 25-02: Capability plane cleanup and strengthening through `/commands` and runtime capability summaries
- [x] 25-03: Action evidence normalization across the main interaction routes
- [x] 25-04: Thin intent helper decision pass with explicit deferral unless a helper clearly earns inclusion
- [x] 25-05: Final contract alignment, focused regression coverage, and build verification

### Phase 25.1: Architecture Convergence For 1.4.0
**Goal**: Finish the next 1.4.0 architecture-convergence step by materially reducing the root-package drop zone and formalizing Capability Plane v2 as a first-class substrate layer centered on definitions, dynamic availability, and capability-centric presentation.
**Depends on**: Phase 25
**Success Criteria** (what must be TRUE):
  1. [ACONV-01] The remaining root-package classes are materially converged into clear domain folders so only true top-level entrypoints remain at the root.
  2. [ACONV-02] Package placement expresses real subsystem ownership across UI, services, interaction, state, screen, preview, notification, integration, capability, catalog, server, and routes instead of relying on filename guesswork.
  3. [ACONV-03] Capability Plane v2 exists as a first-class model with clear `CapabilityDefinition`, `CapabilityAvailability`, and `CapabilityPresentation` layers rather than spread across ad hoc route metadata and state summaries.
  4. [ACONV-04] `GET /capabilities` exists and is genuinely capability-centric rather than a dumb alias of `/commands`.
  5. [ACONV-05] `/commands`, `/state`, and `/capabilities` align on one canonical capability source of truth without semantic drift.
  6. [ACONV-06] No de-scoped 1.x public capabilities, especially launch/root-related truths, are accidentally reintroduced during cleanup.
  7. [ACONV-07] No root/libsu work, observation-plane redesign, intent expansion, app-specific hacks, or unrelated 1.4.1 optimization work is added.
  8. [ACONV-08] The Android project still compiles and focused tests cover package ownership and capability-plane behavior.
**Plans**: TBD

Plans:
- [x] 25.1-01: Architecture note and root-package/capability-plane convergence map
- [x] 25.1-02: Root-package and directory convergence into domain-owned packages
- [x] 25.1-03: Capability Plane v2 definitions, availability modeling, and `/capabilities`
- [x] 25.1-04: `/commands`, `/state`, and `/capabilities` alignment plus stale/dead/truth-conflicting cleanup
- [x] 25.1-05: Ownership tests, capability-plane tests, and full verification

### Phase 25.3: Architecture Convergence Correction Pass For 1.4.0
**Goal**: Correct the failed strict 1.4.0 architecture-convergence review by materially converging the root package into real domain ownership and implementing Capability Plane v2 as a first-class capability substrate rather than partial route-centric approximations.
**Depends on**: Phase 25.1
**Success Criteria** (what must be TRUE):
  1. [ACFIX-01] The root package is materially converged so it is no longer the default ownership sink and only true top-level entrypoints remain there.
  2. [ACFIX-02] Remaining UI, service, provider, runtime-state, execution, integration, notification, and screen-owned classes move into clear domain packages with clean imports and manifest alignment where required.
  3. [ACFIX-03] Capability Plane v2 exists as a real first-class layer with canonical `CapabilityDefinition`, `CapabilityAvailability`, and `CapabilityPresentation` models backed by one capability source of truth.
  4. [ACFIX-04] `GET /capabilities` exists and is genuinely capability-centric, machine-readable, compact, and distinct from `/commands`.
  5. [ACFIX-05] `/commands`, `/state`, and `/capabilities` expose aligned meanings over the same canonical capability source without semantic drift or duplicated-but-divergent capability descriptions.
  6. [ACFIX-06] Stale, dead, or truth-conflicting root-package and capability-surface leftovers, especially launch-adjacent or other de-scoped 1.x public capability remnants, are removed or kept clearly non-public.
  7. [ACFIX-07] No root/libsu work, observation-plane redesign, intent-layer expansion, unrelated endpoint growth, or other new product features are added in the correction pass.
  8. [ACFIX-08] The Android project still compiles and focused tests cover root-package convergence, `/capabilities`, capability modeling, cross-surface alignment, and non-regression of existing runtime behavior.
**Plans**: TBD

Plans:
- [x] 25.3-01: Architecture correction note and root-package or capability-plane execution map
- [x] 25.3-02: Material root-package and directory convergence into real domain ownership
- [x] 25.3-03: Capability Plane v2 canonical model and first-class `/capabilities` route
- [x] 25.3-04: `/commands`, `/state`, and `/capabilities` alignment plus stale or truth-conflicting cleanup
- [x] 25.3-05: Focused tests, ownership verification, and final build gate

### Phase 25.4: Clean Finish And Capability Presentation Polish For 1.4.0
**Goal**: Finish 1.4.0 cleanly by polishing the accepted architecture-convergence result, reducing refactor residue, sharpening capability-plane presentation, tightening `/commands` plus `/state` plus `/capabilities` coherence, and closing out small remaining roughness without changing product direction.
**Depends on**: Phase 25.3
**Success Criteria** (what must be TRUE):
  1. [POLISH-01] The accepted 1.4.0 architecture feels cleaner and less mid-refactor through targeted cleanup of wildcard imports, import clutter, stale glue, and obvious naming roughness without reopening structural decisions.
  2. [POLISH-02] Capability-plane presentation is sharper and more coherent, with cleaner field ordering, naming, grouping, and compact capability-centric expression for agent consumption.
  3. [POLISH-03] `/commands`, `/state`, and `/capabilities` feel intentionally aligned as route-contract, runtime, and capability-plane surfaces without bloating or semantically drifting.
  4. [POLISH-04] Stale, dead, or truth-conflicting residue is further reduced and de-scoped 1.x truths remain respected.
  5. [POLISH-05] No new routes, capability-plane redesign, observation-plane work, intent-layer work, root/libsu work, broad naming migration, UI redesign, 1.4.1 optimization, or app-specific heuristics are added.
  6. [POLISH-06] The Android project still compiles and focused tests cover capability-plane presentation consistency, self-description surface coherence, and cleanup non-regression.
**Plans**: TBD

Plans:
- [x] 25.4-01: Closeout note and bounded polish map
- [x] 25.4-02: Code-cleanliness and refactor-residue cleanup
- [x] 25.4-03: Capability-plane presentation polish
- [x] 25.4-04: `/commands`, `/state`, and `/capabilities` coherence polish plus final closeout cleanup
- [x] 25.4-05: Focused tests and final 1.4.0 clean-finish verification

### Phase 25.5: Current-State Correction Pass For 1.4.0
**Goal**: Finish the current 1.4.0 baseline cleanly by correcting the remaining notification, screenshot-contract, import-cleanliness, coordinator-size, and test-ownership defects without reopening the accepted architecture direction.
**Depends on**: Phase 25.4
**Success Criteria** (what must be TRUE):
  1. [CURFIX-01] `/notify` post/cancel behavior is truthful and symmetric for actual posted notifications, including tagged notifications tracked through the in-memory notification buffer.
  2. [CURFIX-02] The screenshot or MediaProjection foreground-service contract is internally consistent across manifest declarations, runtime service flags, provider comments, and capability claims.
  3. [CURFIX-03] Wildcard imports are removed from main Kotlin source so package ownership is readable without self-package or project-wide star imports.
  4. [CURFIX-04] The worst remaining oversized coordination-heavy classes are materially cleaner through bounded helper or collaborator extraction rather than hero rewrites.
  5. [CURFIX-05] Test ownership continues to converge out of the flat root test package wherever domain ownership is clear.
  6. [CURFIX-06] The accepted 1.4.0 package and capability-plane baseline remains intact and no route, capability, or permission-model scope is broadened.
  7. [CURFIX-07] Focused regression coverage proves the notification truthfulness fix and the screenshot/service contract alignment.
  8. [CURFIX-08] The Android project still compiles and focused unit tests pass after the correction pass lands.
**Plans**: TBD

Plans:
- [x] 25.5-01: Fix `/notify` cancellation truthfulness and add regression coverage
- [x] 25.5-02: Align screenshot or MediaProjection foreground-service contract and guard against drift
- [x] 25.5-03: Remove wildcard imports from main source without behavioral churn
- [x] 25.5-04: Perform contained extractions in the remaining oversized coordination-heavy classes
- [x] 25.5-05: Continue test-package convergence, update maintainability checks, and run verification

### Phase 25.6: Second Current-State Cleanup Pass For 1.4.0
**Goal**: Finish the still-open engineering cleanup on the accepted 1.4.0 baseline by removing the remaining wildcard imports in UI-owned main source, doing one more contained extraction pass on the biggest coordination-heavy files, and further reducing root-flat tests without touching already accepted fixes.
**Depends on**: Phase 25.5
**Success Criteria** (what must be TRUE):
  1. [CURFIX2-01] `app/src/main/java` contains zero wildcard imports, including the remaining UI-package star imports and any self-package star imports.
  2. [CURFIX2-02] `GhostCoreAccessibilityService.kt`, `GhostAccessibilityService.kt`, and `StateCoordinator.kt` are materially cleaner through contained helper or collaborator extraction rather than another architecture rewrite.
  3. [CURFIX2-03] Optional follow-up cleanup in `RuntimeStateStore.kt` or `GhosthandCommandCatalogRoutes.kt` happens only if it is clearly safe and materially useful.
  4. [CURFIX2-04] Root-flat tests are reduced further so only truly shared or cross-domain tests remain at `app/src/test/java/com/folklore25/ghosthand`.
  5. [CURFIX2-05] Touched tests also have clean import hygiene and preserve existing test behavior.
  6. [CURFIX2-06] Already accepted `/notify` and screenshot or MediaProjection fixes remain untouched unless a tiny build-driven follow-up is strictly required.
  7. [CURFIX2-07] No new routes, contracts, capabilities, presentation polish, or speculative package reshuffling is introduced.
  8. [CURFIX2-08] The Android project still builds and unit tests pass after the cleanup closes.
**Plans**: TBD

Plans:
- [x] 25.6-01: Remove the remaining wildcard imports from UI-owned main source to reach zero total wildcard imports
- [x] 25.6-02: Perform one more contained extraction pass on the biggest coordination-heavy files
- [x] 25.6-03: Continue root-flat test convergence and clean touched test imports
- [x] 25.6-04: Run final build and unit-test verification and close the cleanup pass

### Phase 26: Visual Observation Regression Correction For 1.4.1
**Goal**: Restore a truthful and agent-usable visual observation path by correcting the 1.4.0 screenshot regression, tightening preview or downsample quality, and making screenshot failure semantics operationally specific without broadening product scope.
**Depends on**: Phase 25.6
**Success Criteria** (what must be TRUE):
  1. [VIS-01] Full-resolution screenshot works again in the normal supported case, or Ghosthand now reports a precise truthful reason why it cannot.
  2. [VIS-02] Reduced-size screenshot or preview output remains lightweight but is materially usable for agent visual reasoning rather than trivially low quality.
  3. [VIS-03] Empty-image success cases are eliminated so Ghosthand never reports visual success with zero-byte or blank image payloads.
  4. [VIS-04] Screenshot failure responses distinguish materially different causes such as unavailable capability, missing session, inactive projection, bitmap acquisition failure, encode failure, empty output, invalid requested dimensions, or unsupported mode where the runtime can know that truth.
  5. [VIS-05] Observation-plane truthfulness is preserved: Ghosthand does not fake success through silent degradation or misleading fallback behavior.
  6. [VIS-06] The fix stays bounded to screenshot and closely related visual-observation contract paths, without reopening action evidence, capability-plane design, or unrelated 1.4.1 work.
  7. [VIS-07] Real-device and local verification prove the corrected screenshot path and the Android project still compiles after the pass lands.
**Plans**: TBD

Plans:
- [x] 26-01: Trace the screenshot regression end-to-end and lock the exact failing path
- [x] 26-02: Restore truthful full-resolution capture and eliminate empty-image success cases
- [x] 26-03: Raise lightweight preview or downsample output to a decision-usable floor with truthful metadata
- [x] 26-04: Tighten screenshot failure classification, regression coverage, and real-device verification
