# Core Library Proposal — Lightweight FSM

Package: `io.github.khezyapp.fsm.core`

Standalone Java library. Zero framework dependencies. Generic `<S,E,C>` type system.

---

## 1. Package Structure

```
io.github.khezyapp.fsm.core/
├── model/
│   ├── State.java                    — state definition
│   ├── Event.java                    — event definition
│   └── Transition.java               — transition definition
├── api/
│   ├── StateMachine.java             — main machine interface
│   ├── Action.java                   — side effect contract
│   ├── Guard.java                    — boolean predicate contract
│   ├── StateMachineListener.java     — observer interface
│   └── StateMachineInterceptor.java  — pre/post hook interface
├── builder/
│   ├── StateMachineBuilder.java      — fluent builder API
│   └── StateMachineBuilderException.java — validation errors
└── impl/
    ├── DefaultStateMachine.java      — runtime engine
    └── TransitionIndex.java          — O(1) transition lookup
```

---

## 2. Data Model

### 2.1 State

```java
package io.github.khezyapp.fsm.core.model;

import java.util.List;
import java.util.Map;

public record State<S, C>(
    S id,
    List<Action<C>> onEntry,
    List<Action<C>> onExit
) {
    // Compact constructor: null lists become empty
    public State {
        onEntry = onEntry != null ? List.copyOf(onEntry) : List.of();
        onExit  = onExit  != null ? List.copyOf(onExit)  : List.of();
    }

    // Convenience: simple state with no entry/exit actions
    public static <S, C> State<S, C> of(S id) {
        return new State<>(id, null, null);
    }
}
```

**Fields:**
- `id` — unique state identifier (generic `S`)
- `onEntry` — actions executed every time the machine enters this state (immutable list)
- `onExit` — actions executed every time the machine leaves this state (immutable list)

### 2.2 Event

```java
package io.github.khezyapp.fsm.core.model;

import java.util.Map;

public record Event<E>(
    E type,
    Map<String, Object> payload
) {
    // Compact constructor: null payload becomes empty map
    public Event {
        payload = payload != null ? Map.copyOf(payload) : Map.of();
    }

    // Convenience: event without payload
    public static <E> Event<E> of(E type) {
        return new Event<>(type, null);
    }
}
```

**Fields:**
- `type` — event type discriminator (generic `E`)
- `payload` — optional key-value data attached to the event (immutable map)

### 2.3 Transition

```java
package io.github.khezyapp.fsm.core.model;

import io.github.khezyapp.fsm.core.api.Action;
import io.github.khezyapp.fsm.core.api.Guard;

import java.util.List;

public record Transition<S, E, C>(
    String id,
    S source,
    S target,
    E eventType,
    Guard<C> guard,
    List<Action<C>> actions
) {
    // Compact constructor: null guard means always pass, null actions means no-op
    public Transition {
        actions = actions != null ? List.copyOf(actions) : List.of();
    }
}
```

**Fields:**
- `id` — transition identifier (for logging/debugging)
- `source` — source state id
- `target` — target state id
- `eventType` — event type that triggers this transition
- `guard` — optional condition; `null` means always allowed
- `actions` — optional side effects executed during transition (immutable list)

---

## 3. Contract Interfaces

### 3.1 Action

```java
package io.github.khezyapp.fsm.core.api;

@FunctionalInterface
public interface Action<C> {
    /**
     * Execute side effect.
     *
     * @param context the machine context (read/write)
     * @throws Exception if the action fails (propagated to caller)
     */
    void execute(C context) throws Exception;
}
```

**Design notes:**
- `@FunctionalInterface` — lambda-friendly
- Generic `C` — consumer provides their own context type
- Checked exception — caller decides how to handle failures

### 3.2 Guard

```java
package io.github.khezyapp.fsm.core.api;

@FunctionalInterface
public interface Guard<C> {
    /**
     * Evaluate whether a transition is allowed.
     *
     * @param context the machine context (read-only)
     * @return true if the transition should proceed
     * @throws Exception if evaluation fails (treated as denial)
     */
    boolean evaluate(C context) throws Exception;
}
```

**Design notes:**
- `@FunctionalInterface` — lambda-friendly
- Exception during evaluation = transition denied (safe default)

### 3.3 StateMachine

