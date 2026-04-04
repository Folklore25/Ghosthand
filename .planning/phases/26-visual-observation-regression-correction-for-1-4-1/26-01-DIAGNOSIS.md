# Plan 26-01 Screenshot Regression Diagnosis

## Exact failing path

### empty-image success

The current empty-image success path is real at the route layer, not just hypothetical backend noise.

1. `ReadScreenshotRouteHandlers` calls `stateCoordinator.captureBestScreenshot(width, height)`.
2. `StateCoordinator` delegates to `ScreenPreviewCoordinator.captureBestScreenshot(...)`.
3. `ScreenPreviewCoordinator` delegates to `GhosthandScreenshotAccess.captureBestAvailable(...)`.
4. `AccessibilityScreenshotAccess` returns the first backend with `available=true`.
5. `ReadScreenshotRouteHandlers` treats `available=true` as full success and serializes:
   - `"image": "data:image/png;base64,${screenshotResult.base64 ?: ""}"`
   - returned `width`
   - returned `height`
6. Because the route appends `?: ""`, a result with `available=true` and `base64=null` or blank produces `data:image/png;base64,` and still returns HTTP 200.

That means the route contract currently defines success by `available` alone. It does not require usable bytes, non-blank base64, or positive returned dimensions.

### backend selection truth

`GhosthandScreenshotAccess` currently chooses the first backend with `available=true`, not the first backend with a usable image.

- If accessibility returns `available=true` with blank bytes or invalid dimensions, projection is never consulted.
- If projection returns `available=true` with blank bytes or invalid dimensions, that result is also accepted.

This is backend selection truth leaking directly into route truth.

## Full-resolution failure hypothesis

The most concrete full-resolution failure hypothesis is in `MediaProjectionProvider`.

- `captureScreenshot()` creates `ImageReader` and `VirtualDisplay`.
- It immediately calls `reader.acquireLatestImage()`.
- There is no `OnImageAvailableListener`, no frame wait, and no proof that the first frame has arrived before acquisition.
- If no frame is ready yet, the code returns `attemptedPath = "image_acquire_timeout"`.

So the likely regression is an immediate first-frame race in the full-resolution projection path. The code names the failure like a timeout, but it does not actually wait for an image before deciding.

## Invalid requested dimensions

### accessibility backend

`GhostCoreAccessibilityService.takeScreenshot(width, height)` uses:

- requested width if `width > 0`, otherwise hardware width
- requested height if `height > 0`, otherwise hardware height

It then calls `Bitmap.createScaledBitmap(...)` directly with those target values.

Implications:

- negative and zero requests fall back to native dimensions
- independently valid positive requests can still distort aspect ratio
- there is no guard that the final encoded PNG has non-zero bytes
- there is no guard that the returned dimensions are positive and operationally usable before `available=true` is set

So invalid requested dimensions are unlikely to produce the exact empty path through zero or negative values alone, but they can still produce unusable output because the backend never validates byte-bearing success beyond "bitmap preparation returned something."

### MediaProjection backend

`MediaProjectionProvider.captureScreenshot(requestWidth, requestHeight)` uses:

- screen width or height when request is `<= 0`
- requested width or height directly when request is `> 0`

Implications:

- zero and negative requests fall back to full-resolution capture intent
- tiny but positive requests are accepted directly
- there is no validation that the encoded PNG byte array is non-empty before success is reported

So invalid requested dimensions do not explain the immediate empty-image success by themselves, but the current implementation can still mark unusable tiny or byte-empty output as success if encoding returns an empty payload.

## Route serialization truth vs backend truth

### route serialization truth

The route currently defines screenshot success as:

- `available == true`

It does not require:

- non-blank base64
- non-zero encoded byte count
- positive returned width and height

This is the exact source of the empty-image success contract.

### MediaProjection truth

Current MediaProjection truth is weak in two places:

- it can fail full-resolution capture before any frame is actually awaited
- it can report success without validating non-empty encoded bytes

### AccessibilityService truth

Current AccessibilityService truth is weak in one place:

- it reports success after bitmap preparation and base64 encoding attempt, but never proves the encoded PNG is materially present and usable

## Screenshot-adjacent truth leak

`ScreenReadPayloadComposer.createOcrPayload(...)` sets:

- `visualAvailable = screenshotResult.available`
- `previewAvailable = screenshotResult.available`

So the same weak screenshot truth also leaks into OCR and preview signaling. That is not the primary runtime regression here, but it confirms the truth boundary is shared rather than route-local only.

## Exact truths later plans must correct

1. Empty-image success must be eliminated by requiring more than `available=true`.
2. `GhosthandScreenshotAccess` must prefer the first backend with a usable image, not the first backend claiming availability.
3. Full-resolution `MediaProjection` capture must prove a real frame arrived before reporting failure or success.
4. Both backends must validate usable bytes and positive returned dimensions before success can propagate.
5. Route serialization must stop manufacturing `"data:image/png;base64,"` from null or blank image content.

## Expected failing assertions for this plan

- route-level success with `available=true` but blank or missing bytes should fail
- default `/screenshot` behavior should keep full-resolution intent instead of silently normalizing to degraded tiny output
- projection capture should fail specifically when no frame is acquired instead of allowing an empty-image success interpretation

## Recorded red-test failures

Command run:

- `./gradlew :app:testDebugUnitTest --tests "*ReadScreenshotRouteHandlersTest" --tests "*ScreenshotDispatchResultTruthTest" --tests "*MediaProjectionProviderTest"`

Observed result:

- 8 tests executed
- 6 failed
- 2 passed control assertions proving the suite stayed screenshot-specific

Intentional failing assertions:

1. `ReadScreenshotRouteHandlersTest.screenshotRouteDoesNotTreatAvailableAloneAsSuccess`
   - failed because the route still uses `return if (screenshotResult.available)`
2. `ReadScreenshotRouteHandlersTest.screenshotRouteDoesNotSerializeBlankBase64IntoSuccessPayload`
   - failed because the route still serializes `data:image/png;base64,${screenshotResult.base64 ?: ""}`
3. `ScreenshotDispatchResultTruthTest.blankAccessibilityImageMustNotBlockUsableProjectionCapture`
   - failed because `AccessibilityScreenshotAccess` still returns the accessibility result once `available=true`, even when the image bytes are blank
4. `ScreenshotDispatchResultTruthTest.zeroDimensionAccessibilityImageMustNotBlockUsableProjectionCapture`
   - failed because `AccessibilityScreenshotAccess` still returns the accessibility result once `available=true`, even when returned dimensions are unusable
5. `MediaProjectionProviderTest.projectionCaptureWaitsForAnActualFrameBeforeReadingImage`
   - failed because `MediaProjectionProvider` still does not wait for an actual frame listener before reading from `ImageReader`
6. `MediaProjectionProviderTest.projectionCaptureDoesNotAcceptEmptyEncodedBytesAsSuccess`
   - failed because `MediaProjectionProvider` still has a direct success construction from encoded base64 without any explicit non-empty-byte validation

Passing control assertions:

- `ReadScreenshotRouteHandlersTest.screenshotRouteDefaultsToFullResolutionIntent`
- `MediaProjectionProviderTest.projectionCaptureDefaultsToDisplayResolutionWhenRequestIsMissing`

Those passes confirm the current regression is not that `/screenshot` defaults to preview-size dimensions. The broken boundary is success truth, backend selection truth, and projection frame acquisition truth.
