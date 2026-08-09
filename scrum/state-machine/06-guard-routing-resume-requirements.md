# State Machine Core — Requirements: Guard-Driven Branching (CHOICE) + Resumable State

Package: `io.github.khezyapp.fsm.core`

Version: `1.0.0` → target `1.1.0` (additive, backward-compatible)

Status: DONE — implemented in `state-machine-core:1.1.0`

---

## 1. Background / Why

The `kyc-core` project builds a KYC/KYB workflow engine (`WorkflowEngine`, `WorkflowMachineFactory`)
on top of this FSM core (`StateMachine<String, String, WorkflowContext>`). Two real-world needs from
the KYC/KYB flows (design doc `kyc-core/plan/proposal-high-level-design.md` §4.2, §5.1, §5.2) cannot
be expressed by `state-machine-core:1.0.0`:

1. **Guard-driven branching (CHOICE).** A single event must be able to route to different target
   states depending on context, e.g. one `KYC_RESPONSE_RECEIVED` event decides between `approved`,
   `rejected`, `revision`. Today only one transition per `(source, event)` is allowed.
2. **Resumable state.** A persisted workflow instance (whose `currentState` was saved) must be able
   to rebuild the machine positioned at that saved state, so processing can continue. Today there is
   no public way to set the machine's current state to anything other than the initial state.

This document specifies the minimal, additive changes to the core to support both.

---

## 2. What already works (verified in `state-machine-core:1.0.0`)

- Typed generic machine `StateMachine<S, E, C>`; states/events/context; O(1) transition lookup.
- State lifecycle actions: `.state(id, onEntry, onExit)`; transition actions via
  `TransitionConfigurer.action(...)`.
- `Guard<C>` to allow/block a single transition; `null` guard = always allowed.
- Final-state silent-ignore of events; listeners (`StateMachineListener`); interceptors
  (`StateMachineInterceptor` — pre-veto / post-observe).
- `DefaultStateMachine`'s constructor already accepts an arbitrary `currentState` parameter.
- `fire(Event<E, ?>, C)` returns `State<S, C>`; `getTransitions()` returns the full transition set.
- Build-time validation collecting 7 rules into `StateMachineBuilderException`.

---

## 3. What's missing (the gaps)

| # | Gap | Today | Needed |
|---|-----|-------|--------|
| A | Multiple transitions per `(source, event)` | Rejected (`DuplicateTransition`) and stored one-per-key in `TransitionIndex` | Allow several candidates with different targets; guards pick which fires (definition order = priority) |
| B | Resumable current state | No public API to set current state; machine always starts at `initialState` | Build a machine whose current state is a given defined state |

---

## 4. Requirements (testable)

### Gap A — Guard-driven branching

- **R1 — Multiple candidates allowed.** The builder permits more than one transition for the same
  `(source, event)` provided they have **different targets**.
- **R2 — Identical triple still rejected.** A duplicate with the same `(source, event, target)` is
  still a build-time violation (`DuplicateTransition`). *This keeps the existing
  `StateMachineBuilderTest.testDuplicateTransition` passing unchanged.*
- **R3 — Guard required for ambiguity.** When two or more transitions share `(source, event)`, at
  least one of them must carry a non-null guard. Two (or more) shared-`(source,event)` transitions
  that are **all** guard-less are rejected as ambiguous (`AmbiguousTransition`, build-time).
- **R4 — Candidates are ordered.** `TransitionIndex` returns the candidate transitions for a
  `(source, event)` **in definition (insertion) order**.
- **R5 — First-true wins.** `fire()` evaluates the ordered candidates and selects the **first whose
  guard returns `true`**. A `null` guard always matches, so a trailing guard-less transition acts as
  the deterministic fallback / `else`.
- **R6 — No match is a no-op.** If none of the candidates' guards pass, the event is ignored: state
  is unchanged and listeners receive an `onError` (a `NoTransitionSelected`-style
  `TransitionExecutionException`), matching today's "no matching transition" behaviour.
- **R7 — Only the winner executes.** Only the selected transition's actions and the target state's
  entry/exit actions run; the non-selected candidates' actions never run.
- **R8 — Winner is observable.** `StateMachine` exposes
  `Optional<Transition<S, E, C>> getLastTransition()` returning the transition selected by the most
  recent `fire()`. `Optional.empty()` when the last `fire()` was a no-op (no match, or final-state
  ignore). Additive; `fire()`'s return type is unchanged.

### Gap B — Resumable state

- **R9 — Resume at build.** `StateMachineBuilder` gains `StateMachineBuilder<S, E, C> resume(S stateId)`.
  When called, the built machine's current state equals `stateId` instead of `initialState`.
