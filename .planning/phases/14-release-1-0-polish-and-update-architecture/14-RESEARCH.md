# Phase 14: Release 1.0 Polish And Update Architecture - Research

**Researched:** 2026-03-30
**Domain:** Android Views/XML release-state integration, reusable explanation UI, product-copy cleanup
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
### Update Architecture
- The app must compare the installed version against GitHub latest-release metadata.
- The update flow is a full update handoff to GitHub, not a silent or in-place package update.
- Update state must be explicit and user-facing:
  - checking
  - up to date
  - update available
  - check failed / unavailable
- Update/version behavior must be driven by real models and UI state, not hardcoded URL buttons.

### Product Copy
- Current product-facing strings contain iteration leftovers and process-language that must be cleaned out.
- Obsolete string variants should be removed or consolidated rather than left dormant.
- EN and zh-CN must stay aligned.
- Tone should stay calm, precise, and product-grade.

### Explanation Pattern
- Relevant product modules should expose a top-right info affordance.
- Explanations should use a consistent reusable surface, preferably a bottom sheet or lightweight modal/dialog.
- Explanations should cover:
  - what the feature does
  - what it controls
  - what authorization it may need
  - whether Ghosthand/OpenClaw may use it
- Do not use scattered toasts or per-card one-off implementations.

### Front-End Polish
- Keep the accepted front-end direction intact.
- Tighten spacing, alignment, row rhythm, and card hierarchy.
- Make update/version a first-class product area.
- Keep permissions and diagnostics deliberate and product-like.
- Do not restart the major design direction.

### Code Structure
- `MainActivity` should not keep accumulating direct view responsibility.
- Introduce bounded update/release models and a reusable explanation model instead of embedding strings and URLs inline.
- Keep product-facing permission text separate from diagnostic detail.
- Remove misleading or obsolete resources while preserving buildability.

### the agent's Discretion
- exact release metadata source shape from GitHub latest-release payload
- exact class names and file splits for update repository and explanation models
- exact UI placement of the update section as long as it is first-class and truthful
- exact reusable explanation surface implementation as long as it stays consistent and bounded

### Deferred Ideas (OUT OF SCOPE)
- broader release-channel management beyond GitHub latest release
- background auto-download or package-installer automation
- another large information-architecture reset
- broader permission model changes beyond text and presentation polish
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| POL-01 | The app compares the installed version against GitHub latest-release metadata and shows a truthful in-product update state. | Add a dedicated release-check repository and merge its state into `HomeScreenUiState` instead of keeping static buttons in `MainActivity`. |
| POL-02 | The update flow guides the user to GitHub for a full APK update handoff without implying fake seamless updating. | Use GitHub `html_url` or APK asset `browser_download_url` for handoff only; never claim in-app install. |
| POL-03 | Product-facing copy is cleaned up, obsolete string variants are removed, and EN plus zh-CN stay aligned. | Remove `_v2`/`_v3` leftovers, replace process/debug wording, and update both locale files together. |
| POL-04 | Relevant product modules expose a reusable explanation affordance with a consistent help surface. | Add a shared explanation registry plus one reusable bottom-sheet/dialog surface. |
| POL-05 | The accepted 1.0 front-end direction is tightened through bounded polish and cleaner screen-state driven implementation, and the project still compiles. | Keep the current activity/layout structure, improve section hierarchy, and move new logic out of activity click wiring into shared state models. |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

- Follow `docs/RPD.md` and `docs/ROADMAP.md`.
- Only read the minimum files needed for the current task.
- Prefer small, bounded changes. Do not do broad refactors unless asked.
- Keep the project buildable after every task.
- Preserve module boundaries: `domain` defines contracts, `feature/*` implements capabilities, `feature/root` is the only privileged module.
- Never expose arbitrary shell/root execution as public API.
- When changing public API or contracts, update the relevant docs in `docs/`.
- Do one task at a time.
- State what you are changing before editing.
- Build order remains `P0 skeleton -> P1 health/state -> P2 accessibility -> P3 root -> P4 recovery`; Phase 14 must stay bounded polish on the accepted local non-root path.

