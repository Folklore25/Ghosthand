# Ghosthand Runtime Verification Workflow

## Purpose

This is the project-facing runtime verification workflow for Ghosthand.

It describes how to verify the current build on the device in a repeatable way.
Project progression and acceptance state still belong in `.planning/`.

For the accepted Stage 4 operator playbooks, use:

- `.planning/phases/10-agent-integration-scenario-hardening/STAGE4-OPERATOR-PLAYBOOKS.md`

For Stage 5 operator execution discipline, use:

- `.planning/phases/10-agent-integration-scenario-hardening/STAGE5-OPERATOR-RUNBOOK.md`

For Stage 6 operator evaluation and comparison discipline, use:

- `.planning/phases/10-agent-integration-scenario-hardening/STAGE6-OPERATOR-EVALUATION.md`

For Stage 7 operator acceptance auditing and repo-truth promotion, use:

- `.planning/phases/10-agent-integration-scenario-hardening/STAGE7-ACCEPTANCE-AUDIT.md`

For Stage 8 operator evidence packaging and archival discipline, use:

- `.planning/phases/10-agent-integration-scenario-hardening/STAGE8-EVIDENCE-PACKAGING.md`
- `.planning/phases/10-agent-integration-scenario-hardening/EVIDENCE-BUNDLE-TEMPLATE.json`

For Stage 9 operator evidence indexing and milestone summary discipline, use:

- `.planning/phases/10-agent-integration-scenario-hardening/STAGE9-EVIDENCE-INDEXING.md`
- `.planning/phases/10-agent-integration-scenario-hardening/EVIDENCE-INDEX-TEMPLATE.json`
- `.planning/phases/10-agent-integration-scenario-hardening/PHASE10-MILESTONE-SUMMARY-TEMPLATE.md`

For Phase 11 screenshot-first operator remediation, use:

- `.planning/phases/11-product-friction-remediation/.continue-here.md`

## Entry Point

Use:

```bash
scripts/ghosthand-verify-runtime.sh
```

## Core Commands

### `install-current-build`

Streams the local debug APK into `pm install -r -S` on the device.

### `restore-runtime`

Restores the foreground service and accessibility binding after install or runtime drift.
On this ROM, the stable path may also require bringing Ghosthand to foreground and using the app-owned runtime start control before `/ping` is available.

### `smoke`

Quick health check:
- `/ping`
- `/foreground`
- `/commands`

### `core`

High-value interaction chain:
- restore runtime
- smoke
- `/commands` schema check
- `tree -> find -> click`
- focused + input
- selector click
- clipboard
- swipe

Stops at the first failing step and prints that failure point.

### `focused-input`

Targeted check for:
- `/focused`
- `/input`

### `selector-click`

Targeted check for `/click` by:
- text
- resource id
- content description

Treat content description as a normal primary selector path, not a fallback behind text.

### `tree-find-click`

Targeted end-to-end chain check for:
- current `/tree` package alignment
- `/find` locating a known Ghosthand target
- `/click` performing against that selector

### `screenshot-check`

Targeted check for `/screenshot`.

Use this first when structured surface output is questionable.

### `notify-check`

Targeted check for `/notify` post/read.

### `commands-schema-check`

Targeted runtime check that `/commands` still exposes the expected schema fields.

### `wait-home`

Device-shell orchestration for `/wait` followed by `/home`.

### `scenario-settings-search-back`

Scenario 01:
- launch Android Settings
- locate the search field
- focus it
- input text
- enter a stable result
- back out
- confirm both intermediate and terminal UI changes via `/wait`

OpenClaw validation note for this device:
- accepted on the operator-validation path
- `/click` with a launcher `desc` selector is accepted on the known operator path after the transport fix
- `/wait.changed` can remain `false` even when package/activity/foreground confirm the expected transition, so those fields are currently more trustworthy than `changed` alone

### `scenario-settings-home-screenshot`

Scenario 02:
- launch Android Settings
- capture a baseline screenshot
- execute `/home`
- use `/wait` to confirm the final launcher state
- capture a second screenshot
- confirm success by screenshot evidence, not just endpoint success

Screenshot confirmation rule:
- the post-transition screenshot must contain valid PNG image data
- the post-transition screenshot payload must be materially non-empty
- the post-transition screenshot payload hash must differ from the baseline Settings screenshot payload hash
- this screenshot confirmation is only accepted after `/wait` has already resolved the expected launcher package and activity
- on this ROM, `packageName` and `activity` from `/wait` are the stronger transition truth source than `changed` alone