- **R10 — Resume validates.** `resume(...)` with a `stateId` that is not a defined state fails at
  build time with a clear violation (`ResumeStateNotFound`).
- **R11 — Resumed machine behaves normally.** `isFinal()`, guards, actions, and subsequent `fire()`
  calls operate correctly relative to the resumed state.

### Non-goals (explicitly out of scope for the core)

- **CHOICE auto-advance.** Automatically resolving a CHOICE state immediately on entry (following its
  guarded target without an explicit event) is **not** implemented in the core. It is a
  consumer/engine concern. The core only supports guard-routing **at `fire()` time**.
- Scheduled/timeout events, async actions, YAML loading, Spring integration.

---

## 5. Proposed API changes (sketch)

### 5.1 `TransitionIndex` (impl)

```java
// was: find(S, E) -> Transition          (single, or null)
// now:
List<Transition<S, E, C>> findAll(S currentState, E eventType); // ordered, definition order
Transition<S, E, C> find(S currentState, E eventType);          // findAll(...).get(0) or null (kept)
```

- `findAll` returns an immutable, ordered list. Order must match registration order. The existing
  single `find` stays as a convenience returning the first candidate (or `null`), preserving
  backward compatibility.

### 5.2 `DefaultStateMachine.fire(...)` (impl)

```
fire(Event<E,?> event, C context):
  if isFinal()        -> record lastTransition = empty; return currentState
  if event == null    -> record lastTransition = empty; return currentState
  candidates = transitionIndex.findAll(currentState.id(), event.type())
  if candidates.isEmpty():
      lastTransition = empty
      notify onError(NoTransitionSelected)
      return currentState
  selected = null
  for candidate in candidates:              // definition order
      if candidate.guard() == null || candidate.guard().evaluate(context):
          selected = candidate; break
  if selected == null:                      // no guard passed
      lastTransition = empty
      notify onError(NoTransitionSelected)
      return currentState
  lastTransition = Optional.of(selected)
  ... continue existing steps (interceptor pre, exit, selected.actions(), state update, entry, post) ...
```

### 5.3 `StateMachine` (api) — additive

```java
Optional<Transition<S, E, C>> getLastTransition();
```

### 5.4 `StateMachineBuilder` (builder) — additive

```java
public StateMachineBuilder<S, E, C> resume(S stateId) { ... } // R9
```

- Validation (R2, R3, R10) collected alongside the existing rules:
  - `DuplicateTransition`: reject same `(source, event, target)`.
  - `AmbiguousTransition`: reject same `(source, event)` where **all** candidates are guard-less.
  - `ResumeStateNotFound`: `resume(stateId)` references an undefined state.

---

## 6. Example use cases (for the implementing agent)

### UC-1 — Decision routing on one event (the KYC `manual_review` case)

One event `KYC_RESPONSE_RECEIVED` routes to `approved` / `rejected` / `revision` based on the
`decision` in context. Guards are evaluated in definition order; the first match wins.

```java
record KycContext(String decision) {}

StateMachine<String, String, KycContext> machine = StateMachineBuilder
    .<String, String, KycContext>builder()
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
assert "approved".equals(machine.getCurrentState().id());
assert "to-approved".equals(machine.getLastTransition().orElseThrow().id());

machine.fire(Event.of("KYC_RESPONSE_RECEIVED"), new KycContext("revision"));
assert "revision".equals(machine.getCurrentState().id());
assert "to-revision".equals(machine.getLastTransition().orElseThrow().id());
```

### UC-2 — Guarded candidates + a no-guard fallback (`else`)

Same event, guarded rules first, a trailing guard-less transition as the deterministic default.
Only the winner's actions run.

```java
.initialState("route")
.state("route")
.state("known").state("unknown")

.transition("known-a", "route", "known", "check")
    .guard(ctx -> ctx.decision() != null)
.and()
.transition("fallback", "route", "unknown", "check")   // no guard = else
.and()
.build();

machine.fire(Event.of("check"), new KycContext(null));
assert "unknown".equals(machine.getCurrentState().id());
assert "fallback".equals(machine.getLastTransition().orElseThrow().id());
```

### UC-3 — Resume a persisted run (Gap B)

Reload a saved instance by rebuilding the machine positioned at its saved `currentState`.

```java
StateMachine<String, String, KycContext> machine = StateMachineBuilder
    .<String, String, KycContext>builder()
    .initialState("manual_review")
    .state("manual_review")
    .finalState("approved")
    .transition("approve", "manual_review", "approved", "approve")
    .and()
    .resume("manual_review")        // R9: current state = manual_review
    .build();

assert "manual_review".equals(machine.getCurrentState().id());
assert !machine.isFinal();

machine.fire(Event.of("approve"), new KycContext("approved"));
assert "approved".equals(machine.getCurrentState().id());
assert machine.isFinal();
```