## Summary

The current home surface is still missing a real update architecture. [`MainActivity.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/MainActivity.kt) renders a version badge from `RuntimeState.buildVersion`, but the update area is only two static external-link buttons backed by `home_update_url` and `home_github_url` in [`strings.xml`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/res/values/strings.xml). There is no release model, no network fetch, no compare logic, no stale/error/update-available state, and no shared UI state for update truth. The current displayed build string also comes from [`HomeDiagnosticsProvider.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/HomeDiagnosticsProvider.kt) as `"versionName (versionCode)"`, which is unsuitable as the comparison source.

The bounded fit for this codebase is to keep runtime state and release state separate. `RuntimeStateStore` should continue to own local runtime/capability truth. A new release-check repository should own installed-version parsing, GitHub latest-release fetch, compare/normalize logic, and last-check status. [`RuntimeStateViewModel.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/RuntimeStateViewModel.kt) should merge both sources into `HomeScreenUiState`, so `MainActivity` becomes a renderer plus intent launcher instead of accumulating more imperative logic.

For explanations, this app is still plain Activities plus XML, already using Material Components. The least disruptive reusable pattern is one shared explanation registry plus one reusable bottom-sheet dialog surface, wired by small top-right info affordances in card headers. That keeps copy centralized, avoids one-off toasts, and fits the accepted UI direction without reopening the layout architecture.

**Primary recommendation:** Implement Phase 14 as three plans: `release-state repository + home update section`, `shared explanation surface + copy cleanup`, then `bounded layout/state cleanup + regression verification`.

## Standard Stack

### Core
| Library / API | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Android Views + XML | repo current | Existing UI stack | Already used by all three activities; no redesign or Compose migration required. |
| `androidx.lifecycle` LiveData/ViewModel | 2.8.7 (repo-pinned) | Merge runtime state and release state | Already used in [`RuntimeStateViewModel.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/RuntimeStateViewModel.kt); keeps screen derivation centralized. |
| Material Components | 1.10.0 (repo-pinned) | Cards, buttons, chips, dialog/bottom-sheet surface | Already present; supports a reusable explanation surface without new dependencies. |
| `HttpsURLConnection` + `org.json.JSONObject` | platform / existing | GitHub latest-release fetch and JSON parsing | Bounded one-endpoint solution; avoids introducing Retrofit/OkHttp for a single public metadata call. |

### Supporting
| Library / API | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `PackageManager` version info | platform | Installed `versionName` and `longVersionCode` | Use for canonical installed-version snapshot, not the UI badge string. |
| `BottomSheetDialogFragment` or `BottomSheetDialog` | Material current | Reusable explanation surface | Use one shared implementation for all info affordances. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `HttpsURLConnection` + `org.json` | Retrofit/OkHttp/Moshi | Cleaner APIs, but overkill for one unauthenticated endpoint in a bounded polish phase. |
| Shared explanation sheet | Per-activity dialogs/toasts | Faster short term, but duplicates copy and makes Phase 14 brittle again. |
| Separate release state source | Putting remote status in `RuntimeStateStore` | Pollutes runtime truth with network/UI concerns and makes `/state` semantics harder later. |

**Installation:**
```bash
# No new dependency is required for the recommended bounded implementation.
```

## Current Gaps

