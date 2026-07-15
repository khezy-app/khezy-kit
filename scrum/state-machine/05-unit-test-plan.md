# Unit Test Plan — State Machine Core

## Test Structure

```
io.github.khezyapp.fsm.core/
├── model/
│   ├── MessageTest.java        — header mutability, withHeader/withHeaders
│   └── EventTest.java          — creation, typed vs untyped message
├── builder/
│   └── StateMachineBuilderTest.java  — 7 validation rules + successful build
├── impl/
│   ├── DefaultStateMachineTest.java  — fire() logic, guards, entry/exit, errors
│   ├── StateMachineInterceptorTest.java  — veto, post-notification
│   └── StateMachineListenerTest.java     — lifecycle callbacks
└── kyc/
    └── KycWorkflowTest.java       — end-to-end KYC scenario
```

## KYC Workflow (reference)

```
DRAFT ──submit──► INFO_COLLECTED ──validate──► VALIDATING ──pass──► APPROVED
                       │                            │
                       │                            └──fail──► REJECTED
                       │
                       └──revision──► DRAFT (resubmit)
```

States: DRAFT (initial), INFO_COLLECTED, VALIDATING, APPROVED (final), REJECTED (final)
Events: submit, revision, validate, pass, fail

Context:
```java
record KycContext(String name, String placeOfBirth, String dateOfBirth, String decision) {}
```

Guard: `submit` transition requires `name != null && !name.isBlank()`

## Test Cases

### MessageTest
| Test | Assertion |
|------|-----------|
| Create with body and headers | headers is mutable HashMap |
| Create with null headers | defaults to empty HashMap |
| `of(body)` factory | body set, headers empty |
| `withHeader` mutates in place | header exists after call |
| `withHeaders` merges multiple | all headers present |
| `withHeader` returns this | same reference for chaining |
| Mutate via `headers().put()` | direct mutation works |

### EventTest
| Test | Assertion |
|------|-----------|
| `of(type)` creates event with null message body | type set, message body null |
| `of(type, message)` with typed message | type + message match |
| Event type is readable | event.type() returns input |
| Construct with null message | defaults to message with null body |

### StateMachineBuilderTest (all 7 validation rules)
| # | Test | Expects |
|---|------|---------|
| 1 | No initial state set | `InitialStateNotSet` |
| 2 | Initial state not in states set | `InitialStateNotFound` |
| 3 | No states defined | `NoStatesDefined` |
| 4 | Transition source not found | `SourceStateNotFound` |
| 5 | Transition target not found | `TargetStateNotFound` |
| 6 | Duplicate transition (same source + event) | `DuplicateTransition` |
| 7 | Final state not in states set | `FinalStateNotFound` |
| 8 | All violations collected at once | exception contains all messages |
| 9 | Valid machine builds successfully | no exception, `getCurrentState()` returns initial |

### DefaultStateMachineTest
| Test | Assertion |
|------|-----------|
| Simple transition fires | currentState updates to target |
| Null event is ignored | state unchanged |
| Unknown event notifies onError | state unchanged; onError fires with TransitionNotFound |
| No matching transition notifies onError | state unchanged; onError fires |
| Final state ignores all events | state stays final, no listener callbacks |
| Guard allows transition | transition proceeds |
| Guard blocks transition (returns false) | state unchanged |
| Guard that throws is treated as denial | state unchanged |
| Exit actions execute before transition | observable side effect |
| Entry actions execute after transition | observable side effect |
| Transition actions execute | observable side effect |
| Action failure propagates exception | `TransitionExecutionException` thrown |
| On failure, state remains at old state | partial transition not rolled forward |
| `isFinal()` returns false for non-final | `false` |
| `isFinal()` returns true for final state | `true` |
| `getStates()` returns all registered states | matches input |
| `getTransitions()` returns all registered transitions | matches input |
| `getInitialState()` returns initial | matches input |

### StateMachineInterceptorTest
| Test | Assertion |
|------|-----------|
| `preTransition` returning true allows transition | state changes |
| `preTransition` returning false vetoes | state unchanged |
| `postTransition` fires after successful transition | hook invoked with correct params |
| Multiple interceptors execute in order | both pre, then both post |
| Veto by first interceptor skips remaining | second pre not called |

### StateMachineListenerTest
| Test | Assertion |
|------|-----------|
| `onTransitionStart` fires before transition | captured state transition |
| `onTransitionComplete` fires after transition | captured after state change |
| `onStateChanged` fires on state update | old → new captured |
| `onError` fires on interceptor veto | exception captured |
| `onError` fires on action failure | exception captured |
| `onError` fires on unknown event | exception captured; state unchanged |
| Add/remove listener | removed listener does not fire |

### KycWorkflowTest
| Test | Assertion |
|------|-----------|
| Happy path: submit → validate → pass → APPROVED | final state is APPROVED, isFinal() true |
| Fail path: submit → validate → fail → REJECTED | final state is REJECTED, isFinal() true |
| Revision: submit → revision → DRAFT | back to DRAFT |
| Guard: submit with blank name blocks transition | stays in DRAFT |
| Full lifecycle with entry/exit actions | actions fire in correct order |