```java
package io.github.khezyapp.fsm.core.api;

import io.github.khezyapp.fsm.core.model.Event;
import io.github.khezyapp.fsm.core.model.State;
import io.github.khezyapp.fsm.core.model.Transition;

import java.util.Optional;
import java.util.Set;

public interface StateMachine<S, E, C> {

    /**
     * Fire an event. The machine evaluates the current state + event,
     * finds a matching transition, runs guard/actions/entry/exit,
     * and updates the current state.
     *
     * @param event the event to process
     * @param context the mutable context
     * @return the new current state after processing
     * @throws IllegalStateException if an action fails
     */
    State<S, C> fire(Event<E> event, C context);

    /**
     * Get the current state.
     */
    State<S, C> getCurrentState();

    /**
     * Get the machine's initial state.
     */
    State<S, C> getInitialState();

    /**
     * Get all registered states.
     */
    Set<State<S, C>> getStates();

    /**
     * Get all registered transitions.
     */
    Set<Transition<S, E, C>> getTransitions();

    /**
     * Check if the machine is in a final state.
     */
    boolean isFinal();

    /**
     * Register a listener for lifecycle events.
     */
    void addListener(StateMachineListener<S, E> listener);

    /**
     * Remove a listener.
     */
    void removeListener(StateMachineListener<S, E> listener);

    /**
     * Register an interceptor for pre/post transition hooks.
     */
    void addInterceptor(StateMachineInterceptor<S, E, C> interceptor);

    /**
     * Remove an interceptor.
     */
    void removeInterceptor(StateMachineInterceptor<S, E, C> interceptor);
}
```

### 3.4 StateMachineListener

```java
package io.github.khezyapp.fsm.core.api;

import io.github.khezyapp.fsm.core.model.Event;
import io.github.khezyapp.fsm.core.model.Transition;

public interface StateMachineListener<S, E> {

    /**
     * Called before a transition executes.
     */
    default void onTransitionStart(S sourceState, S targetState, Event<E> event) {}

    /**
     * Called after a transition completes successfully.
     */
    default void onTransitionComplete(S sourceState, S targetState, Event<E> event) {}

    /**
     * Called when the current state changes.
     */
    default void onStateChanged(S oldState, S newState) {}

    /**
     * Called when an error occurs during transition.
     */
    default void onError(S currentState, Event<E> event, Exception exception) {}
}
```

**Design notes:**
- All methods are `default` (no-op) — implement only what you need
- Read-only — cannot affect machine behavior

### 3.5 StateMachineInterceptor

```java
package io.github.khezyapp.fsm.core.api;

import io.github.khezyapp.fsm.core.model.Event;

public interface StateMachineInterceptor<S, E, C> {

    /**
     * Called before a transition executes.
     * Return false to veto (block) the transition.
     *
     * @param sourceState current state
     * @param targetState proposed target state
     * @param event the triggering event
     * @param context the mutable context
     * @return true to allow, false to block
     */
    default boolean preTransition(S sourceState, S targetState, Event<E> event, C context) {
        return true;
    }

    /**
     * Called after a transition completes successfully.
     *
     * @param sourceState state before transition
     * @param targetState state after transition
     * @param event the triggering event
     * @param context the mutable context
     */
    default void postTransition(S sourceState, S targetState, Event<E> event, C context) {}
}
```

**Design notes:**
- `preTransition` returns `boolean` — `false` blocks the transition entirely
- `postTransition` is notification-only (no veto)
- Interceptors run **before** listeners

---

## 4. Builder API

### 4.1 Interface

```java
package io.github.khezyapp.fsm.core.builder;

import io.github.khezyapp.fsm.core.api.Action;
import io.github.khezyapp.fsm.core.api.Guard;
import io.github.khezyapp.fsm.core.api.StateMachine;

import java.util.Set;

public class StateMachineBuilder<S, E, C> {

    // --- State configuration ---

    public StateMachineBuilder<S, E, C> initialState(S stateId) { ... }

    public StateMachineBuilder<S, E, C> state(S stateId) { ... }

    public StateMachineBuilder<S, E, C> state(S stateId,
                                               java.util.List<Action<C>> onEntry,
                                               java.util.List<Action<C>> onExit) { ... }

    public StateMachineBuilder<S, E, C> finalState(S stateId) { ... }

    // --- Transition configuration ---

    public TransitionConfigurer<S, E, C> transition(String id, S source, S target, E eventType) { ... }

    // --- Build ---

    public StateMachine<S, E, C> build() throws StateMachineBuilderException { ... }
}
```

### 4.2 TransitionConfigurer (Fluent Sub-Builder)

