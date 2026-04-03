# Phase 24 Architecture Note — Maintainability Convergence For 1.3.1

## Purpose

Reduce runtime entropy in Ghosthand 1.x without changing the product line.
This note fixes the target decomposition, layer responsibilities, naming direction, and future 2.0 seam before the refactor starts.

## Decomposition Targets

### 1. LocalApiServer

Split current ownership into:
- protocol parsing and request validation
- route registration and route dispatch
- action-route handlers
- input-route handlers
- read-route handlers
- wait-route handlers
- navigation or system-route handlers
- response envelope and disclosure helpers

Target rule:
route behavior should no longer require editing one giant server file just to add or align a field.

### 2. StateCoordinator

Split current ownership into:
- action observation and effect derivation
- post-action state summary derivation
- full-screen read derivation
- screen-summary derivation
- OCR or hybrid read composition helpers
- preview metadata and preview access helpers
- capability and permission composition

Target rule:
observation, state summary, screen shaping inputs, and capability composition should have distinct homes.

### 3. GhosthandApiPayloads

Split current ownership into:
- request models and request parsing helpers
- action-effect payload shaping
- post-action summary payload shaping
- full-screen payload shaping
- screen-summary payload shaping
- disclosure, retry, and fallback helpers
- preview payload helpers

Target rule:
one payload layer should not silently own every other payload layer.

### 4. GhosthandCommandCatalog

Split current ownership into:
- shared param and field descriptors
- route descriptor definitions by domain
- route registry composition
- schema metadata and versioning

Target rule:
route metadata should be composable without copy-editing a long flat list.

## State And Contract Layer Responsibilities

These layers stay separate:

### Action effect
Answers:
- what changed around this action
- what was observed before and after

Canonical examples:
- `stateChanged`
- `beforeSnapshotToken`
- `afterSnapshotToken`
- `finalPackageName`
- `finalActivity`

### Post-action state summary
Answers:
- after this action, what state am I now in?

Canonical examples:
- `packageName`
- `activity`
- `snapshotToken`
- `focusedEditablePresent`
- `keyboardVisible`
- `renderMode`
- `surfaceReadability`
- `visualAvailable`
- `previewAvailable`

### Screen summary
Answers:
- what is the current surface like in lightweight form?

Canonical examples:
- lightweight orientation and readability fields
- compact element counts and omission summary
- fallback and preview availability signals

### Full screen
Answers:
- what is the full structured current surface?

Canonical examples:
- `elements`
- source provenance
- full omission and disclosure fields

### Disclosure, retry, and fallback
Answers:
- what misunderstanding is most likely?
- what observation-mode retry is justified?

Canonical examples:
- `disclosure`
- `retryHint`
- `suggestedFallback`
- `fallbackReason`

Constraint:
do not merge these layers into one blob, and do not publish the same concept under different meanings.

## Naming Normalization Plan

Canonical concept names to preserve or normalize around:
- `renderMode`
- `surfaceReadability`
- `visualAvailable`
- `previewAvailable`
- `suggestedFallback`
- `fallbackReason`
- `stateChanged`
- `conditionMet`
- `timedOut`
- `focusedEditablePresent`

Rules:
- one concept gets one canonical name
- a field name must match its actual semantics
- if a duplicate concept exists under another name, remove or align it
- if a field cannot keep the same semantics across layers, it should not reuse the same name

Specific correction target:
- `focusedEditablePresent` must mean a focused editable is present, not merely that any editable exists

## Future 2.0 Seam Strategy

Prepare a clean architecture seam between:
- route or contract logic
- execution backends
- observation backends

What this should enable later:
- adding a root-backed execution plane without cutting through route, payload, and contract logic again

What this phase must not add:
- libsu
- root-only codepaths
- shell wrappers
- fake root flags
- placeholder providers with no current value

Present-value principle:
only add an interface or boundary if it already reduces today’s entanglement in 1.x.

## Refactor Sequence

1. Split `LocalApiServer` and `StateCoordinator` first.
2. Split `GhosthandApiPayloads` and `GhosthandCommandCatalog` second.
3. Converge state or summary layer ownership after those new homes exist.
4. Normalize vocabulary only after ownership is clear.
5. Converge tests last so they mirror the final runtime structure.

## Risk Controls

- preserve existing runtime behavior unless a semantic correction is required for truthfulness
- keep compile and focused test gates after each wave
- avoid broad package churn with no ownership benefit
- do not introduce new public endpoints or capabilities during the refactor
- keep docs alignment minimal and contract-truthful only
