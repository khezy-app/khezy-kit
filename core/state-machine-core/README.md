# khezy State Machine Core

A lightweight, zero-dependency Java library for building and executing **deterministic finite state machines (FSM)**. Generic type-safe parameters, fluent builder API, O(1) transition lookup, and full lifecycle hooks — all in under 500 lines of code.

**Package:** `io.github.khezyapp.fsm.core`

---

## Motivation

State machines are a natural fit for modelling business workflows — order processing, document approval, user onboarding, and more. In any non-trivial Java project you quickly find yourself writing the same boilerplate: if-else chains, state enums, manual guards, and scattered side effects.

We wanted a clean, reusable implementation of the FSM pattern that we could drop into any project without pulling in a heavy framework. Our design is inspired by easy-states and Spring State Machine (whose future versions are no longer open source). This library distills what we learned into a general-purpose, zero-dependency core.

- **Zero framework dependencies** — pure Java, no Spring, no Reactor
- **Generic type parameters** — typed state IDs (`<S>`), event discriminators (`<E>`), and extended context (`<C>`)
- **O(1) transition lookup** — suitable for high-throughput workflows
- **Lifecycle hooks** — interceptors (pre-transition veto) and listeners (post-transition observation)
- **Fail-fast validation** — 7 rules checked at build time, before the first event

No annotations, no code generation, no XML config. Just a fluent builder and a synchronous runtime engine. Maybe it helps you too.

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.khezyapp</groupId>
    <artifactId>state-machine-core</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.khezyapp:state-machine-core:1.1.0'
```

## Example Usage

Below is a complete KYC (Know Your Customer) workflow using Cambodian names and locations. The workflow has five states and five events:

```
DRAFT ──submit──► INFO_COLLECTED ──validate──► VALIDATING ──pass──► APPROVED
                       │                             │
                       │                             └──fail──► REJECTED
                       │
                       └──revision──► DRAFT
```

```java
import io.github.khezyapp.fsm.core.api.StateMachine;
import io.github.khezyapp.fsm.core.builder.StateMachineBuilder;
import io.github.khezyapp.fsm.core.model.Event;

// --- 1. Define your typed context ---
record KycContext(String name, String placeOfBirth, String dateOfBirth, String decision) {}

// --- 2. Build the machine ---
StateMachine<String, String, KycContext> machine =
    StateMachineBuilder.<String, String, KycContext>builder()
        .initialState("DRAFT")
        .state("DRAFT")
        .state("INFO_COLLECTED")
        .state("VALIDATING")
        .finalState("APPROVED")
        .finalState("REJECTED")

        // DRAFT -> INFO_COLLECTED (requires non-blank name)
        .transition("submit", "DRAFT", "INFO_COLLECTED", "submit")
            .guard(ctx -> ctx.name() != null && !ctx.name().isBlank())
            .action(ctx -> System.out.println("Submitted by: " + ctx.name()))
        .and()

        // INFO_COLLECTED -> DRAFT (revision)
        .transition("revision", "INFO_COLLECTED", "DRAFT", "revision")
        .and()

        // INFO_COLLECTED -> VALIDATING
        .transition("validate", "INFO_COLLECTED", "VALIDATING", "validate")
        .and()

        // VALIDATING -> APPROVED
        .transition("pass", "VALIDATING", "APPROVED", "pass")
            .action(ctx -> System.out.println("Approved: " + ctx.name()))
        .and()

        // VALIDATING -> REJECTED
        .transition("fail", "VALIDATING", "REJECTED", "fail")
            .action(ctx -> System.out.println("Rejected: " + ctx.name()))
        .and()

        .build();

// --- 3. Fire events ---
KycContext sok = new KycContext("Sok", "Phnom Penh", "1995-05-20", null);

machine.fire(Event.of("submit"), sok);
System.out.println(machine.getCurrentState().id());  // INFO_COLLECTED

machine.fire(Event.of("validate"), sok);
System.out.println(machine.getCurrentState().id());  // VALIDATING

