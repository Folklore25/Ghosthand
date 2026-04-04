# Phase 26: Visual Observation Regression Correction For 1.4.1 - Research

**Researched:** 2026-04-05
**Domain:** Android screenshot capture truth, preview usability, and screenshot failure semantics
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Treat this as an observation-plane correctness issue, not a cosmetic screenshot issue.
- Do not solve the problem by silently forcing tiny degraded images while reporting success.
- Restore reliable full-resolution capture in the normal supported case if platform and runtime conditions allow it.
- If capture cannot be produced, return a truthful and more specific failure than generic `NO_SCREENSHOT` where the runtime has that information.
- Preview or downsample output may remain lighter than full capture, but it must be materially useful for agent visual reasoning.
- Empty-image success cases must be eliminated.
- Keep the pass bounded to screenshot and directly related visual-observation contract paths.

### Claude's Discretion
- Exact helper or collaborator extraction shape inside the screenshot path
- Exact lightweight preview dimensions or compression settings
- Exact failure code taxonomy, as long as it stays bounded, truthful, and operationally useful
- Exact test split between unit, route, and device-verification coverage

### Deferred Ideas (OUT OF SCOPE)
- Suggested fallback versus suggested source vocabulary cleanup outside screenshot-specific needs
- Broader observation-plane redesign
- Capability-plane or action-evidence follow-up not required by the screenshot fix
</user_constraints>

## Project Constraints (from CLAUDE.md)

- Follow `docs/RPD.md` and `docs/ROADMAP.md`.
- Only read the minimum files needed for the current task.
- Prefer small, bounded changes. Do not do broad refactors unless asked.
- Keep the project buildable after every task.
- Preserve module boundaries: `domain` defines contracts, `feature/*` implements capabilities, `feature/root` is the only privileged module.
- Never expose arbitrary shell/root execution as public API.
- When changing public API or contracts, update the relevant docs in `docs/`.

## Summary

The screenshot regression is most likely not one bug but one broken truth boundary shared by the screenshot backends and the route contract. `ReadScreenshotRouteHandlers.kt` returns `200` whenever `ScreenshotDispatchResult.available` is true, even if `base64` is null or blank, and always serializes `"data:image/png;base64,"` by appending `?: ""`. That makes empty-image success possible by construction. The same weak truth source leaks into OCR and preview signaling, because preview metadata and OCR visual availability treat `available` as sufficient even when the image bytes are unusable.

The most concrete capture-side failure hypothesis is in `MediaProjectionProvider.kt`: it creates a virtual display and immediately calls `reader.acquireLatestImage()` with no listener or frame wait. That strongly suggests a null or stale first-frame race, which matches the existing `image_acquire_timeout` attempted path. The accessibility path in `GhostCoreAccessibilityService.kt` has a different but related truth issue: it scales directly to requested `width`/`height`, never validates positive dimensions or encoded byte count, and marks success if bitmap preparation returns a bitmap, not if PNG bytes are materially present.

**Primary recommendation:** introduce a single screenshot-result validation layer that requires non-empty PNG bytes plus positive returned dimensions, use it in both capture backends and `/screenshot`, preserve full-resolution as the default path, and make preview a bounded, aspect-preserving derivative that never reports success or availability without a real image payload.

## Standard Stack

### Core
| Library / API | Version | Purpose | Why Standard Here |
|---------|---------|---------|--------------|
| Android `AccessibilityService.takeScreenshot` | API 30+ | Primary screenshot path when accessibility dispatch is live | Already the mainline local capture path in Ghosthand |
| Android `MediaProjection` + `ImageReader` | Framework API | Fallback full-screen capture path | Already the non-accessibility screenshot backend |
| `ScreenshotDispatchResult` | in-repo contract | Shared backend result model | Central point to harden truth semantics without widening API surface |

### Supporting
| Library / Tool | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| ML Kit text recognition | `16.0.1` | OCR on screenshot bytes | Only for OCR/hybrid screen reads after screenshot bytes are valid |
| JUnit | `4.13.2` | unit tests | Route/result validation and composer regressions |
| `scripts/ghosthand-verify-runtime.sh` | repo script | device runtime verification | Endpoint and on-device screenshot proof |

