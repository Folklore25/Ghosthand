# Phase 26: Visual Observation Regression Correction For 1.4.1 - Validation

**Status:** Planned
**Updated:** 2026-04-05

## Requirement Coverage

| Requirement | Covered By | Verification |
|---|---|---|
| `VIS-01` Full-resolution screenshot works again or fails with a precise truthful reason | `26-02`, `26-04` | Route tests, backend tests, device screenshot verification |
| `VIS-02` Lightweight preview remains visually meaningful | `26-03`, `26-04` | Preview sizing tests, screen payload tests, device screenshot verification |
| `VIS-03` Empty-image success cases are eliminated | `26-01`, `26-02`, `26-04` | Screenshot truth tests, route tests, device screenshot verification |
| `VIS-04` Screenshot failures become more specific and operationally useful | `26-04` | Route tests and catalog-alignment checks |
| `VIS-05` No silent degradation or fake success | `26-01`, `26-02`, `26-03`, `26-04` | Diagnosis artifact, screenshot truth tests, preview truth tests, device verification |
| `VIS-06` Scope stays bounded to screenshot-adjacent observation correction | `26-01`, `26-02`, `26-03`, `26-04` | Plan review plus final execution review |
| `VIS-07` Real-device and local verification prove the corrected path and the app still compiles | `26-04` | `./gradlew :app:compileDebugKotlin testDebugUnitTest` and `scripts/ghosthand-verify-runtime.sh screenshot-check` |

## Planned Test Artifacts

- `app/src/test/java/com/folklore25/ghosthand/routes/read/ReadScreenshotRouteHandlersTest.kt`
- `app/src/test/java/com/folklore25/ghosthand/interaction/execution/ScreenshotDispatchResultTruthTest.kt`
- `app/src/test/java/com/folklore25/ghosthand/integration/projection/MediaProjectionProviderTest.kt`
- `app/src/test/java/com/folklore25/ghosthand/preview/ScreenPreviewCoordinatorTest.kt`
- `app/src/test/java/com/folklore25/ghosthand/screen/StateCoordinatorScreenPayloadTest.kt`

## Device Verification Gate

The phase is not complete until `scripts/ghosthand-verify-runtime.sh screenshot-check` proves all of the following on a real device:

- `/screenshot` returns non-empty image bytes
- returned dimensions are positive and match the requested mode truthfully
- preview-grade requests stay lightweight but above the decision-usable floor established in Phase 26 execution
- no empty-image success path passes through the verifier

## Notes

- Plan `26-01` intentionally creates screenshot-specific failing tests as a lock-in step. Those failures must be recorded in `26-01-DIAGNOSIS.md` and then resolved by `26-02`; they are not a final phase failure by themselves.
- This validation file is intentionally bounded to screenshot and closely related preview truth. It does not reopen wider 1.4.1 work.
