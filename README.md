# Ghosthand

<p align="center">
  <img src="./icon.svg" alt="Ghosthand logo" width="120" />
</p>

<p align="center">
  <strong>Android-native, localhost-only execution substrate for OpenClaw-style mobile agents.</strong>
</p>

<p align="center">
  <a href="https://ghosthand.cc/">
    <img alt="Website" src="https://img.shields.io/badge/ghosthand.cc-live-111111?style=for-the-badge&logo=googlechrome&logoColor=white">
  </a>
  <img alt="Android" src="https://img.shields.io/badge/Android-30%2B-34a853?style=for-the-badge&logo=android&logoColor=white">
  <img alt="API" src="https://img.shields.io/badge/Loopback%20API-127.0.0.1%3A5583-2563eb?style=for-the-badge">
  <img alt="License" src="https://img.shields.io/badge/License-MPL--2.0-f97316?style=for-the-badge">
</p>

> Ghosthand is not a no-code macro app, not a cloud dashboard, and not a generic RPA shell.  
> It is a phone-side control plane that exposes truthful Android state and action primitives to an external agent over a local HTTP API.

Inspired by Karry × Orb: https://github.com/cflank/orb-eye

---

## Why This Exists

Most mobile automation stacks fail one of two ways:

1. they are too weak: taps, screenshots, and hope
2. they are too magical: hidden heuristics, unclear fallback, fake success

Ghosthand takes the narrower route:

- run locally on Android
- expose a stable localhost API
- prefer accessibility-backed semantic control
- keep capability and permission truth explicit
- preserve inspectable action and screenshot results

That makes it useful for:

- OpenClaw-like mobile agents
- Android accessibility automation
- grounded UI control loops
- phone-side evaluation harnesses
- local-first mobile copilots
- device-side AI tool-use runtimes

---

## Current State

The current codebase includes the accepted 1.4.0 baseline plus the completed Phase 26 visual-observation correction pass for 1.4.1.

That means the repo head now emphasizes:

- truthful `/screenshot` success and failure
- bounded screenshot failure classification
- lightweight preview metadata through `/screen`
- preview sizing with an aspect-preserving decision-usable floor
- real-device verification for full-size and preview screenshot retrieval

Current governed sensitive capabilities:

- Accessibility control
- Screenshot capture

Current local API schema version:

- `/commands` schema: `1.25`

---

## What Ghosthand Exposes

### Read / observe

- `/ping`
- `/commands`
- `/capabilities`
- `/info`
- `/foreground`
- `/events`
- `/screen`
- `/tree`
- `/focused`
- `/screenshot`
- `/wait` (`GET` and `POST`)
- `/notify` (`GET`)
- `/clipboard`

### Act / interact

- `/find`
- `/click`
- `/tap`
- `/input`
- `/setText`
- `/scroll`
- `/swipe`
- `/longpress`
- `/gesture`
- `/back`
- `/home`
- `/recents`
- `/notify` (`POST` and `DELETE`)

### High-value agent properties

- structured screen reading
- selector-based interaction by text, content description, and resource id
- explicit click-resolution metadata
- accessibility-first control with screenshot support
- capability governance: system truth, app policy, effective usability
- loopback-only runtime surface, not remote-control-by-default

---

## What Makes It Agent-Friendly

### Structured truth, not one giant “screen dump”

Ghosthand keeps multiple truth surfaces distinct:

- `/screen`: shaped actionable surface
- `/tree`: fuller structural accessibility truth
- `/foreground`: observer context
- `/screenshot`: visual truth
- `/wait`: change and settle primitive

### Inspectable action results

Ghosthand does not just say “click worked”.

It can expose:

- requested selector surface
- effective strategy
- fallback usage
- wrapper or ancestor resolution
- post-action state hints
- bounded failure categories

### Truthful screenshot and preview semantics

Current repo head treats screenshot success as real only when the response has:

- decodable non-empty image bytes
- positive returned width
- positive returned height

`/screen` preview metadata now points back to `/screenshot` through `previewPath` instead of embedding full image payloads into normal `/screen` responses.

