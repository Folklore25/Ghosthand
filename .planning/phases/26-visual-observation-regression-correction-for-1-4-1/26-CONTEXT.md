# Phase 26: Visual Observation Regression Correction For 1.4.1 - Context

**Gathered:** 2026-04-05
**Status:** Ready for research and planning
**Source:** OpenClaw real-usage correction brief

<domain>
## Phase Boundary

This phase corrects the regressed screenshot and preview observation path in Ghosthand 1.4.0 so 1.4.1 restores truthful, agent-usable visual evidence.

In scope:
- full-resolution screenshot regression investigation and correction
- lightweight preview or downsample quality restoration to a decision-usable floor
- elimination of empty-image success cases
- more precise screenshot failure semantics where the runtime can know the cause
- narrow contract clarifications directly required by the screenshot fix

Out of scope:
- action-evidence redesign
- capability-plane redesign
- broad observation-plane redesign
- unrelated 1.4.1 feature work
- presentation polish

</domain>

<decisions>
## Implementation Decisions

### Locked Decisions
- Treat this as an observation-plane correctness issue, not a cosmetic screenshot issue.
- Do not solve the problem by silently forcing tiny degraded images while reporting success.
- Restore reliable full-resolution capture in the normal supported case if platform and runtime conditions allow it.
- If capture cannot be produced, return a truthful and more specific failure than generic `NO_SCREENSHOT` where the runtime has that information.
- Preview or downsample output may remain lighter than full capture, but it must be materially useful for agent visual reasoning.
- Empty-image success cases must be eliminated.
- Keep the pass bounded to screenshot and directly related visual-observation contract paths.

### The agent's Discretion
- Exact helper or collaborator extraction shape inside the screenshot path
- Exact lightweight preview dimensions or compression settings
- Exact failure code taxonomy, as long as it stays bounded, truthful, and operationally useful
- Exact test split between unit, route, and device-verification coverage

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Product boundary and roadmap
- `.planning/ROADMAP.md` - Active phase list and acceptance framing for 1.4.0 through 1.4.1
- `.planning/STATE.md` - Current accepted baseline and immediate phase focus
- `docs/RPD.md` - Product truth for screenshot capture and local observation responsibilities

### Screenshot runtime path
- `app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt` - `/screenshot` HTTP contract and current success or failure shaping
- `app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt` - route-facing screenshot delegation and projection wiring
- `app/src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewCoordinator.kt` - preview-facing screenshot delegation
- `app/src/main/java/com/folklore25/ghosthand/interaction/execution/GhosthandScreenshotAccess.kt` - best-available screenshot selection seam
- `app/src/main/java/com/folklore25/ghosthand/service/accessibility/GhostCoreAccessibilityService.kt` - accessibility screenshot acquisition path
- `app/src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt` - MediaProjection full-screen capture path

### Existing contract and verification surfaces
- `app/src/main/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalogRoutes.kt` - route description and screenshot contract wording
- `docs/Runtime-Verification-Workflow.md` - screenshot verification expectations and visual-truth workflow
- `scripts/ghosthand-verify-runtime.sh` - existing runtime verification entry points for screenshot scenarios

</canonical_refs>

<specifics>
## Specific Ideas

- Determine whether the regression lives in MediaProjection lifecycle handling, accessibility screenshot acquisition, bitmap encoding, width or height validation, response serialization, or preview downsample settings.
- Audit whether current `width` and `height` request parameters can drive invalid or empty output paths.
- Ensure full-resolution and preview-grade responses explicitly report returned dimensions and real byte-bearing success.
- Use git history if necessary to identify the last known-good screenshot behavior before 1.4.0 regression.

</specifics>

<deferred>
## Deferred Ideas

- Suggested fallback versus suggested source vocabulary cleanup outside screenshot-specific needs
- Broader observation-plane redesign
- Capability-plane or action-evidence follow-up not required by the screenshot fix

</deferred>

---

*Phase: 26-visual-observation-regression-correction-for-1-4-1*
*Context gathered: 2026-04-05*