machine.fire(Event.of("pass"), sok);
System.out.println(machine.getCurrentState().id());  // APPROVED
System.out.println(machine.isFinal());                // true
```

### With State Lifecycle Actions

Entry and exit actions fire automatically whenever a state boundary is crossed:

```java
machine = StateMachineBuilder.<String, String, KycContext>builder()
    .initialState("DRAFT")

    // onExit fires when leaving DRAFT (regardless of target)
    .state("DRAFT",
        List.of(),                                   // onEntry: none
        List.of(ctx -> validateFields(ctx))          // onExit: always validate
    )

    // onEntry fires when entering INFO_COLLECTED (regardless of source)
    .state("INFO_COLLECTED",
        List.of(ctx -> performAgeCheck(ctx)),        // onEntry: always check age
        List.of()                                    // onExit: none
    )

    .finalState("APPROVED")
    .transition("submit", "DRAFT", "INFO_COLLECTED", "submit")
        .guard(ctx -> ctx.name() != null)
    .and()
    .build();
```

## Guard-Driven Branching (CHOICE)

Since `1.1.0`, a single event can route to **different target states** based on context. Register several transitions for the same `(source, event)` pair — each with a guard. At `fire()` time the machine picks the **first candidate whose guard returns `true`** (definition order). A trailing guard-less transition acts as a deterministic fallback (`else`).

```java
record KycContext(String decision) {}

StateMachine<String, String, KycContext> machine =
    StateMachineBuilder.<String, String, KycContext>builder()
        .initialState("manual_review")
        .state("manual_review")
        .state("revision")
        .finalState("approved")
        .finalState("rejected")

        .transition("to-approved", "manual_review", "approved", "KYC_RESPONSE_RECEIVED")
            .guard(ctx -> "approved".equals(ctx.decision()))
        .and()
        .transition("to-rejected", "manual_review", "rejected", "KYC_RESPONSE_RECEIVED")
            .guard(ctx -> "rejected".equals(ctx.decision()))
        .and()
        .transition("to-revision", "manual_review", "revision", "KYC_RESPONSE_RECEIVED")
            .guard(ctx -> "revision".equals(ctx.decision()))
        .and()
        .build();

machine.fire(Event.of("KYC_RESPONSE_RECEIVED"), new KycContext("approved"));
machine.getCurrentState().id();                              // "approved"
machine.getLastTransition().orElseThrow().id();              // "to-approved"
```

`getLastTransition()` returns the winning transition (`Optional` empty when the last `fire()` was a no-op). Multiple candidates sharing a `(source, event)` pair must have at least one guard; all-guard-less siblings are rejected at build time as `AmbiguousTransition`.

## Resuming a Persisted Machine

Since `1.1.0`, you can rebuild a machine positioned at a saved `currentState` with `resume(...)`, so processing continues from where it left off. The resumed state must be a defined state (else `ResumeStateNotFound` at build time).

```java
StateMachine<String, String, KycContext> machine =
    StateMachineBuilder.<String, String, KycContext>builder()
        .initialState("manual_review")
        .state("manual_review")
        .finalState("approved")
        .transition("approve", "manual_review", "approved", "approve")
        .and()
        .resume("manual_review")     // start at the saved state instead of initial
        .build();

