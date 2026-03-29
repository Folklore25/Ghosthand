# Phase 17 Exploratory Reconciliation Note 01

## Source

Latest zero-context OpenClaw exploratory evaluation against Ghosthand.

## Accepted Truth Anchors

- `/wait` already exists and is accepted operator-path truth through Phase 10 validation, playbooks, and runbook discipline.
- Wrapper-driven `/click` is already more inspectable than before through explicit resolution metadata; broad wrapper-opacity is no longer the main repo-truth problem.
- Remaining selector friction is narrower than the earlier broad clickable-gate interpretation and is increasingly about natural selector-surface choice.

## Purpose

Preserve the useful agent-perspective signal while correcting conclusions that contradict already accepted platform truth.

## Four-Bucket Classification

### 1. Real substrate defects still worth improving

- App launch / app open handoff remains a real candidate if the current platform still lacks a clean launch/open primitive for natural zero-context use.
- Scroll/content-changed/snapshot-token coherence remains a real candidate if the exploratory run reinforces that action feedback and post-action structured truth still diverge materially.
- Selector-surface discoverability may still be partially under-expressive if the platform does not yet help a zero-context agent distinguish when visible text is more likely to live in `desc` / `contentDescription` / `viewId` rather than `text`.

These remain candidates, not automatic accepted defects. Phase 17 preserves them as follow-up directions only after correcting the exploratory misclassifications first.

### 2. Discoverability / natural-consumption weaknesses

- `/wait` not being naturally discovered or correctly consumed in a zero-context run.
- Existing route and selector capabilities not being surfaced clearly enough for a zero-context agent to choose the next best primitive.
- Natural understanding of how to interpret post-action settle truth, especially when content changes within the same activity.

This is the strongest trustworthy signal from the exploratory run: existing Ghosthand capability is present, but natural zero-context consumption is still weaker than it should be.

### 3. App / platform constraints

- Reddit-like long visible content living in `contentDescription` instead of `text`.
- Lazy list / below-the-fold rendering limits and partial visibility constraints imposed by the target app surface.
- Visually interactive surfaces that are not represented cleanly by the accessibility tree.

These constraints should remain distinct from Ghosthand defects even when they still create real downstream agent friction.

### 4. Agent misclassification / rejected conclusions

- “Ghosthand lacks `/wait`” is rejected. `/wait` already exists and is accepted operator-path truth.
- Any broad claim that wrapper-driven `/click` is still fundamentally opaque is rejected; that is no longer the main repo-truth problem after accepted wrapper-resolution evidence.
- Any broad claim that the platform simply lacks settled swipe/interaction primitives must be treated skeptically when accepted repo truth already proves those primitives exist.

Phase 17 exists specifically to prevent these exploratory misclassifications from becoming repository truth.

## Reconciliation Conclusion

The exploratory run is useful evidence of natural discoverability weakness and remaining substrate-owned friction, but it is not valid as a raw defect list. The honest follow-up is to improve discoverability and narrow the real remaining platform-owned gap, not to mislabel existing capabilities as absent.

## Narrow Next-Direction Candidate

Most likely next platform-owned direction after this reconciliation:

1. launch/open handoff, if still genuinely absent
2. scroll/snapshot coherence, if the evidence still shows a real truth mismatch
3. selector-surface discoverability, if the strongest signal is still zero-context route/selector choice confusion