### UC-4 — Already-works reassurance (single guarded transition, KYB wait-for-all)

A single transition whose guard waits for all vendor outputs must behave exactly as today.

```java
.transition("continue", "run_vendor_data", "manual_review", "vendors_done")
    .guard(ctx -> allVendorDataPresentOrError(ctx))
.and()
```

No change to this behaviour; it is the degenerate case of R5 (one candidate).

---

## 7. Unit test plan

Mirror the existing style (`@DisplayName`, `assertThrows`, JUnit Platform) in
`core/state-machine-core/src/test/java/io/github/khezyapp/fsm/core/`. Map each requirement to tests.

### `builder/StateMachineBuilderTest.java` (add)
| Req | Test | Assertion |
|-----|------|-----------|
| R1 | Allows same `(source, event)` with different targets | `build()` succeeds; `getTransitions()` contains all |
| R2 | Rejects same `(source, event, target)` | `StateMachineBuilderException` contains `DuplicateTransition` |
| R3 | Rejects all guard-less duplicates | `AmbiguousTransition` in violations |
| R3 | Allows guard-less + guarded shared `(source,event)` | builds successfully (fallback pattern) |
| R10 | `resume(undefinedState)` fails | `ResumeStateNotFound` in violations |
| R9 | `resume(definedState)` builds | machine current state == resumed state |

### `impl/TransitionIndexTest.java` (new or add)
| Req | Test | Assertion |
|-----|------|-----------|
| R4 | `findAll` preserves definition order | returned list order matches registration |
| R4 | `findAll` returns empty for unknown `(source,event)` | empty list |
| R4 | `find` returns first candidate | equals `findAll().get(0)` |

### `impl/DefaultStateMachineTest.java` (add)
| Req | Test | Assertion |
|-----|------|-----------|
| R5 | First true guard wins | target of first matching candidate reached |
| R5 | Guard-less fallback chosen when no guard passes | fallback target reached |
| R6 | No candidate guard passes | state unchanged; `onError` fires; `getLastTransition()` empty |
| R6 | No candidate exists for event | state unchanged; `getLastTransition()` empty |
| R7 | Only winner's actions run | action log contains only selected transition's action |
| R8 | `getLastTransition()` returns winner | correct transition id after fire |
| R8 | Final-state fire leaves `getLastTransition()` empty | `Optional.empty()` |

### `kyc/ChoiceRoutingWorkflowTest.java` (new — end-to-end)
| Req | Test | Assertion |
|-----|------|-----------|
| R5/R7/R8 | UC-1 decision routing | each `decision` reaches its target; winner id correct |
| R5 | UC-2 fallback | null decision reaches fallback target |
| R7 | Only one branch executes | losing branches' actions not recorded |

### `ResumeTest.java` (new — Gap B)
| Req | Test | Assertion |
|-----|------|-----------|
| R9 | UC-3 resume then continue | resumes at saved state, transitions onward |
| R11 | `isFinal()` reflects resumed state | correct before/after |
| R9/R11 | Resume to a final state | `isFinal()` true; events ignored |

---

## 8. Backward compatibility

- All additions are **additive**: new methods (`resume`, `getLastTransition`, `findAll`), no removed
  or changed signatures.
- `fire(Event<E, ?>, C)` return type and existing behaviour are unchanged for single-transition
  machines.
- Existing 7 validation tests remain green because `DuplicateTransition` is retained for the
  identical `(source, event, target)` triple (their `t1`/`t2` are identical).
- `kyc-core`'s `WorkflowEngine`/`WorkflowMachineFactory` currently use the single-transition API;
  they compile unchanged. Their guard-routing + resume support is a separate follow-up after this
  library change is published.

---

## 9. Acceptance / Definition of Done

- [x] `StateMachineBuilder.resume(...)`, `StateMachine.getLastTransition()`,
      `TransitionIndex.findAll(...)` implemented; `fire()` selects first-true candidate.
- [x] Validation: `DuplicateTransition` (identical triple), `AmbiguousTransition` (all guard-less
      shared `(source,event)`), `ResumeStateNotFound`.
- [x] All existing tests still pass; new tests from §7 pass.
- [x] `./gradlew build` (with checkstyle) passes in `khezy-kit`.
- [x] Version bumped to `1.1.0`; `README.md` updated with the branching + resume examples.
- [x] This requirements doc marked `DONE`.
