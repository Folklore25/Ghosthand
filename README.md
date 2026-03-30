## License

Ghosthand is licensed under the Mozilla Public License 2.0 (MPL-2.0).

This project uses the standard MPL-2.0 terms. Modifications to MPL-covered files must remain under MPL-2.0, while the project may be combined with other code in a larger work.

See the [LICENSE](./LICENSE) file for the full text.

# Ghosthand

Inspired by Karry × Orb 🔮 https://github.com/cflank/orb-eye

**Ghosthand** is an Android-native, agent-oriented control substrate for **OpenClaw-like mobile agents**.

It is designed for people building or operating:

- Android agents
- mobile AI assistants
- accessibility-based automation
- local loopback phone control APIs
- device-side tool-use systems
- screen-reading / screen-action pipelines
- smartphone agents that need structured UI access instead of brittle image-only automation

Ghosthand is **not** trying to be a consumer automation app, a no-code macro builder, or a generic RPA shell. The goal is narrower and more useful: provide a **truthful, inspectable, Android-side control plane** that an external agent can call through a **local HTTP API**.

---

## Why Ghosthand exists

Most mobile automation stacks fail in one of two ways:

1. they are too weak, exposing only taps, screenshots, and hope
2. they are too magical, hiding resolution logic and making agents guess why something worked or failed

Ghosthand takes a different route:

- expose a compact but powerful runtime API
- preserve structured UI truth where possible
- keep interaction results inspectable
- favor additive capability exposure over black-box “success”
- make the app itself a reliable substrate for **AI agents**, not merely a human UI tool

This makes Ghosthand especially useful for:

- **OpenClaw / Voyager / phone agents**
- Android accessibility automation
- local-first agent execution
- grounded UI control
- agent verification and runtime diagnostics
- mobile browsing, navigation, search, clipboard, notification, and app-launch tasks

---

## What Ghosthand is good at

Ghosthand is optimized for **agent consumption**, not just human tapping.

### Core strengths

- **Structured screen reading** via `/screen` and `/tree`
- **Selector-based interaction** via text, content description, and resource id
- **Transparent click resolution** with click-to-ancestor reconciliation metadata
- **Coordinate fallback** when a surface is visually interactive but accessibility structure is imperfect
- **Focused input control** via `/input` and `/setText`
- **Clipboard read/write** for agent data handoff
- **Notifications read/post** for notification-driven flows
- **Foreground app and runtime state visibility**
- **App launch by package name** via `/launch`
- **Foreground service based runtime stability**
- **Permission governance** for sensitive capabilities
- **GitHub-release-based update path** for full APK updates

### High-value agent primitives

Ghosthand currently exposes a local runtime surface centered on routes like:

- `/ping`
- `/commands`
- `/info`
- `/foreground`
- `/screen`
- `/tree`
- `/find`
- `/click`
- `/tap`
- `/input`
- `/scroll`
- `/swipe`
- `/wait`
- `/clipboard`
- `/notify`
- `/launch`
- `/back`
- `/home`
- `/recents`

This is the practical toolset an Android agent actually needs: **read UI, reason about UI, act on UI, verify result**.

---

## Why Ghosthand is agent-friendly

Ghosthand is unusually friendly to LLM-based or tool-using agents for a few concrete reasons.

### 1. Inspectable interaction instead of black-box success

When Ghosthand resolves a click, it can report how that happened:

- requested selector surface
- effective strategy
- whether contains fallback was used
- whether the matched node itself was clickable
- whether a clickable ancestor/wrapper was used
- ancestor depth
- resolved target identity

That means the agent does not have to guess whether a click landed directly, through a wrapper, or through a bounded fallback path.

### 2. Structured UI truth

Ghosthand separates multiple truth surfaces instead of pretending all UI readings mean the same thing:

- `/screen` = shaped actionable surface snapshot
- `/tree` = fuller structural accessibility truth
- `/foreground` = observer context
- `/wait` = change/settled-state primitive

This matters because mobile UI automation fails when tools collapse all state into one ambiguous “screen dump.”

### 3. Natural Android substrate

Ghosthand is built on the real Android control surfaces that matter:

- **AccessibilityService**
- **foreground service**
- **notification listener**
- **media projection** for screenshots
- **local loopback HTTP API**
- normal **package launch intent** paths

So the technical route is realistic and sustainable for a phone-side agent stack.

### 4. Bounded heuristics, not hidden magic

Ghosthand does use bounded convenience logic where it helps, but the design intent is to keep it:

- explicit
- inspectable
- non-destructive
- compatible with agent reasoning

That is important for debugging, evaluation, and future skill-layer orchestration.

---

## Technical route: why this architecture makes sense

