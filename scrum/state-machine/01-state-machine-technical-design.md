# Finite State Machine — Technical Design Document

## 1. FSM Fundamentals

A **Finite State Machine (FSM)** is a computational model with:

- A **finite** set of states
- One **initial** state
- Zero or more **final** (terminal) states
- A set of **transitions** between states, triggered by **events**

### The Fundamental Equation

```
State(S) x Event(E) = Action(A) x State(S')
```

Given current state `S` and incoming event `E`: find the matching transition, execute the associated action `A`, and move to the new state `S'`.

### Deterministic FSM

Our library implements a **deterministic** FSM: for any given (state, event) pair, there is **at most one** matching transition. This simplifies lookup and guarantees predictable behavior.

### State Types

| Type | Description | Behavior on event |
|------|-------------|-------------------|
| **Initial** | The state the machine starts in | Transitions out on first event |
| **Regular** | Normal operating state | Processes events per transition rules |
| **Final** | Terminal state | All incoming events are **ignored** |

---

## 2. Core Concepts

### State

A **state** is a named condition or mode of the system. States are identified by a unique identifier within a machine instance. The identifier type is generic (`S`) — consumers can use `String`, `enum`, `Integer`, or any type.

```
DRAFT → INFO_COLLECTED → VALIDATING → APPROVED / REJECTED
```

### Event

An **event** is a signal that something happened. Events carry a type discriminator (`E`) and optional payload data. The type is generic — consumers define their own event types.

```
submit, validate, pass, fail, approve, reject
```

### Transition

A **transition** defines the path from one state to another, triggered by a specific event. A transition binds together:

- **Source state** — where the machine must be
- **Target state** — where the machine goes
- **Event type** — what triggers this transition
- **Guard** (optional) — condition that must be true
- **Actions** (optional) — side effects to execute

```
Transition: source=DRAFT, event=submit, target=INFO_COLLECTED
Guard:      context.getName() != null
Action:     validate fields, log audit
```

---

## 3. Transition Evaluation

### Lookup Strategy

Use an **indexed map** for O(1) transition lookup instead of linear scan:

```
Map<S, Map<E, Transition<S,E,C>>>
// keyed by: currentState → eventType → Transition
```

At build time, all transitions are indexed. At runtime, finding a transition for (currentState, event) is a constant-time hash lookup.

### Evaluation Order

When an event is fired, the machine follows this strict sequence:

```
1. Final state check    → if in final state, ignore event
2. Transition lookup    → O(1) by (currentState, eventType)
3. Interceptor pre-hook → can inspect/veto the transition
4. Guard evaluation     → boolean predicate on context
5. Exit action          → actions on the source state (leaving)
6. Transition action    → side effects of the transition itself
7. State update         → currentState = target
8. Entry action         → actions on the target state (entering)
9. Interceptor post-hook→ notification after transition completes
10. Listener notification → inform observers of the change
```

This ordering is critical. The guarantee is:

```
exit(source) → action(transition) → entry(target)
```

This matches UML state machine semantics and Spring State Machine's execution model.

---

## 4. Actions — Transition vs State

There are two distinct kinds of actions in a state machine. Understanding the difference is essential.

### Transition Actions

**When:** During the transition, after guard passes, before state changes.

**Purpose:** Side effects that belong to the *change itself* — things that happen *because* we are moving from A to B.

**Example:** Sending a notification that a KYC case moved from draft to validation.

```java
// Transition action: "submit" from DRAFT to INFO_COLLECTED
action(ctx -> auditLog.log("KYC submitted for validation"))
```

### State Actions (Entry/Exit)

**When:** Entry actions fire when *entering* a state. Exit actions fire when *leaving* a state.

**Purpose:** Side effects that belong to the *state itself* — things that happen *whenever* we are in this state, regardless of which transition brought us here.

**Example:** When entering INFO_COLLECTED, always perform age validation. When entering APPROVED, always log the result.

```java
// Entry action on INFO_COLLECTED: always runs when entering this state
.onEntry(ctx -> performAgeCheck(ctx))

// Exit action on DRAFT: always runs when leaving DRAFT
.onExit(ctx -> validateFields(ctx))
```

### Ordering Guarantee

```
exit(source)  →  transition action  →  entry(target)
```

Exit runs first (cleaning up source), then transition action (the change itself), then entry (setting up target).

---

## 5. Entry/Exit Actions — Deep Dive

Entry and exit actions are **state lifecycle hooks**. They are the most misunderstood concept in state machine design because their purpose overlaps with transition actions. This section clarifies when to use which.

### 5.1 What Are Entry/Exit Actions?

Each state can define:

- **`onEntry`** — actions that run every time the machine *enters* this state
- **`onExit`** — actions that run every time the machine *leaves* this state

They are **state-scoped**, not transition-scoped. This is the key distinction.

### 5.2 When Do They Fire?

```
              ┌──────────────────────────────────────┐
              │         fire(event)                   │
              │                                       │
              │  guard passes                         │
              │       │                               │
              │       ▼                               │
              │  ┌─────────┐    ┌──────────┐         │
              │  │ onExit  │───▶│ transition│───▶┌────┤
              │  │ source  │    │  action   │   │entry│
              │  └─────────┘    └──────────┘   │target│
              │                                └────┘
              └──────────────────────────────────────┘
```

- `onExit(DRAFT)` fires every time we leave DRAFT — regardless of which event or which target
- `onEntry(INFO_COLLECTED)` fires every time we enter INFO_COLLECTED — regardless of which source

### 5.3 Use Cases from Our KYC Project

These are real examples from `KycWorkflowService.java`:

#### Use Case 1: Validate on Exit from DRAFT

```java
var exitActions = Map.of(
    "DRAFT", VALIDATE_FIELDS    // ValidateFieldsAction
);
```

**Why exit action (not transition action)?** Because field validation applies to *leaving DRAFT*, not to any specific transition. Whether the user submits, saves, or cancels — if we're leaving DRAFT, we must validate. An exit action guarantees this regardless of which event triggers the departure.

**If it were a transition action instead:** You'd have to attach the same validation to *every* transition originating from DRAFT. If someone adds a new transition from DRAFT later, they might forget the validation — introducing a bug.

#### Use Case 2: Perform Validation on Entry to INFO_COLLECTED

```java
var entryActions = Map.of(
    "INFO_COLLECTED", PERFORM_VALIDATING    // PerformValidatingAction (age check)
);
```

**Why entry action (not transition action)?** Because the age check belongs to the *state* INFO_COLLECTED — it's what this state *does*. No matter how we arrive at INFO_COLLECTED (from DRAFT via submit, or from DRAFT via revision), we always need to validate age.

**If it were a transition action instead:** The age check would only run on the specific transition that has the action. If INFO_COLLECTED can be reached from multiple sources, you'd duplicate the action on each.

#### Use Case 3: Log Result on Entry to Terminal States

```java
var entryActions = Map.of(
    "APPROVED", LOG_RESULT,     // LogResultAction
    "REJECTED", LOG_RESULT      // LogResultAction
);
```

**Why entry action?** Because logging the result is a concern of the *terminal state* — when we reach APPROVED or REJECTED, we log. It doesn't matter which transition brought us here. The logging is tied to the destination, not the journey.

### 5.4 When to Use Entry/Exit vs Transition Actions

| Scenario | Use | Why |
|----------|-----|-----|
| Side effect depends on the **destination** | Entry action | Runs regardless of source |
| Side effect depends on the **source** | Exit action | Runs regardless of target |
| Side effect depends on the **transition itself** (specific event + path) | Transition action | Unique to that specific transition |
| Validation before leaving a state | Exit action | Guards all departures |
| Setup/initialization when arriving | Entry action | Guards all arrivals |
| Cleanup when departing | Exit action | Guards all departures |

### 5.5 Edge Cases

**Initial state entry:** When the machine starts, the entry action of the initial state does **not** fire. The machine simply begins in that state. Entry actions only fire on *transitions into* the state.

**Final state exit:** When the machine reaches a final state, the entry action fires (entering the final state). But since no events are processed in a final state, exit actions never fire for final states.

**Self-transition:** If a transition goes from A back to A (same source and target), the sequence is: `exit(A)` → `transition action` → `entry(A)`. The state is exited and re-entered, so both hooks fire.

---

## 6. Guard System

A **guard** is a boolean predicate evaluated against the context before a transition is allowed.

### Semantics

| Condition | Result |
|-----------|--------|
| No guard defined | Transition is **always allowed** |
| Guard returns `true` | Transition is **allowed** |
| Guard returns `false` | Transition is **blocked**, machine stays in current state |
| Guard throws exception | Transition is **blocked**, exception is propagated |

### Purpose

Guards enable **conditional transitions** — the same event can lead to different states depending on context:

```
VALIDATING --pass--> APPROVED    (guard: age >= 18)
VALIDATING --fail--> REJECTED    (guard: age < 18)
```

Both transitions listen to different events (`pass` vs `fail`), but in more complex scenarios, guards on the *same* event route to different targets.

### Guard Evaluation Timing

Guards are evaluated **before** any side effects (exit, action, entry). If the guard fails, nothing happens — no actions execute, no state changes. This ensures side effects only run when transitions actually occur.

---

## 7. Extended State (Context)