## Architecture Patterns

### Recommended ownership
- `interaction/execution`: backend capture dispatch and shared result truth
- `integration/projection`: MediaProjection full-resolution capture mechanics
- `service/accessibility`: accessibility screenshot mechanics
- `routes/read`: HTTP `/screenshot` contract and failure mapping
- `screen/read` and `preview`: preview advertisement and OCR-facing visual truth

### Pattern 1: validate once, serialize once
Use one helper around `ScreenshotDispatchResult` to answer:
- does this result contain usable image bytes
- what are the returned dimensions
- what specific failure code should the route expose

Do not let routes, OCR, preview, and backends each invent their own screenshot truth rules.

### Pattern 2: full resolution by default, preview as derivative
- `GET /screenshot` with no size params should request native/full-resolution capture.
- Preview should be derived intentionally from a successful capture, not by forcing every normal request through a tiny requested width/height.
- Keep preview bounded, but preserve aspect ratio and a minimum decision-usable floor.

### Pattern 3: distinguish capability from capture success
`CapabilityAccessSnapshot.screenshot.effective.usableNow` is only a gate, not proof that `/screenshot` will return bytes. `previewAvailable` and OCR visual truth should not overclaim based only on capability readiness.

### Anti-Patterns to Avoid
- Treating `available=true` as synonymous with usable image bytes.
- Advertising `previewAvailable=true` from pure capability state when live capture is failing.
- Forcing a square `240x240` preview that distorts aspect ratio and can be too small for agent reasoning.
- Collapsing backend-specific failure knowledge into one `SCREENSHOT_FAILED` response with the real reason buried inside the message text.

## Likely Concrete Failure Locations

1. `app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt`
- Empty-image success is possible because success only checks `available`.
- Route always emits `data:image/png;base64,${base64 ?: ""}`.
- No failure-code mapping beyond generic `SCREENSHOT_FAILED`.

2. `app/src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt`
- `acquireLatestImage()` is called immediately after virtual display creation with no frame wait or image-available callback.
- No validation that the compressed PNG produced non-zero bytes.
- Request size is applied directly to the capture surface; invalid tiny sizes can degrade truth or usability.

3. `app/src/main/java/com/folklore25/ghosthand/service/accessibility/GhostCoreAccessibilityService.kt`
- Requested `width` and `height` are applied directly to scaling with no bounded preview policy.
- No positive-dimension guard after requested resize.
- `Bitmap.compress(...)` return value and output byte count are ignored.

4. `app/src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadPayloadComposer.kt`
- OCR payload sets `visualAvailable` and `previewAvailable` from `screenshotResult.available`, not from real image bytes.

5. `app/src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewMetadata.kt`
- Accessibility-mode `/screen` preview metadata is published from capability truth only, not observed capture truth.
- This can advertise preview while `/screenshot` fails.

## Git-History Clues

- `GhostCoreAccessibilityService.takeScreenshot` and `MediaProjectionProvider.captureScreenshot` were introduced together in commit `286a5ac6` on 2026-03-28.
- Later passes `06ea02c` and `bae1cc3` touched screenshot-adjacent code only for service-type/cleanup work; they did not materially harden image-byte validation or MediaProjection frame acquisition.
- That makes the 2026-03-28 screenshot introduction the strongest regression origin visible in the repo.

## Concrete Implementation Guidance

### Workstream 1: trace failing screenshot path
Touch:
- `app/src/main/java/com/folklore25/ghosthand/interaction/execution/GhostAccessibilityExecutionCore.kt`
- `app/src/main/java/com/folklore25/ghosthand/interaction/execution/GhosthandScreenshotAccess.kt`
- `app/src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt`
- `app/src/main/java/com/folklore25/ghosthand/service/accessibility/GhostCoreAccessibilityService.kt`