Ghosthand’s technical route is intentionally conservative and platform-native.

### Control path

At a high level, Ghosthand works like this:

1. Android app runs locally on the phone
2. a foreground runtime hosts a **localhost API**
3. the runtime reads accessibility/UI state
4. an external agent calls the API over loopback
5. Ghosthand executes actions and returns structured results

This route avoids several common traps:

- no exposed remote control surface by default
- no dependency on OCR-only automation
- no fake abstraction pretending Android is a web page
- no giant intent DSL trying to predict every workflow

### Why accessibility + local API is the right baseline

For phone-side agents, a good platform has to balance:

- capability
- inspectability
- stability
- permission boundaries
- agent usability

Accessibility + local API + explicit runtime state is a good baseline because it gives:

- structured node access
- actionable selectors
- bounded gesture support
- grounded runtime diagnostics
- clear permission and capability governance

### Why this is better than “just screenshots”

Image-only phone automation can work, but it is wasteful, brittle, and hard to debug.

Ghosthand is stronger when the app exposes:

- text
- content descriptions
- resource ids
- bounds
- focus/editability
- clickability / wrapper resolution
- runtime truth signals

Screenshots still matter, but they are part of the stack, not the whole stack.

---

## Product model

Ghosthand is built around a few explicit product ideas.

### 1. Permission governance matters

System authorization is not the same thing as agent usability.

Ghosthand separates:

- system-granted capability
- app-level policy
- effective usable state

That makes it possible to grant Android-side permission while still denying Ghosthand/OpenClaw use of that capability.

### 2. Diagnostics should exist, but not dominate

The app keeps runtime truth visible, but the default surface is not supposed to be a cluttered engineering panel.

The product direction is:

- clean operator home surface
- dedicated permissions governance page
- secondary diagnostics page
- advanced/root entry kept subordinate

### 3. Full-update model, not fake silent update

Ghosthand checks GitHub release metadata and guides users to a **full APK update** path instead of pretending Android can always do seamless silent updates.

---

## Example use cases

Ghosthand is useful for workflows like:

- launch an app by package name
- inspect current foreground app
- read the current structured screen
- find an element by text, content description, or resource id
- click through nested wrappers with transparent resolution metadata
- type into a focused input
- scroll or swipe and verify what changed
- read or write clipboard contents
- post or inspect notifications
- build phone-side agent loops such as:
  - open app
  - locate target
  - click target
  - wait for state change
  - read next screen
  - continue

That makes it a strong fit for:

- mobile browser agents
- Android research tools
- agent evaluation harnesses
- local phone copilots
- device-side AI operators
- loopback-controlled mobile assistants

---

## Design principles

Ghosthand follows a few principles that matter for long-term agent tooling.

### Expose capability, do not over-hide it

If the platform knows something truthful, it should prefer exposing it rather than hiding it for the sake of short-term success rate.

### Keep heuristics bounded and inspectable

Helpful fallbacks are acceptable.
Uninspectable magic is not.

### Preserve structural truth

A shaped actionable view is useful.
A fuller structural truth surface should still remain available.

### Separate platform from skill

Ghosthand is the **platform substrate**.
A future `ghosthand-skill` or ClawHub package can handle:

- prompting
- selector-choice discipline
- escalation defaults
- task-specific operator guidance

That separation keeps the app reusable and honest.

---

## Current positioning

Ghosthand is best understood as:

- an **Android agent control substrate**
- a **mobile accessibility automation runtime**
- a **localhost phone API for AI agents**
- an **OpenClaw-friendly smartphone tool layer**
- a **structured alternative to blind screenshot-only phone control**

It is especially relevant if you are searching for topics like:

- Android AI agent
- phone agent infrastructure
- OpenClaw tool
- Android accessibility API for LLM agents
- local mobile automation API
- on-device smartphone agent control
- Android UI automation for AI
- mobile agent runtime
- loopback Android control plane
- agent-friendly Android automation
- structured mobile UI control

---

## Non-goals

Ghosthand is not trying to be:

- a mass-market automation app
- a cloud control dashboard
- a full MDM product
- a no-code shortcut builder
- a generic Android intent laboratory
- an image-only agent wrapper

That focus is part of the point.

---

## License

Ghosthand uses **MPL 2.0** for the core project files.

---

## Who this is for

Ghosthand is most useful if you are the kind of user who:

- builds or tests Android agents
- runs OpenClaw-like systems
- wants local, inspectable phone control
- values structured truth over black-box success
- is comfortable with Android permissions, accessibility, and runtime debugging
- wants a practical substrate for mobile AI action loops

If that is what you are looking for, Ghosthand is built for you.