The **context** (`C`) is a typed data container that lives alongside the state machine. It holds runtime data that the machine reads and writes during transitions.

### Why Extended State?

A finite state machine has a finite number of states. But real systems need to carry data — form fields, API responses, calculation results. Extended state solves this by separating **state identity** (which state we're in) from **state data** (what we know).

### How It Works

- The context is passed to every action and guard
- Actions can read and modify the context
- Guards can read the context to make decisions
- The context persists across transitions

```java
// Guard reads from context
guard(ctx -> ctx.getAge() >= 18)

// Action writes to context
action(ctx -> ctx.setDecision("approved"))
```

### Context Type

The context type `C` is a generic parameter. Consumers define their own context class:

```java
record KycContext(String name, String placeOfBirth, LocalDate dateOfBirth) {}
```

This gives **type safety** — no casting, no `Map<String, Object>` soup.

---

## 8. Extensibility Points

### 8.1 StateMachineListener

A **read-only observer** that receives notifications about machine lifecycle events. Listeners cannot affect the machine's behavior — they only observe.

| Event | When |
|-------|------|
| `onTransitionStart` | Before transition executes |
| `onTransitionComplete` | After transition completes successfully |
| `onStateChanged` | After currentState changes |
| `onError` | When an exception occurs during transition |

**Use cases:** Logging, metrics, audit trails, UI updates.

### 8.2 StateMachineInterceptor

A **read-write hook** that can inspect and modify transition behavior. Interceptors run before/after transitions and can **veto** (block) transitions.

| Hook | When | Can veto? |
|------|------|-----------|
| `preTransition` | Before transition executes | Yes — return false to block |
| `postTransition` | After transition completes | No (notification only) |

**Use cases:** Authorization checks, audit logging with veto power, transaction management.

### 8.3 Relationship

```
Interceptor (can block)  →  runs before  →  Listener (read-only)
```

The interceptor is the gatekeeper; the listener is the observer. Use interceptors when you need to *control* transitions. Use listeners when you need to *react* to them.

---

## 9. Comparison Table

| Feature | Easy States | Spring SM | Our Design |
|---------|-------------|-----------|------------|
| **Generic types** | None (String only) | `<S,E>` | `<S,E,C>` (includes context) |
| **Transition lookup** | O(n) linear scan | O(1) indexed | O(1) indexed |
| **Guards** | No | Yes (reactive) | Yes (synchronous) |
| **Extended state** | No | Yes (`Map<Object,Object>`) | Yes (typed `C`) |
| **Entry/exit actions** | No | Yes (reactive) | Yes (synchronous) |
| **Listeners** | No | Yes | Yes |
| **Interceptors** | No | Yes | Yes |
| **Builder pattern** | Yes | Yes (fluent DSL) | Yes (fluent DSL) |
| **Framework dependency** | None | Spring + Reactor | **None** |
| **Reactive** | No | Yes (Mono/Flux) | No (synchronous) |
| **Pseudo-states** | No | Yes (CHOICE, FORK, etc.) | No |
| **Regions (orthogonal)** | No | Yes | No |
| **Hierarchical (nested)** | No | Yes | No |
| **Persistence** | No | Yes | No (future concern) |
| **~Lines of code** | ~300 | ~15,000 | ~400 (target) |

---

## 10. Scope Boundaries

### What We Include

- Generic `<S,E,C>` type system
- Deterministic FSM with indexed transition lookup
- Guards (synchronous boolean predicates)
- Actions (synchronous, on transitions and state entry/exit)
- Typed context (`C`) passed to all actions and guards
- Listener/observer pattern for lifecycle notifications
- Interceptor hooks for pre/post transition control
- Builder pattern for fluent machine construction
- Build-time validation (fail-fast on invalid definitions)

### What We Explicitly Exclude

| Feature | Why excluded |
|---------|-------------|
| **Reactive streams** (Mono/Flux) | Adds complexity; our use cases are synchronous |
| **Pseudo-states** (CHOICE, FORK, JOIN, HISTORY) | Not needed for simple workflows; adds UML complexity |
| **Regions / orthogonal states** | Not needed; our workflows are linear |
| **Hierarchical / nested states** | Not needed; no sub-machine composition |
| **Persistence** | Out of scope; can be added later as a separate concern |
| **Spring dependency** | Standalone library; no framework coupling |
| **Annotation-based config** | Runtime builder API only; no compile-time annotations |
| **Dynamic reconfiguration** | Machines are immutable after build |

### Future Considerations (Out of Scope Now)

- Persistence (serialize/deserialize machine state)
- Timer triggers (event after delay)
- Choice pseudo-state (guard-based routing without explicit events)
- Sub-machine delegation