---

## Architecture

```text
External Agent
      |
      v
127.0.0.1:5583 localhost API
      |
      +--> read surfaces: /screen /tree /foreground /events /screenshot
      |
      +--> interaction routes: /find /click /tap /input /scroll /swipe ...
      |
      v
Android app runtime
      |
      +--> AccessibilityService
      +--> foreground service
      +--> notification listener
      +--> screenshot coordination
      +--> capability / policy state
```

Product baseline:

- local Android app
- loopback-only API
- accessibility-first interaction
- explicit permission governance
- local screenshot capture

This is the supported product path from [docs/RPD.md](./docs/RPD.md).

---

## Build And Install

### Requirements

- Android SDK / `adb`
- JDK compatible with the Gradle build
- Android device running Android 11+ (`minSdk = 30`)

### Debug build

```bash
./gradlew :app:assembleDebug
adb install -r -d app/build/outputs/apk/debug/app-debug.apk
```

### Release build

Release signing is configured from Gradle properties:

- `GHOSTHAND_RELEASE_STORE_FILE`
- `GHOSTHAND_RELEASE_STORE_PASSWORD`
- `GHOSTHAND_RELEASE_KEY_ALIAS`
- `GHOSTHAND_RELEASE_KEY_PASSWORD`

Build:

```bash
./gradlew :app:assembleRelease
```

Artifact:

- [app-release.apk](./app/build/outputs/apk/release/app-release.apk)

### Main app components

- launcher activity: `com.folklore25.ghosthand/.ui.main.MainActivity`
- foreground runtime service: `com.folklore25.ghosthand/.service.runtime.GhosthandForegroundService`
- primary accessibility service: `com.folklore25.ghosthand/com.folklore25.ghosthand.service.accessibility.GhostCoreAccessibilityService`

---

## Runtime Verification

The repo includes a device-side verifier:

- [scripts/ghosthand-verify-runtime.sh](./scripts/ghosthand-verify-runtime.sh)

Useful modes include:

- `restore-runtime`
- `core`
- `screenshot-check`
- `install-current-build`

Examples:

```bash
scripts/ghosthand-verify-runtime.sh restore-runtime
scripts/ghosthand-verify-runtime.sh screenshot-check
```

Current screenshot verification checks:

- full screenshot returns real PNG payload bytes
- `/screen` publishes preview metadata only when usable
- preview fetch through `previewPath` returns a real image
- preview short edge stays above the current decision-usable floor

---

## Design Principles

### Capability-first

Expose typed platform capability, not raw hidden plumbing.

### Accessibility-first

Semantic control through accessibility is the mainline path.

### Truth over decorative UX

If the app shows capability state, it must reflect:

- system authorization
- app-level policy
- effective usable state

### Local debugability

Critical behavior should be testable on-device without cloud infrastructure.

### Platform, not skill

Ghosthand is the substrate.  
Prompting, task heuristics, or app-specific strategy should live in the external agent or skill layer.

---

## Non-Goals

Ghosthand is not trying to become:

- arbitrary shell execution
- root/libsu product behavior
- cloud orchestration
- workflow planner logic
- no-code automation UI
- screenshot-only control wrapper

Its value comes from staying narrow and truthful.

---

## Who This Is For

Ghosthand is a strong fit if you:

- build or test Android agents
- operate OpenClaw-like systems
- want local inspectable phone control
- prefer structured UI truth over black-box “worked”
- are comfortable with Android permissions and runtime debugging

If you want a reusable Android agent substrate rather than a consumer automation app, that is exactly the project boundary.

---

## Roadmap

The current near-term roadmap lives in [.planning/ROADMAP.md](./.planning/ROADMAP.md).

Recent completed line:

- 1.4.0 architecture and current-state cleanup passes
- 1.4.1 visual observation regression correction

The next work should build from that truthful baseline, not reopen the same screenshot contract mistakes.

---

## Project Site

- Website: https://ghosthand.cc/

---

## License

Ghosthand is licensed under the Mozilla Public License 2.0.

See [LICENSE](./LICENSE).
