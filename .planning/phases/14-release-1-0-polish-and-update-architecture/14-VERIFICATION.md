---
phase: 14-release-1-0-polish-and-update-architecture
verified: 2026-03-30T00:00:00Z
status: human_needed
score: 5/5 must-haves verified
human_verification:
  - test: "Home update module reads clearly"
    expected: "The home surface shows installed version, latest release state, and a clear status without looking like a leftover utility strip."
    why_human: "Final product hierarchy and visual reading order need human inspection."
  - test: "Explanation affordances feel consistent"
    expected: "Home, permissions, and diagnostics modules expose the same info affordance and open a consistent explanation surface."
    why_human: "Consistency of visual affordances and explanation presentation is a product-surface judgment."
  - test: "GitHub handoff opens the expected destination"
    expected: "When an update is available, the CTA opens the GitHub release destination for full APK update rather than implying in-app installation."
    why_human: "External app handoff behavior requires device-side validation."
  - test: "Final EN and zh-CN copy reads as product copy"
    expected: "Active product surfaces no longer read like planning/debug iteration text in either locale."
    why_human: "Final language quality needs direct human review."
---

# Phase 14: Release 1.0 Polish And Update Architecture Verification Report

**Phase Goal:** Polish the accepted 1.0 product surface with real GitHub-release update state, reusable in-app explanations, cleaned product copy, and bounded implementation cleanup without reopening the major UI or permission architecture.
**Verified:** 2026-03-30T00:00:00Z
**Status:** human_needed

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | The app compares the installed version against GitHub latest-release metadata and shows a truthful in-product update state. | ✓ VERIFIED | `GitHubReleaseRepository`, `GitHubReleaseInfo`, `UpdateUiState`, `RuntimeStateViewModel`, and `HomeScreenUiState` now derive checking/current/available/failed update state for the home surface. |
| 2 | The update flow guides the user to GitHub for a full APK update handoff without implying fake seamless updating. | ✓ VERIFIED | The home update module only exposes a CTA when a real GitHub release URL exists, and `docs/API.md` explicitly describes the flow as GitHub handoff rather than in-app installation. |
| 3 | Product-facing copy is cleaned up, obsolete active string variants are removed, and EN plus zh-CN stay aligned on the active product surface. | ✓ VERIFIED | Active layout references now use the final base string names, the `_v2` and `_v3` leftovers are removed from the active surfaces, and `rg -n "_v2|_v3|closer to 1.0|更接近 1.0"` returns no matches in the locale files. |
| 4 | Relevant product modules expose a reusable explanation affordance with a consistent help surface. | ✓ VERIFIED | `ModuleExplanation`, `ModuleExplanationDialogFragment`, activity wiring, and layout affordances implement one reusable explanation path across home, permissions, and diagnostics. |
| 5 | The accepted 1.0 direction is tightened through bounded polish and cleaner screen-state driven implementation, and the project still compiles. | ✓ VERIFIED | `HomeScreenBinder` reduces `MainActivity` UI accumulation and the full bundle `./gradlew :app:testDebugUnitTest :app:assembleDebug` passes. |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/com/folklore25/ghosthand/GitHubReleaseRepository.kt` | Real latest-release metadata fetch path | ✓ EXISTS + SUBSTANTIVE | Fetches GitHub latest-release JSON and maps it into the update domain. |
| `app/src/main/java/com/folklore25/ghosthand/UpdateUiState.kt` | Update state contract | ✓ EXISTS + SUBSTANTIVE | Encodes checking, current, available, and failed update states for the home surface. |
| `app/src/main/java/com/folklore25/ghosthand/ModuleExplanation.kt` | Shared explanation catalog | ✓ EXISTS + SUBSTANTIVE | Central catalog for product-module explanations. |
| `app/src/main/java/com/folklore25/ghosthand/ModuleExplanationDialogFragment.kt` | Reusable explanation surface | ✓ EXISTS + SUBSTANTIVE | Shared dialog surface for module explanations. |
| `app/src/main/java/com/folklore25/ghosthand/HomeScreenBinder.kt` | Bounded binding cleanup | ✓ EXISTS + SUBSTANTIVE | Extracts home rendering logic out of `MainActivity`. |
| `docs/API.md` | Truthful update behavior note | ✓ EXISTS + SUBSTANTIVE | Explicitly states that product update behavior is a GitHub handoff, not an installer path. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `GitHubReleaseRepository` | home update state | `RuntimeStateViewModel.refreshReleaseInfo()` | ✓ WIRED | Remote release metadata is merged at the ViewModel layer instead of the runtime-state store. |
| `RuntimeStateViewModel` | home surface | `homeScreenState` | ✓ WIRED | Update state and runtime state are combined into one home UI model. |
| Home / Permissions / Diagnostics layouts | reusable explanations | `ModuleExplanationDialogFragment.show(...)` | ✓ WIRED | Each surface uses the shared explanation dialog rather than one-off help code. |
| `docs/API.md` | shipped product behavior | product update note | ✓ WIRED | The docs now match the GitHub handoff behavior exposed in the app. |

## Requirements Coverage

| Requirement | Status | Blocking Issue |
|-------------|--------|----------------|
| `POL-01` Real GitHub latest-release comparison and in-product update state | ✓ SATISFIED | - |
| `POL-02` Update flow guides the user to GitHub for full APK update | ✓ SATISFIED | Human device-side handoff check still pending |
| `POL-03` Product-facing copy cleaned and aligned | ✓ SATISFIED | Human language-quality review still pending |
| `POL-04` Reusable explanation affordance exists | ✓ SATISFIED | Human consistency review still pending |
| `POL-05` Accepted 1.0 direction is tightened with cleaner implementation and successful compile | ✓ SATISFIED | - |

## Anti-Patterns Found

None blocking. The bounded polish remained inside the accepted architecture and did not reopen the permission or front-end rewrite.

## Human Verification Required

### 1. Home update module reads clearly
**Test:** Inspect the home surface.
**Expected:** Installed version, latest release state, and update status read as one coherent product module.
**Why human:** Final hierarchy and visual balance are product judgments.

### 2. Explanation affordances feel consistent
**Test:** Open the info affordances on home, permissions, and diagnostics.
**Expected:** They use one consistent explanation surface and feel like one shared pattern.
**Why human:** Visual consistency and affordance quality require direct inspection.

### 3. GitHub handoff opens the expected destination
**Test:** Trigger the update CTA when an update is available.
**Expected:** The CTA opens the GitHub release destination for the full APK update.
**Why human:** External handoff behavior is device-side.

### 4. Final EN and zh-CN copy reads as product copy
**Test:** Inspect active surfaces in both locales if available.
**Expected:** No active copy reads like planning/debug iteration text.
**Why human:** Final copy quality requires human judgment.

## Gaps Summary

No code-level gaps were found. Remaining work is limited to manual product verification of the polished surface.

## Verification Metadata

**Verification approach:** Goal-backward against Phase 14 success criteria  
**Must-haves source:** `.planning/ROADMAP.md` plus `14-01/02/03-PLAN.md`  
**Automated checks:** 4 passed, 0 failed  
**Human checks required:** 4  
**Total verification time:** inline session

---
*Verified: 2026-03-30T00:00:00Z*  
*Verifier: Codex*