```java
public class TransitionConfigurer<S, E, C> {

    public TransitionConfigurer<S, E, C> guard(Guard<C> guard) { ... }

    public TransitionConfigurer<S, E, C> action(Action<C> action) { ... }

    public TransitionConfigurer<S, E, C> actions(java.util.List<Action<C>> actions) { ... }

    public StateMachineBuilder<S, E, C> and() { ... }  // return to parent builder
}
```

### 4.3 Usage Example (KYC Workflow)

```java
record KycContext(String name, String placeOfBirth, LocalDate dateOfBirth, String decision) {}

StateMachine<String, String, KycContext> machine = StateMachineBuilder
    .<String, String, KycContext>builder()
    .initialState("DRAFT")
    .state("DRAFT")
    .state("INFO_COLLECTED")
    .state("VALIDATING")
    .finalState("APPROVED")
    .finalState("REJECTED")

    // Transition: submit from DRAFT → INFO_COLLECTED
    .transition("t1", "DRAFT", "INFO_COLLECTED", "submit")
        .guard(ctx -> ctx.name() != null && !ctx.name().isBlank())
        .action(ctx -> log.info("Fields submitted: {}", ctx.name()))
    .and()

    // Transition: validate from INFO_COLLECTED → VALIDATING
    .transition("t2", "INFO_COLLECTED", "VALIDATING", "validate")
    .and()

    // Transition: pass from VALIDATING → APPROVED
    .transition("t3", "VALIDATING", "APPROVED", "pass")
        .action(ctx -> log.info("Approved: {}", ctx.name()))
    .and()

    // Transition: fail from VALIDATING → REJECTED
    .transition("t4", "VALIDATING", "REJECTED", "fail")
        .action(ctx -> log.info("Rejected: {}", ctx.name()))
    .and()

    .build();

// Usage
KycContext ctx = new KycContext("Alice", "Hanoi", LocalDate.of(1995, 5, 20), null);
machine.fire(Event.of("submit"), ctx);
// currentState: INFO_COLLECTED

machine.fire(Event.of("validate"), ctx);
// currentState: VALIDATING

machine.fire(Event.of("pass"), ctx);
// currentState: APPROVED
// machine.isFinal() == true
```

### 4.4 Entry/Exit Actions Example

```java
StateMachine<String, String, KycContext> machine = StateMachineBuilder
    .<String, String, KycContext>builder()
    .initialState("DRAFT")

    // DRAFT with onExit: validate fields before leaving
    .state("DRAFT",
        List.of(),                                    // onEntry: none
        List.of(ctx -> validateFields(ctx))           // onExit: validate
    )

    // INFO_COLLECTED with onEntry: always perform age check
    .state("INFO_COLLECTED",
        List.of(ctx -> performAgeCheck(ctx)),         // onEntry: age check
        List.of()                                     // onExit: none
    )

    .finalState("APPROVED")
    .finalState("REJECTED")
    // ... transitions ...
    .build();
```

---

## 5. fire() Algorithm

The `DefaultStateMachine.fire()` method implements this sequence:

```
fire(Event<E> event, C context):
│
├─ 1. FINAL STATE CHECK
│     if (currentState is final state)
│       → return currentState  (ignore event)
│
├─ 2. NULL EVENT CHECK
│     if (event == null)
│       → return currentState  (ignore)
│
├─ 3. TRANSITION LOOKUP
│     transition = transitionIndex.get(currentState.id, event.type)
│     if (transition == null)
│       → notify listeners: onError (TransitionNotFound)
│       → return currentState  (no matching transition)
│
├─ 4. INTERCEPTOR PRE-HOOK
│     for each interceptor:
│       if (interceptor.preTransition(...) == false)
│         → notify listeners: onError (TransitionVetoed)
│         → return currentState  (blocked)
│
├─ 5. GUARD EVALUATION
│     if (transition.guard != null && !guard.evaluate(context))
│       → return currentState  (guard failed, no side effects)
│
├─ 6. EXIT ACTIONS
│     for each action in currentState.onExit:
│       action.execute(context)
│
├─ 7. TRANSITION ACTIONS
│     for each action in transition.actions:
│       action.execute(context)
│
├─ 8. STATE UPDATE
│     oldState = currentState
│     currentState = stateMap.get(transition.target)
│
├─ 9. ENTRY ACTIONS
│     for each action in currentState.onEntry:
│       action.execute(context)
│
├─ 10. INTERCEPTOR POST-HOOK
│      for each interceptor:
│        interceptor.postTransition(...)
│
├─ 11. LISTENER NOTIFICATION
│      for each listener:
│        listener.onStateChanged(oldState, currentState)
│        listener.onTransitionComplete(oldState, currentState, event)
│
└─ return currentState
```

