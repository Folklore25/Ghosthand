# Testing Patterns

**Analysis Date:** 2026-03-28

## Test Framework

**Runner:**
- JUnit 4 `4.13.2`
- Config: `app/build.gradle.kts`

**Assertion Library:**
- `org.junit.Assert`

**Run Commands:**
```bash
./gradlew testDebugUnitTest
./gradlew :app:assembleDebug
scripts/ghosthand-verify-runtime.sh smoke
scripts/ghosthand-verify-runtime.sh core
scripts/ghosthand-verify-runtime.sh full
scripts/ghosthand-verify-runtime.sh commands-schema-check
scripts/ghosthand-verify-runtime.sh tree-find-click
scripts/ghosthand-verify-runtime.sh focused-input
scripts/ghosthand-verify-runtime.sh selector-click
scripts/ghosthand-verify-runtime.sh screenshot-check
scripts/ghosthand-verify-runtime.sh notify-check
scripts/ghosthand-verify-runtime.sh wait-home
```

## Test File Organization

**Location:**
- Separate source sets under `app/src/test/` and `app/src/androidTest/`

**Naming:**
- Kotlin class names end with `Test`, for example `ExampleUnitTest.kt` and `ExampleInstrumentedTest.kt`

**Structure:**
```text
app/
├── src/test/java/com/folklore25/ghosthand/
│   ├── AccessibilityNodeFinderTest.kt
│   ├── AccessibilityNodeLocatorTest.kt
│   ├── GhosthandApiPayloadsTest.kt
│   ├── GhosthandCommandCatalogTest.kt
│   ├── GhosthandHttpTest.kt
│   ├── GhosthandRoutePoliciesTest.kt
│   ├── GhosthandSelectorsTest.kt
│   └── GhosthandWaitLogicTest.kt
└── src/androidTest/java/com/folklore25/ghosthand/ExampleInstrumentedTest.kt
```

## Test Structure

**Suite Organization:**
**Patterns:**
- Pure logic extraction for JVM tests
- Contract tests for:
  - `/commands`
  - selector normalization
  - route policy
  - HTTP parsing
  - payload shapes
  - wait semantics
- Assertion pattern: direct `assertEquals` / `assertTrue` from JUnit

## Mocking

**Framework:** None committed

**Patterns:**
```kotlin
// Current preference is to avoid mocking Android internals by extracting
// pure logic and payload builders into testable helpers.
```

**What to Mock:**
- Prefer extracting pure helpers and contract builders first.
- If mocking becomes necessary, mock at module boundaries rather than Android framework internals.

**What NOT to Mock:**
- Avoid mocking the runtime contract itself; prefer pure contract-layer tests plus device-shell verification.

## Fixtures and Factories

**Test Data:**
**Location:**
- Inline helper fixtures in each test file for now

## Coverage

**Requirements:** No formal coverage threshold enforced yet

**View Coverage:**
```bash
./gradlew cleanTest testDebugUnitTest
```

## Test Types

**Unit Tests:**
- Present and meaningful around:
  - command catalog
  - selectors
  - snapshot/node identity
  - payload builders
  - route policy
  - HTTP parsing
  - wait semantics

**Integration Tests:**
- Device-shell verification script is the current practical integration layer
- The current runner is repeatable and mode-based:
  - `smoke` for runtime-up checks
  - `core` for the canonical high-value interaction chain
  - `full` for install + core + screenshot + notify + wait acceptance

**E2E Tests:**
- No formal instrumentation E2E suite yet
- Current authoritative runtime acceptance path is device-shell orchestration

## Common Patterns

**Async Testing:**
Current approach:
- isolate async behavior into pure decision logic where possible
- verify timing-sensitive routes (`/wait`, foreground transitions) through device-shell orchestration
- verify delayed-acceptance routes such as `/swipe` through the runner rather than one-off host timing

**Error Testing:**
Current approach:
- validate bad-route and contract-edge behavior via route policy / HTTP parsing tests
- validate runtime failure modes through acceptance workflow rather than mocked framework behavior

## Device Verification Notes

- `restore-runtime` is not just a raw service start:
  - on this ROM, the runner may need to bring Ghosthand to foreground and use the app-owned runtime start control if direct service start does not make `/ping` available yet
- Clipboard verification is device-sensitive:
  - the runner brings Ghosthand to foreground before the clipboard write/read chain because that is the stable acceptance path on the target device
- Screenshot verification accepts the current runtime payload shape:
  - data URI PNG response is valid for the current route contract

---

*Testing analysis refreshed: 2026-03-28*