Implement:
- Add explicit screenshot result helpers on or beside `ScreenshotDispatchResult` for `hasUsableImage`, `failureCode`, and possibly `returnedWidth/returnedHeight` validation.
- Preserve `attemptedPath`, but split backend failure reasons cleanly enough that `/screenshot` can classify them.
- In `GhosthandScreenshotAccess`, prefer returning the first backend with validated bytes, not the first backend with `available=true`.

### Workstream 2: restore truthful full-resolution capture and eliminate empty-image success
Touch:
- `app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt`
- `app/src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt`
- `app/src/main/java/com/folklore25/ghosthand/service/accessibility/GhostCoreAccessibilityService.kt`

Implement:
- Default `width=0,height=0` to native capture dimensions.
- Reject success unless all are true: positive returned width, positive returned height, non-blank base64, and non-zero encoded byte array before base64 encoding.
- In `MediaProjectionProvider`, wait for an actual image frame instead of immediate `acquireLatestImage()` after virtual display creation.
- If requested resize is invalid or unsupported, fail specifically instead of returning an empty image.

### Workstream 3: improve lightweight preview/downsample usefulness without misleading success
Touch:
- `app/src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewCoordinator.kt`
- `app/src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewMetadata.kt`
- `app/src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadCoordinator.kt`
- `app/src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadPayloadComposer.kt`
- `app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt`

Implement:
- Stop hard-forcing a square `240x240` preview policy. Use an aspect-preserving bounded preview size.
- Keep `/screenshot?width=&height=` as the preview retrieval path, but ensure the requested size policy produces a materially readable image.
- `previewAvailable` should not mean only "capability exists"; it should mean the runtime is willing to advertise a preview path that should return a real image under current conditions.
- OCR/hybrid payloads should set `visualAvailable` and `previewAvailable` from validated screenshot usability, not raw `available`.

### Workstream 4: tighten failure classification and verification
Touch:
- `app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt`
- `app/src/main/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalogRoutes.kt`
- `scripts/ghosthand-verify-runtime.sh`
- `app/src/test/java/com/folklore25/ghosthand/screen/StateCoordinatorScreenPayloadTest.kt`
- add new route/execution tests under `app/src/test/java/com/folklore25/ghosthand/routes/` and `app/src/test/java/com/folklore25/ghosthand/interaction/`

Implement:
- Replace generic `SCREENSHOT_FAILED`-only handling with bounded specific codes where already knowable, such as projection missing, frame timeout, security/capability unavailable, invalid requested dimensions, or empty image produced.
- Keep command catalog wording aligned with truthful screenshot/preview semantics.
- Strengthen runtime verifier to require non-empty base64 payload length and, for preview requests, materially useful returned dimensions.

## Exact Files / Packages The Planner Should Touch

- `app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt`
- `app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt`
- `app/src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewCoordinator.kt`
- `app/src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewMetadata.kt`
- `app/src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewCaptureSupport.kt`
- `app/src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadCoordinator.kt`
- `app/src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadPayloadComposer.kt`
- `app/src/main/java/com/folklore25/ghosthand/interaction/execution/GhostAccessibilityExecutionCore.kt`
- `app/src/main/java/com/folklore25/ghosthand/interaction/execution/GhosthandScreenshotAccess.kt`
- `app/src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt`
- `app/src/main/java/com/folklore25/ghosthand/service/accessibility/GhostCoreAccessibilityService.kt`
- `app/src/main/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalogRoutes.kt`
- `scripts/ghosthand-verify-runtime.sh`
- `app/src/test/java/com/folklore25/ghosthand/screen/StateCoordinatorScreenPayloadTest.kt`
- new or updated screenshot route tests in `app/src/test/java/com/folklore25/ghosthand/routes/`
- new or updated screenshot execution tests in `app/src/test/java/com/folklore25/ghosthand/interaction/` or `app/src/test/java/com/folklore25/ghosthand/screen/`

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| OCR input gating | custom image-validity heuristics in OCR only | shared screenshot validation helper | OCR is downstream of screenshot truth |
| Preview truth | separate preview-only success rules | same validated screenshot result contract | avoids route/preview/OCR drift |
| Failure semantics | ad hoc string parsing from `attemptedPath` in many places | one bounded failure mapping layer | keeps API truth stable and testable |

