# Mutability Review — State Machine Core

## Context

After implementing the core library, we reviewed which components should be **mutable** (read/write at runtime) vs **immutable** (read-only definitions) to reflect real business use-cases.

The key scenario: *"Users need to update message headers from each state to add more context for business logic."*

---

## Component-by-Component Analysis

### Definitions — Always Immutable

These are registered at build time and **never change** during machine execution.

| Component | Fields | Mutable? | Reason |
|---|---|---|---|
| `State<S, C>` | `id`, `onEntry`, `onExit` | **No** | Machine topology is fixed after `build()`. Entry/exit action lists are compile-time configuration. |
| `Transition<S, E, C>` | `id`, `source`, `target`, `eventType`, `guard`, `actions` | **No** | Transition rules are fixed. Guard predicates and action lists are compile-time configuration. |

Both use `List.copyOf()` in compact constructors to guarantee immutability of stored action/guard references.

### Runtime Data Carriers — Selectively Mutable

| Component | Fields | Mutable? | Reason |
|---|---|---|---|
| `Event<E, T>` | `type`, `message` | **No** (immutable reference) | Event's `type` discriminator must never change. The `message` reference is stable (the Message object inside is mutable by reference). |
| `Message<T>` | `body`, `headers` | **Yes (headers)** | `body` is user-controlled (they decide mutability of their own type `T`). `headers` is **mutable** so users can enrich context across state transitions. |

**Change made to `Message`:**

```diff
- headers = headers != null ? Map.copyOf(headers) : Map.of();
+ headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
```

Why `HashMap` over `Map.copyOf()`:
- `Map.copyOf()` returns an **immutable** map — `put()` throws `UnsupportedOperationException`
- `HashMap` allows direct mutation via `message.headers().put("key", "value")`
- Spring Integration's `MessageHeaders` follows the same mutable-behind-the-scenes pattern

Fluent mutation helpers added:

```java
message.withHeader("traceId", "abc-123");          // add/replace single header
message.withHeaders(Map.of("userId", 42));          // merge multiple headers
```

Both mutate in-place and return `this` for chaining:

```java
message.withHeader("step", "VALIDATING")
       .withHeader("attempt", 3);
```

### Engine — Mutable by Nature

| Component | Fields | Mutable? | Reason |
|---|---|---|---|
| `DefaultStateMachine` | `currentState` | **Yes** | `fire()` transitions update `currentState` — the whole point of a state machine. |
| `TransitionIndex` | all | **No** | Built once at construction, never modified. O(1) read-only lookup. |
| `listeners` / `interceptors` | `CopyOnWriteArrayList` | **Yes** | Safe to add/remove during iteration. Thread-safe by design. |

---

## Where Mutation Happens in the fire() Pipeline

```
fire(event, context)
  ├─ Interceptor.preTransition   ← can read+write event.message.headers()
  ├─ Guard.evaluate(context)     ← read-only context
  ├─ Exit actions                 ← write context C (user's domain object)
  ├─ Transition actions           ← write context C
  ├─ Entry actions                ← write context C
  ├─ Interceptor.postTransition  ← can read+write event.message.headers()
  └─ Listener notifications      ← read-only (default methods are no-ops)
```

Key insight: **entry/exit/transition actions receive only `C context`**, not the event. If the user needs to enrich message headers from within an action, they should:

1. **Store the event/message in the context `C`** — include a `Message<?>` reference in the context object
2. **Mutate headers via that reference** — `context.getMessage().withHeader("key", "value")`
3. **Alternatively, use interceptors** — they receive both event and context for cross-cutting concerns like tracing

---

## Thread Safety Guidelines

| Operation | Safe? | Notes |
|---|---|---|
| Reading headers from listener/interceptor | **Yes** | Readers see latest writes (happens-before via `synchronized fire()`) |
| Writing headers in interceptor | **Yes** | Inside `synchronized fire()` — single thread |
| Writing headers from action via context reference | **User's responsibility** | If context is shared across threads, synchronize externally or use per-machine instances |
| Mutating headers outside `fire()` | **Not safe** | Only mutate during transition processing |

---

## Summary

- **Machine definitions** (State, Transition, TransitionIndex) → fully immutable
- **Runtime data carriers** (Event, Message) → type is fixed, headers are mutable
- **Engine state** (currentState, listeners, interceptors) → mutable with thread-safe guards
- **User context `C`** → fully user-controlled (recommend making relevant fields mutable as needed)

Only one source file changed: `Message.java` — `Map.copyOf()` → `HashMap` for headers, plus fluent `withHeader`/`withHeaders` helpers. Everything else remains as originally implemented.