machine.fire(Event.of("approve"), new KycContext("approved"));
machine.isFinal();                   // true
```

## Transition Algorithm

When `fire(event, context)` is called, the machine follows this strict sequence:

1. **Final state check** — if machine is in a terminal state, ignore the event
2. **Null event check** — silently ignore null events
3. **Transition lookup** — O(1) hash lookup returns ordered candidates by (currentState, eventType)
4. **Guard selection** — pick the first candidate whose guard returns `true` (null guard always matches)
5. **Interceptor pre-hooks** — each interceptor can veto the selected transition
6. **Notify `onTransitionStart`** — inform all listeners
7. **Exit actions** — `onExit` of the source state
8. **Transition actions** — actions attached to the selected transition
9. **State update** — `currentState = target`
10. **Entry actions** — `onEntry` of the target state
11. **Post-hooks** — interceptor `postTransition` + listener notifications

If no candidate's guard passes (or no candidate exists), the event is a no-op: the state is unchanged and listeners receive `onError`. If any action throws, the machine catches the exception, notifies listeners via `onError`, and throws a `TransitionExecutionException` carrying the source state, event type, and target state.

## Extensibility

### Listeners (Read-Only Observation)

Implement `StateMachineListener<S, E>` to react to lifecycle events without influencing the machine:

```java
machine.addListener(new StateMachineListener<String, String>() {
    @Override
    public void onStateChanged(String oldState, String newState) {
        System.out.println("Moved from " + oldState + " to " + newState);
    }

    @Override
    public void onError(String state, Event<String, ?> event, Exception ex) {
        System.err.println("Error in state " + state + " on event " + event.type());
    }
});
```

All methods have default no-op implementations — override only what you need.

### Interceptors (Can Veto Transitions)

Implement `StateMachineInterceptor<S, E, C>` for cross-cutting concerns like authorization, tracing, or transaction management:

```java
machine.addInterceptor(new StateMachineInterceptor<String, String, KycContext>() {
    @Override
    public boolean preTransition(String source, String target,
                                  Event<String, ?> event, KycContext ctx) {
        // Enrich the event message with tracing headers
        event.message().withHeader("sourceState", source)
                       .withHeader("targetState", target);
        // Return false to veto this transition
        return true;
    }

    @Override
    public void postTransition(String source, String target,
                                Event<String, ?> event, KycContext ctx) {
        System.out.println("Completed: " + source + " -> " + target);
    }
});
```

### Custom Context

The context type `C` is entirely under your control. Use a Java `record`, a POJO, or a mutable holder — whatever fits your domain:

```java
// Mutable context for richer workflows
class KycContext {
    String name;
    String placeOfBirth;
    String decision;
    Message<?> currentMessage;
    // getters, setters, or direct field access
}
```

## Build-Time Validation

The builder checks 7 rules at `build()` time, collecting all violations before throwing:

| Rule | Error Message |
|------|---------------|
| Initial state set | `InitialStateNotSet` |
| Initial state exists | `InitialStateNotFound` |
| At least one state | `NoStatesDefined` |
| Transition source exists | `SourceStateNotFound` |
| Transition target exists | `TargetStateNotFound` |
| No duplicate (source, event, target) | `DuplicateTransition` |
| Final state exists | `FinalStateNotFound` |
| Guarded branching is deterministic | `AmbiguousTransition` |
| Resume state is defined | `ResumeStateNotFound` |

## Module Contents

```
io.github.khezyapp.fsm.core/
├── api/
│   ├── Action.java                     — @FunctionalInterface for side effects
│   ├── Guard.java                      — @FunctionalInterface for conditions
│   ├── StateMachine.java               — main contract (fire, query, listen, intercept)
│   ├── StateMachineListener.java       — read-only observer (default no-ops)
│   ├── StateMachineInterceptor.java    — pre/post hooks (can veto)
│   └── TransitionExecutionException.java  — runtime transition failure
├── builder/
│   ├── StateMachineBuilder.java        — fluent builder with validation
│   ├── TransitionConfigurer.java       — fluent sub-builder for transitions
│   └── StateMachineBuilderException.java  — build-time validation errors
├── impl/
│   ├── DefaultStateMachine.java        — synchronised 11-step runtime engine
│   └── TransitionIndex.java            — O(1) lookup map (source -> event -> transition)
└── model/
    ├── State.java                      — immutable state record (+ onEntry/onExit lists)
    ├── Event.java                      — event record (type + typed Message payload)
    ├── Transition.java                 — transition record (source, target, event, guard, actions)
    └── Message.java                    — message envelope (typed body + mutable headers)
```

## Requirements

- **Java 17+** (uses records, sealed interfaces aren't required)
- **Zero** external dependencies

## License

Part of the [KHEZY](https://github.com/khezyapp) ecosystem.