Follow-up hardening note:
- this is accepted with payload-hash confirmation; later hardening can still move to decoded-image comparison if needed

### `scenario-settings-clipboard-input`

Scenario 03:
- write a known clipboard value
- read it back through `/clipboard`
- focus the Settings search field
- apply `/input` using the clipboard-driven value
- confirm the focused field text matches the expected clipboard value

Acceptance rule:
- `/clipboard` write must report success
- `/clipboard` read must return the expected text
- the focused field must be the Settings search input before `/input`
- `/input` must report the clipboard-driven text
- `/focused` after input must still expose the search field and its text must match the clipboard-driven value

### `scenario-notification-navigation`

Scenario 04:
- move to Home so Ghosthand is not already foreground
- post a unique Ghosthand notification
- confirm that exact notification through `GET /notify`
- expand the notification shade
- open the MIUI unimportant-notifications bucket if the unique notification is hidden there
- locate that exact notification by its unique text
- click it
- confirm the transition with `/wait`
- confirm Ghosthand is foreground afterward

Acceptance rule:
- `/notify` must return the unique notification text
- the buffered notification entry must match Ghosthand package and the expected tag
- the notification must also be locatable in the expanded shade by that same unique text
- if the initial expanded shade only exposes the MIUI unimportant-notifications bucket, the runner may open that bucket first and then require the same unique text to become visible
- `/click` must act on that exact unique text target
- `/wait` must confirm a UI change after the notification click
- `/foreground` after the click must resolve to `com.folklore25.ghosthand`

### `full`

Runs:
1. install current build
2. restore runtime
3. core
4. screenshot-check
5. notify-check
6. wait-home

## Current Rules

- Prefer device-shell orchestration over host-side timing-sensitive verification.
- Treat root here as a testing recovery aid, not the product baseline.
- Treat `/commands` as the runtime contract source for agents.
- Treat Ghosthand as a substrate first: prefer additive truthful exposure over capability-reducing workflow shortcuts.
- Treat `/screen` as the structured actionable surface route for selector planning and geometry.
- Treat `/screenshot` as the visual-truth route for debugging and verification.
- Treat `/foreground` as observer context, not the sole visible-surface truth source.
- Treat `text`, `contentDesc`, and `resourceId` as the normal primary selector set.
- Treat `desc` as the operator-facing alias for `contentDesc`.
- When the meaningful actionable label lives in content description, prefer `desc` over forcing text-first reasoning.
- Treat `nodeId` as snapshot-ephemeral and same-snapshot only.
- Treat selector-based re-resolution as the normal reliable interaction model after UI changes.
- Treat `/screen.warnings` and `/screen.omittedInvalidBoundsCount` as trust signals for geometry problems on complex surfaces.
- Treat `/tree.invalidBoundsCount` and per-node `boundsValid` / `actionableBounds` as diagnostic truth when geometry looks suspicious.
- Treat `/screen.foregroundStableDuringCapture` and `/tree.foregroundStableDuringCapture` as freshness trust signals.
- Treat `/screen.omittedLowSignalCount` and `/tree.lowSignalCount` as readability/noise signals on complex surfaces.
- Treat `/screen.partialOutput`, `/screen.candidateNodeCount`, `/screen.returnedElementCount`, and `/screen.omittedNodeCount` as truncation/reduction signals.
- Treat `/tree.partialOutput` and `/tree.returnedNodeCount` as explicit structure-completeness signals.
- Treat `/wait.changed` as "transition observed during the wait window"; treat `/wait.packageName`, `/wait.activity`, and `/wait.snapshotToken` as the final settled state.
- Treat `/wait` as the standard post-action settle path after visible-state-changing actions.
- Treat the Stage 4 operator playbook document as the canonical prompt/template layer for accepted OpenClaw scenarios.
- Treat the Stage 5 operator runbook document as the canonical execution-discipline and report-normalization layer.
- Treat the Stage 6 operator evaluation guide as the canonical grading and cross-run comparison layer.
- Treat the Stage 7 acceptance-audit guide as the canonical review and repo-truth promotion layer.
- Treat the Stage 8 evidence-packaging guide and template as the canonical archival layer for future operator runs.
- Treat the Stage 9 indexing guide and templates as the canonical discovery and milestone-summary layer.
- Treat the summary matrix as authoritative for the first narrow failing step.
- Prefer scenario runners for Phase 10 validation once endpoint-level acceptance is already stable.

## Platform vs Workflow Boundary

Keep this split explicit during Phase 11:

- platform layer:
  - expose truthful capability, node properties, relationships, trust signals, and bounded heuristic behavior
  - keep shaped output additive rather than replacing structural truth
  - keep Phase 10 in the Ghosthand main repo as the canonical operator-validation and evidence framework
  - keep Phase 11 in the Ghosthand main repo as the canonical platform-remediation layer
- skill / playbook layer:
  - choose defaults, escalation order, and reporting discipline
  - carry more task-specific steering so the platform itself stays general
  - future `ghosthand-skill` repository should own prompting, selector-choice discipline, operator steering, task-specific workflow discipline, and ClawHub-facing packaging
  - the future skill repo does not replace Phase 10 or absorb the main repo's evidence framework

Do not narrow the platform surface merely because a playbook can get a higher immediate success rate from a more curated path.

## Forward Rules

For future remediation work:

1. prefer additive exposure over subtractive hiding
2. keep heuristics bounded, inspectable, and non-destructive
3. if Ghosthand shapes output for readability or operator speed, keep a truthful structural route accessible
4. prefer exposing node/action relationships more faithfully over silently guessing them
5. use skills and playbooks for steering before weakening the runtime substrate itself
6. once wrapper-driven behavior is inspectable, prefer improving selector-surface understanding in skills before adding broader platform heuristics

## Screenshot-First Debugging Rule

When the structured surface is questionable, capture `/screenshot` before guessing:

- if `/screen` looks stale relative to the expected app or activity
- if bounds look invalid, negative, or obviously unusable
- if `/tree` or `/screen` is too deep or noisy to interpret confidently
- if an action/result appears ambiguous from structured output alone

Interpretation order for zero-context operator use:

1. use `/screen` for structured actionable planning
2. use `/foreground` only as observer context
3. use `/screenshot` as the default debugging and verification fallback when structured output is suspect

## Post-Action Settle Discipline

For operator use, prefer `/wait` over arbitrary fixed sleeps after visible-state-changing actions:

1. after `tap`, `click`, `back`, `home`, `recents`, or similar UI-changing actions, call `/wait` first
2. interpret `/wait.changed` only as "transition observed during the wait window"
3. interpret `/wait.packageName`, `/wait.activity`, and `/wait.snapshotToken` as the final settled state
4. if `changed = false` but the final settled state is the expected target, treat the settle as successful rather than falling back immediately to sleep-based guessing
5. use a fixed sleep only as a last resort when `/wait` cannot express the relevant settle condition

Same-activity interpretation rule:

- if the surface uses one long-lived activity, `packageName` and `activity` may remain unchanged across real content transitions
- in that case, `changed = false` is not a failure by itself
- operators should then confirm the content transition with a subsequent `/screen`
- do not default back to arbitrary fixed sleep if final `/wait` state plus `/screen` already confirm the expected new content

Fixed-sleep exception rule:

- allowed only when `/wait` cannot express the relevant settle condition
- must be bounded and short
- must be reported explicitly as a caveat in the operator result
- must not replace `/wait` as the normal settle path

## Coordinate Fallback Discipline

Coordinate fallback is an exception path, not the default interaction model.

Use coordinate fallback only when all of these are true:

1. the target is specific enough on `/screen` or `/screenshot` to justify a bounded tap region
2. selector/action paths were attempted or ruled out for a concrete reason
3. the fallback tap is not blind exploration
4. `/wait` or equivalent settled-state evidence is still used after the action

Minimum justification to allow coordinate fallback:

- which selector/action path was insufficient
- which truth source justified the coordinates:
  - `/screen`
  - `/screenshot`
  - or both
- why the target region was specific enough

Minimum reporting when coordinate fallback is used:

- why selector/action path was insufficient
- what coordinates were used
- what truth source justified them
- what post-action settled-state evidence confirmed the result

Discouraged or forbidden uses:

- blind exploratory tapping
- repeated unguided tapping
- replacing selector paths when selector paths are already good enough

## Selector-To-Action Escalation Discipline

For desc-heavy or container-driven surfaces, use this escalation ladder instead of improvising:

1. try the strongest direct selector action first
   - prefer the best available primary selector:
     - `text`
     - `desc` / `contentDesc`
     - `id` / `resourceId`
2. if direct selector action fails in a way that matches the current surface structure, re-resolve with `/find`
   - prefer `/find` using the best available primary selector for that surface
3. if `/find` returns a trustworthy actionable target, use that target directly when possible
4. if `/find` returns a broad or parental target and direct actionability is still insufficient, coordinate fallback becomes allowed as an exception path
5. confirm the result with `/screen`, and use `/wait` as the preferred settle method where applicable