| Area | Current State | Gap |
|------|---------------|-----|
| Update flow | `homeUpdateButton` and `homeGithubButton` open static URLs | No release fetch, compare logic, status model, retry state, or truthful “failed/unavailable” rendering. |
| Installed version source | `HomeDiagnosticsProvider` emits `buildVersion = "versionName (versionCode)"` | Good for display, bad for semantic compare. |
| Home UI state | [`HomeScreenUiState.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/HomeScreenUiState.kt) has no update or help model | Forces activity-local button wiring instead of state-driven rendering. |
| Explanation/help | No shared affordance or content model exists | Every future explanation would become ad hoc. |
| Copy resources | Many `_v2` / `_v3` leftovers remain in EN and zh-CN | Visible copy still carries process/iteration language and dead variants. |
| Verification | Existing tests cover layout/state patterns, but nothing covers release parsing or explanation affordances | Phase 14 needs new unit tests and light manual device validation. |

## Architecture Patterns

### Recommended Project Structure
```text
app/src/main/java/com/folklore25/ghosthand/
├── MainActivity.kt                  # render + intents only
├── RuntimeStateViewModel.kt         # merges runtime + release + explanation UI state
├── HomeScreenUiState.kt             # includes update section model
├── release/
│   ├── ReleaseRepository.kt         # network fetch + compare + cache + retry
│   ├── ReleaseModels.kt             # payload + status models
│   └── ReleaseVersionParser.kt      # normalize tag/version comparison
└── explanation/
    ├── FeatureExplanation.kt        # reusable content model
    ├── FeatureExplanationRegistry.kt
    └── FeatureExplanationBottomSheet.kt
```

### Pattern 1: Dedicated Release Repository
**What:** A small repository fetches `/repos/{owner}/{repo}/releases/latest`, parses `tag_name`, `name`, `html_url`, `published_at`, and optionally the first APK `browser_download_url`, then compares that with installed package version info.

**When to use:** Home screen creation/resume, and manual retry from the update card.

**Recommendation:** Keep this out of `RuntimeStateStore`. Expose `LiveData<ReleaseUiState>` or equivalent from the repository and merge it in the view model.

**Example:**
```kotlin
data class InstalledVersion(val versionName: String, val versionCode: Long)

data class ReleaseUiState(
    val state: ReleaseCheckState,
    val installedLabel: String,
    val latestLabel: String? = null,
    val releaseUrl: String? = null,
    val checkedAt: String? = null,
    val errorMessage: String? = null
)

enum class ReleaseCheckState {
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    UNAVAILABLE
}
```

### Pattern 2: ViewModel Merge, Not Activity Logic
**What:** `RuntimeStateViewModel` should combine `RuntimeStateStore.observe()` with release status into a single derived home screen model.

**When to use:** Any new home-surface section that is not purely a click target.

**Recommendation:** Add `updateSummary` and `pendingExplanation` models to `HomeScreenUiState`, then let `MainActivity` bind them. Keep network start/retry methods on the view model.

### Pattern 3: Reusable Explanation Registry + One Surface
**What:** Define explanations as structured data keyed by module/capability, then show them through one reusable bottom-sheet/dialog implementation.

**When to use:** Home runtime section, home update section, permissions cards, diagnostics section.

**Recommendation:** Use one top-right info affordance per relevant section header and one shared renderer. Copy should live in string resources, not inline code.

### Anti-Patterns to Avoid
- **Network fetch in `MainActivity`:** keeps screen state button-wired and makes retry/configuration handling brittle.
- **Comparing `buildVersion` text to GitHub tags:** `"1.0 (1)"` is display text, not semantic version data.
- **Writing remote release state into `RuntimeStateStore`:** update checks are app UI/network concerns, not runtime substrate truth.
- **One-off toasts or inline strings for explanations:** duplicates logic and guarantees locale drift.
- **Auto-install language:** this phase must never imply silent, seamless, or in-place APK upgrade.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Update install flow | In-app APK installer/seamless updater | GitHub release page / APK asset handoff | Avoids fake claims and avoids reopening install/security architecture. |
| Release transport | New networking stack | `HttpsURLConnection` + `org.json` | One endpoint only; bounded and already compatible with current code style. |
| Explanation system | One dialog implementation per activity | Shared explanation registry + one bottom-sheet/dialog | Keeps copy and behavior consistent across screens. |
| Version compare | String compare on rendered labels | Normalized semantic-ish parser on `versionName` / `tag_name` | Prevents `v1.0.0` vs `1.0` false mismatches. |

**Key insight:** The bounded win here is not “more UI”; it is moving update/help behavior into small shared models so the existing UI can render truthful state.

## Common Pitfalls

### Pitfall 1: Comparing the wrong version string
**What goes wrong:** The app compares `buildVersion = "1.0 (1)"` or a rendered badge like `v1.0 (1)` to a GitHub tag such as `v1.0.0`.
**Why it happens:** Current diagnostics formatting is display-first, not compare-first.
**How to avoid:** Compare normalized `PackageInfo.versionName` and `tag_name`; render human labels separately.
**Warning signs:** “Update available” flips incorrectly for equal releases.

### Pitfall 2: Treating `/releases/latest` as “newest by publish time”
**What goes wrong:** The repo’s latest release shown by GitHub does not match developer expectations.
**Why it happens:** GitHub documents latest release as the latest published full release and sorts by `created_at`; prereleases and drafts are excluded.
**How to avoid:** Accept that contract for Phase 14 and, if needed later, manage release semantics in GitHub release metadata rather than app code.
**Warning signs:** A just-published older-tag release appears “latest” unexpectedly.

### Pitfall 3: Letting explanation copy live in code
**What goes wrong:** EN and zh-CN drift, and the explanation surface becomes another source of inline strings.
**Why it happens:** Quick dialog implementations tempt activity-local text blocks.
**How to avoid:** Keep explanation identifiers in code and text in resources.
**Warning signs:** One locale shows missing text or stale copy after cleanup.

### Pitfall 4: Cleaning copy without deleting dead variants
**What goes wrong:** `_v2` and `_v3` strings remain in resources and keep tests/layouts ambiguous.
**Why it happens:** Substitution-only cleanup is faster short term.
**How to avoid:** Replace active references, then delete obsolete variants in both locales in the same change.
**Warning signs:** Resource files keep growing and layout/tests still reference old suffix variants.

## Code Examples

Verified patterns from official sources:

### GitHub Latest Release Endpoint
```text
GET https://api.github.com/repos/{owner}/{repo}/releases/latest
Accept: application/vnd.github+json
```

Use fields:
- `tag_name`
- `name`
- `html_url`
- `published_at`
- `assets[].browser_download_url`

### Shared update-card rendering rule
```kotlin
when (releaseUi.state) {
    CHECKING -> showProgress()
    UP_TO_DATE -> showStatus("Up to date", neutralOrPositiveTone)
    UPDATE_AVAILABLE -> showAction("Download from GitHub", releaseUi.releaseUrl)
    UNAVAILABLE -> showStatus("Update check unavailable", warningTone)
}
```

### Shared explanation registry
```kotlin
enum class ExplanationKey {
    HOME_RUNTIME,
    HOME_UPDATE,
    HOME_PERMISSIONS,
    HOME_DIAGNOSTICS,
    PERMISSION_ACCESSIBILITY,
    PERMISSION_SCREENSHOT
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Static “Update” and “GitHub” buttons | Real release-state section driven by GitHub latest-release metadata | Phase 14 target | Truthful update state and explicit failure handling. |
| Inline section behavior in `MainActivity` | ViewModel-derived home state from multiple sources | Existing pattern, extended in Phase 14 | Keeps activity bounded and testable. |
| Ad hoc explanatory text per screen | Shared explanation registry + one surface | Phase 14 target | Consistent product guidance without reopening IA. |

**Deprecated/outdated:**
- `_v2` / `_v3` string variants that remain referenced only because cleanup stopped halfway.
- `home_update_url` as the primary update “architecture”; it is just a fallback link.

## Copy Cleanup Targets

High-signal cleanup targets from current resources:

- [`strings.xml`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/res/values/strings.xml): `home_subtitle_v3`, `home_runtime_subtitle_v2`, `home_permissions_summary_default_v2`, `home_permissions_summary_template_v2`, `home_diagnostics_subtitle_v2`, `permissions_subtitle_v2`, `permissions_layer_note_v2`, `permission_screenshot_description_v2`.
- [`strings.xml`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/res/values/strings.xml): `home_external_link_unavailable` should stop saying “configured yet”.
- [`strings.xml`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/res/values/strings.xml): visible runtime/service text still says `placeholder` and `P0`; if still product-visible in Phase 14 surfaces, rewrite.
- [`strings.xml`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/res/values/strings.xml) and [`strings.xml`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/res/values-zh-rCN/strings.xml): diagnostics helper strings are still dev-tool oriented; keep them only if diagnostics remains explicitly advanced, otherwise tone them down.

**Cleanup risk:** Existing tests assert specific resource keys like `permissions_layer_note_v2`, so deleting resources will require updating layout/tests in the same plan.

## Recommended Plan Split

### Plan 14-01: Update/version architecture and release-state integration
**Wave order**
1. Add `ReleaseRepository`, models, parser, and installed-version snapshot.
2. Extend `RuntimeStateViewModel` + `HomeScreenUiState` to expose update state.
3. Replace static update/GitHub row in home layout with a first-class update section.

**Verification focus**
- Unit tests for tag/version normalization and compare outcomes.
- Unit tests for home update-state mapping.
- Manual device check: offline, up-to-date, and update-available states all render truthfully.

### Plan 14-02: Reusable explanation pattern and copy cleanup
**Wave order**
1. Add explanation key/model/registry and one reusable bottom-sheet/dialog.
2. Add info affordances to relevant home and permissions sections.
3. Consolidate strings, delete obsolete variants, and align zh-CN.

**Verification focus**
- Layout contract tests for new info affordances.
- Unit tests for explanation lookup and rendering inputs.
- Manual locale pass in EN and zh-CN.

### Plan 14-03: Bounded surface polish and implementation cleanup
**Wave order**
1. Tighten spacing/rhythm/hierarchy around update, permissions, and diagnostics sections.
2. Remove remaining activity-local wiring that should be screen-state driven.
3. Update docs/tests and run final compile/test/manual sanity pass.

**Verification focus**
- Existing layout/state tests stay green.
- Add focused layout assertions for update card order and no regression on permissions/diagnostics.
- Manual app sanity on launcher, permissions page, diagnostics page, and update handoff.

## Open Questions

1. **What is the canonical GitHub tag format for releases?**
   - What we know: current installed app version is `versionName = "1.0"` and UI display is `1.0 (1)`.
   - What's unclear: whether GitHub tags will be `v1.0`, `1.0`, or `v1.0.0`.
   - Recommendation: implement a tolerant normalizer that strips leading `v` and compares numeric segments; document the expected tag format in release docs.

2. **Should the home screen auto-check on every resume or use a TTL cache?**
   - What we know: Phase 14 requires real status, but not background syncing.
   - What's unclear: desired frequency and network tolerance.
   - Recommendation: auto-check on first load, then refresh only when stale by a short TTL or on explicit retry.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java | Gradle / Android builds | ✓ | 17.0.18 | — |
| Gradle wrapper | Unit/instrumentation verification | ✓ | 9.3.1 | — |
| ADB | Manual device verification | ✓ | 1.0.41 | — |
| Android INTERNET permission | GitHub release check at runtime | ✓ | declared in manifest | — |
| GitHub network reachability from device | Live update check | Unknown | — | Show `check failed / unavailable` truthfully |

**Missing dependencies with no fallback:**
- None found for planning. Live GitHub reachability still has to be handled in app state, not assumed.

**Missing dependencies with fallback:**
- `.planning/config.json` is absent. Validation should be treated as enabled by default.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit4 via Android unit tests + Android instrumentation |
| Config file | none — Gradle Android defaults |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.ScreenUiStateMapperTest --tests com.folklore25.ghosthand.HomeSurfaceLayoutContractTest --tests com.folklore25.ghosthand.PermissionsSurfaceLayoutContractTest` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| POL-01 | Release fetch/compare maps to `checking`, `up to date`, `available`, `unavailable` | unit | `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.ReleaseRepositoryTest` | ❌ Wave 0 |
| POL-02 | Update action hands off to GitHub only and stays truthful | unit + manual | `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.HomeUpdateUiStateTest` | ❌ Wave 0 |
| POL-03 | Strings/layouts stay aligned after cleanup | unit | `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.CopyResourceContractTest` | ❌ Wave 0 |
| POL-04 | Reusable explanation affordances and registry work across screens | unit | `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.FeatureExplanationRegistryTest` | ❌ Wave 0 |
| POL-05 | Home/permissions/diagnostics surfaces keep accepted structure | unit + manual | `./gradlew :app:testDebugUnitTest --tests com.folklore25.ghosthand.HomeSurfaceLayoutContractTest --tests com.folklore25.ghosthand.PermissionsSurfaceLayoutContractTest` | ✅ partial |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests <focused test class>`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`
- **Phase gate:** `./gradlew :app:testDebugUnitTest` plus manual launcher/device sanity and one live GitHub check

### Wave 0 Gaps
- [ ] `app/src/test/java/com/folklore25/ghosthand/ReleaseRepositoryTest.kt` — release fetch/parse/compare coverage for POL-01/POL-02
- [ ] `app/src/test/java/com/folklore25/ghosthand/HomeUpdateUiStateTest.kt` — home update-card rendering rules
- [ ] `app/src/test/java/com/folklore25/ghosthand/FeatureExplanationRegistryTest.kt` — explanation registry consistency for POL-04
- [ ] `app/src/test/java/com/folklore25/ghosthand/CopyResourceContractTest.kt` — asserts active copy keys and no stale `_v2` / `_v3` references

## Sources

### Primary (HIGH confidence)
- Local code and planning files read for this phase:
  - [`14-PRD.md`](/Users/zohar/Code/Project/ongoing/Ghosthand/.planning/phases/14-release-1-0-polish-and-update-architecture/14-PRD.md)
  - [`14-CONTEXT.md`](/Users/zohar/Code/Project/ongoing/Ghosthand/.planning/phases/14-release-1-0-polish-and-update-architecture/14-CONTEXT.md)
  - [`MainActivity.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/MainActivity.kt)
  - [`HomeScreenUiState.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/HomeScreenUiState.kt)
  - [`RuntimeStateViewModel.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/RuntimeStateViewModel.kt)
  - [`RuntimeState.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/RuntimeState.kt)
  - [`RuntimeStateStore.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/RuntimeStateStore.kt)
  - [`PermissionsActivity.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/PermissionsActivity.kt)
  - [`DiagnosticsActivity.kt`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/java/com/folklore25/ghosthand/DiagnosticsActivity.kt)
  - [`activity_main.xml`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/res/layout/activity_main.xml)
  - [`strings.xml`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/res/values/strings.xml)
  - [`strings.xml`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/res/values-zh-rCN/strings.xml)
  - [`app/build.gradle.kts`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/build.gradle.kts)
  - [`AndroidManifest.xml`](/Users/zohar/Code/Project/ongoing/Ghosthand/app/src/main/AndroidManifest.xml)
- GitHub REST docs: https://docs.github.com/en/rest/releases/releases#get-the-latest-release
- Android dialogs docs: https://developer.android.com/develop/ui/views/components/dialogs
- Android Material bottom sheet reference: https://developer.android.com/reference/com/google/android/material/bottomsheet/BottomSheetDialog

### Secondary (MEDIUM confidence)
- Android `AlertDialog.Builder` reference: https://developer.android.com/reference/androidx/appcompat/app/AlertDialog.Builder

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - recommended approach stays within the repo’s current View/XML, LiveData, Material, and platform networking stack.
- Architecture: HIGH - recommendation follows the existing `RuntimeStateViewModel` derived-state pattern while keeping remote update state out of runtime substrate state.
- Pitfalls: HIGH - identified directly from current code/resource shape and verified GitHub release semantics.

**Research date:** 2026-03-30
**Valid until:** 2026-04-29