**Error handling:** If any action throws, the exception propagates to the caller. The machine's `currentState` remains at the state it was in when the error occurred (partial transitions are not rolled back — actions are expected to be idempotent or handle their own cleanup).

---

## 6. Transition Index

### 6.1 Structure

```java
class TransitionIndex<S, E, C> {
    // Primary index: currentStateId -> eventType -> Transition
    private final Map<S, Map<E, Transition<S, E, C>>> index;

    // All transitions (for getTransitions())
    private final Set<Transition<S, E, C>> allTransitions;
}
```

### 6.2 Build-Time Indexing

During `build()`, all transitions are inserted into the index:

```java
// For each transition:
index.computeIfAbsent(transition.source(), k -> new HashMap<>())
     .put(transition.eventType(), transition);
```

### 6.3 Runtime Lookup

```java
Transition<S, E, C> find(S currentState, E eventType) {
    return index
        .getOrDefault(currentState, Map.of())
        .get(eventType);
}
```

**O(1)** average case (hash lookup). **O(n)** worst case only on hash collision.

---

## 7. Validation

At `build()` time, the builder validates the complete definition. All violations are collected and reported together.

### 7.1 Validation Rules

| # | Rule | Error |
|---|------|-------|
| 1 | Initial state must be set | `InitialStateNotSet` |
| 2 | Initial state must exist in states set | `InitialStateNotFound` |
| 3 | At least one state defined | `NoStatesDefined` |
| 4 | All transition source states exist | `SourceStateNotFound` |
| 5 | All transition target states exist | `TargetStateNotFound` |
| 6 | No duplicate transitions (same source + same event) | `DuplicateTransition` |
| 7 | Final states exist in states set | `FinalStateNotFound` |

### 7.2 Exception

```java
public class StateMachineBuilderException extends IllegalStateException {
    private final List<String> violations;

    public StateMachineBuilderException(List<String> violations) {
        super("Invalid state machine definition: " + violations);
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
```

---

## 8. Class Summary

| Class/Interface | Package | Role |
|-----------------|---------|------|
| `State<S,C>` | model | Immutable state definition with id + entry/exit actions |
| `Event<E>` | model | Immutable event with type + payload |
| `Transition<S,E,C>` | model | Immutable transition with source, target, event, guard, actions |
| `StateMachine<S,E,C>` | api | Main machine interface: fire, query, listen, intercept |
| `Action<C>` | api | `@FunctionalInterface` — side effect contract |
| `Guard<C>` | api | `@FunctionalInterface` — boolean predicate contract |
| `StateMachineListener<S,E>` | api | Observer: lifecycle notifications (read-only) |
| `StateMachineInterceptor<S,E,C>` | api | Hook: pre/post transition (can veto) |
| `StateMachineBuilder<S,E,C>` | builder | Fluent API for constructing machines |
| `TransitionConfigurer<S,E,C>` | builder | Fluent sub-builder for individual transitions |
| `StateMachineBuilderException` | builder | Validation error collection |
| `DefaultStateMachine<S,E,C>` | impl | Runtime engine: fire(), indexed lookup, lifecycle |
| `TransitionIndex<S,E,C>` | impl | O(1) transition lookup structure |

---

## 9. Thread Safety

- `StateMachine` instances are **mutable** (currentState changes on fire)
- `fire()` is **synchronized** — one event at a time per machine instance
- Listeners and interceptors are stored in `CopyOnWriteArrayList` — safe to add/remove during iteration
- `State`, `Event`, `Transition` records are **immutable**
- For concurrent access to the same workflow, consumers should synchronize externally or use one machine per workflow instance

---

## 10. Future Extension Points

These are **not** in the core library but the design accommodates them:

| Extension | How to extend |
|-----------|--------------|
| **Persistence** | Add `StateMachineSerializer` that reads/writes `currentState` + `context` to storage |
| **Timer triggers** | Add `Trigger` interface to `Transition`; `DefaultStateMachine` calls `trigger.arm()` on entry |
| **Choice pseudo-state** | Add `ChoiceTransition` with multiple target + guard pairs |
| **Async actions** | Add `AsyncAction<C>` returning `CompletableFuture`; `fire()` returns `CompletableFuture<State>` |
| **Spring integration** | Create `SpringStateMachineConfigurer` that wires beans as actions/guards |
| **YAML config** | Create `YamlStateMachineLoader` that reads YAML and calls `StateMachineBuilder` |