Interpretation rule:

- a failed `/click(text)` on a desc-heavy surface is not automatically a Ghosthand truth failure
- it is often a signal to escalate to `/find(desc)` or another stronger selector path before considering coordinate fallback
- on nested-text surfaces, selector-based `/click` now tries to resolve to the actionable clickable target by default before escalation is needed
- on text/desc surfaces, selector-based `/click` now also retries a bounded contains-based match before failing, which reduces brittleness on truncated feed/card labels

## Swipe Discoverability Discipline

For zero-context operator use:

1. canonical `/swipe` request shape is:
   - `from: {x, y}`
   - `to: {x, y}`
   - `durationMs`
2. `x1/y1/x2/y2` is accepted as an alias form for discoverability
3. do not guess between incompatible swipe shapes when `/commands` or docs already expose the canonical model
4. use `contentChanged` as the primary same-activity effect signal after the swipe

## Scroll Effect Discipline

For `/scroll`:

1. `performed = true` means the gesture dispatched, not necessarily that content advanced
2. use `contentChanged` as the primary effect signal
3. use `surfaceChanged` and before/after snapshot tokens as supporting detail
4. if `contentChanged = false`, do not assume the scroll meaningfully changed the visible content
5. on same-activity surfaces, a changed snapshot token is stronger than `changed = false` from `/wait`
6. follow with `/screen` and `/wait` where applicable before concluding that content advanced or that no more content exists

## Selector Discipline

For zero-context operator use:

1. inspect `/screen` for meaningful `text`, `desc`, and `id` values
2. choose the selector whose field actually carries the actionable meaning
3. use `index` only when one meaningful selector returns multiple matches

Do not default to text-only selector reasoning when the actionable label is exposed via `desc` / `contentDesc`.

## Stable Reference Discipline

For zero-context operator use:

1. treat `nodeId` as a bounded same-snapshot aid only
2. after a UI change, re-read `/screen` or re-run `/find` before acting again
3. if a post-change action fails with `NODE_NOT_FOUND`, rule out stale node reuse before escalating

Do not carry a `nodeId` across surface changes as if it were a stable handle.

## Geometry Trust Discipline

For complex surfaces:

1. treat `/screen` as actionable-only output, not exhaustive output
2. if `/screen.warnings` is non-empty or `omittedInvalidBoundsCount > 0`, assume some nodes had unusable geometry
3. use `/tree` to inspect flagged nodes through `boundsValid` and `actionableBounds`
4. use `/screenshot` if geometry caveats still leave the visible surface ambiguous

## Readability / Noise Discipline

For complex surfaces:

1. treat `/screen` as low-noise actionable output, not a full dump of every structural node
2. if `omittedLowSignalCount > 0`, assume Ghosthand suppressed low-information structural noise to keep the surface readable
3. use `/tree.lowSignalCount` and per-node `lowSignal` to inspect where deep container noise still exists
4. do not mistake a large raw tree for a better operator view if most of it is low-signal structure
5. do not assume suppressed low-signal nodes included unlabeled actionable nodes; `/screen` now retains actionable structure even when labels are absent

## Partial Output Discipline

For `/screen` and `/tree`:

1. if `/screen.partialOutput = true`, treat `/screen` as a reduced actionable subset rather than an exhaustive structured surface
2. use `candidateNodeCount`, `returnedElementCount`, and `omittedNodeCount` to judge how much reduction happened
3. do not conclude “no more content” or “no target exists” from `/screen` alone when partial output is signaled
4. use `/tree` when you need the fuller structural view
5. if `/tree.partialOutput = false`, treat the current tree payload as structurally full even when trust warnings still apply

## Freshness Trust Discipline

For `/screen` and `/tree`:

1. prefer captures where `foregroundStableDuringCapture = true`
2. if `foregroundStableDuringCapture = false`, treat the payload as a best-effort final attempt rather than fully fresh truth
3. if warnings include foreground drift or package mismatch, re-read `/screen` or fall back to `/screenshot` before trusting the structured surface for action planning

When screenshot fallback is used, record it explicitly in the run report:

- which structured output looked questionable
- that `/screenshot` was captured as visual confirmation
- whether the screenshot clarified the visible surface or action result

## Caveat

On this ROM, repeated reinstalls may require explicit accessibility rebinding through secure settings during testing.
Clipboard verification is runner-scoped through the Ghosthand-foreground path used on the target device.