## Common Pitfalls

### Pitfall 1: success without bytes
`available=true` is not enough. The planner should require tests for blank base64, zero-byte compression output, and zero dimensions.

### Pitfall 2: capability truth mistaken for capture truth
`usableNow` only says a backend should exist. It does not prove a frame was acquired.

### Pitfall 3: preview that is technically valid but operationally useless
A tiny square thumbnail can pass schema checks but still be unusable for visual reasoning. The acceptance bar must include returned dimensions and practical readability.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Gradle + Android local unit tests + JUnit 4.13.2 |
| Config file | `app/build.gradle.kts` |
| Quick run command | `./gradlew :app:testDebugUnitTest` |
| Full suite command | `./gradlew :app:compileDebugKotlin testDebugUnitTest` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| WS1 | backend result validation chooses real image, not empty success | unit | `./gradlew :app:testDebugUnitTest --tests "*Screenshot*"` | ❌ Wave 0 |
| WS2 | `/screenshot` rejects blank image payloads and preserves full-res default | unit/route | `./gradlew :app:testDebugUnitTest --tests "*Route*"` | ❌ Wave 0 |
| WS3 | `/screen` preview metadata and OCR visual flags only advertise usable preview | unit | `./gradlew :app:testDebugUnitTest --tests "com.folklore25.ghosthand.screen.StateCoordinatorScreenPayloadTest"` | ✅ partial |
| WS4 | runtime verifier fails on empty image success and unusable preview | device script | `scripts/ghosthand-verify-runtime.sh screenshot-check` | ✅ needs strengthening |

### Wave 0 Gaps
- Add dedicated route tests for `ReadScreenshotRouteHandlers` success/failure mapping.
- Add backend-result tests for blank-base64, zero-dimension, and invalid resize handling.
- Extend `StateCoordinatorScreenPayloadTest` to assert preview truth uses validated image availability, not capability alone.
- Strengthen `scripts/ghosthand-verify-runtime.sh` so `screenshot-check` requires non-empty payload bytes, not just the base64 prefix.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| `adb` | device screenshot verification | ✓ | 1.0.41 | — |
| Gradle wrapper | build/test validation | ✓ | 9.3.1 | — |
| `node` | verifier payload parsing | ✓ | v22.12.0 | — |
| Java | Gradle Android build | ✓ | 17.0.18 | — |

## Sources

### Primary (HIGH confidence)
- `.planning/phases/26-visual-observation-regression-correction-for-1-4-1/26-CONTEXT.md`
- `docs/RPD.md`
- `app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlers.kt`
- `app/src/main/java/com/folklore25/ghosthand/state/StateCoordinator.kt`
- `app/src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewCoordinator.kt`
- `app/src/main/java/com/folklore25/ghosthand/preview/ScreenPreviewMetadata.kt`
- `app/src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadCoordinator.kt`
- `app/src/main/java/com/folklore25/ghosthand/screen/read/ScreenReadPayloadComposer.kt`
- `app/src/main/java/com/folklore25/ghosthand/interaction/execution/GhostAccessibilityExecutionCore.kt`
- `app/src/main/java/com/folklore25/ghosthand/interaction/execution/GhosthandScreenshotAccess.kt`
- `app/src/main/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProvider.kt`
- `app/src/main/java/com/folklore25/ghosthand/service/accessibility/GhostCoreAccessibilityService.kt`
- `app/src/main/java/com/folklore25/ghosthand/catalog/GhosthandCommandCatalogRoutes.kt`
- `app/src/main/java/com/folklore25/ghosthand/screen/ocr/ScreenOcrProvider.kt`
- `scripts/ghosthand-verify-runtime.sh`
- `app/src/test/java/com/folklore25/ghosthand/screen/StateCoordinatorScreenPayloadTest.kt`

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - derived from current project code and Gradle files
- Architecture: HIGH - based on current package ownership and direct call chains
- Pitfalls: HIGH - directly visible in route serialization, preview metadata, verifier assertions, and backend capture code

**Research date:** 2026-04-05
**Valid until:** 2026-05-05
