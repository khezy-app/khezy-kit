# Index Concept — Technical Design Document

## 1. What Is an Index?

An **index** is a data structure that maps **keys** to **locations or values**, enabling fast lookup without scanning every element.

**Book analogy:** A textbook index maps "topic → page number." Instead of reading every page to find "binary search," you look it up in the index, get page 142, and go directly there. The index is smaller than the book, so searching the index is faster than searching the book.

**Computer science definition:** Given a collection of N items, an index reduces the cost of finding a specific item from **O(N)** (scan everything) to **O(1)** (hash lookup) or **O(log N)** (tree traversal).

### Without Index vs With Index

```
Without index:  Search 1,000,000 items → up to 1,000,000 comparisons
With hash index: Search 1,000,000 items → ~1 comparison (average)
With tree index:  Search 1,000,000 items → ~20 comparisons
```

---

## 2. The Problem Indexes Solve

Consider a simple FSM with 100 transitions. When an event arrives, you need to find the transition matching (currentState, eventType).

### Linear Scan (No Index)

```java
// O(N) — must check every transition
for (Transition transition : transitions) {
    if (currentState.equals(transition.source())
        && event.equals(transition.eventType())) {
        return transition;  // found it, but we scanned N-1 others first
    }
}
```

**Cost:** For N = 100 transitions, worst case = 100 comparisons per event.
For N = 10,000 transitions, worst case = 10,000 comparisons per event.
This is what Easy States does.

### Hash Index (With Index)

```java
// O(1) — direct lookup, no scanning
Map<String, Map<String, Transition>> index;  // state → event → transition
return index.get(currentState).get(eventType);
```

**Cost:** For any N, average case = ~1 comparison (hash + equality check).
This is what our FSM core library does.

### Timing Comparison

| Transitions (N) | Linear Scan (worst) | Hash Index (average) | Speedup |
|-----------------|--------------------|--------------------|---------|
| 10 | 10 comparisons | 1 comparison | 10x |
| 100 | 100 comparisons | 1 comparison | 100x |
| 1,000 | 1,000 comparisons | 1 comparison | 1,000x |
| 10,000 | 10,000 comparisons | 1 comparison | 10,000x |

---

## 3. Index Type 1: Hash Index

**Core idea:** Use a `HashMap` to map keys directly to values. Average O(1) lookup, O(1) insert, O(1) delete.

**Tradeoff:** Unordered — you cannot iterate keys in sorted order, and range queries (find all keys between A and Z) are not possible.

### Java Implementation: Word Frequency Counter

```java
import java.util.HashMap;
import java.util.Map;

public class WordFrequencyIndex {

    // The index: word → count
    private final Map<String, Integer> index = new HashMap<>();

    // Build the index from a list of words
    public void build(String[] words) {
        for (String word : words) {
            index.merge(word.toLowerCase(), 1, Integer::sum);
        }
    }

    // O(1) lookup — no scanning
    public int frequency(String word) {
        return index.getOrDefault(word.toLowerCase(), 0);
    }

    public static void main(String[] args) {
        var docs = new WordFrequencyIndex();
        docs.build(new String[]{
            "the", "cat", "sat", "on", "the", "mat", "the", "cat"
        });

        System.out.println(docs.frequency("the"));   // 3
        System.out.println(docs.frequency("cat"));   // 2
        System.out.println(docs.frequency("dog"));   // 0
    }
}
```

**How it works:**
1. Build phase: iterate all words, increment count in HashMap — O(N)
2. Query phase: `get(word)` is O(1) average — direct hash bucket access

**Without the index:** To count "the" you'd scan the entire array every time.

---

## 4. Index Type 2: Tree Index

**Core idea:** Use a `TreeMap` (red-black tree) to map keys to values. O(log N) lookup, but keys are **sorted**.

**Tradeoff:** Slightly slower than hash (log N vs 1), but supports range queries and ordered iteration.

### Java Implementation: Event Log with Range Queries

```java
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

public class EventLogIndex {

    // The index: timestamp → event description
    // TreeMap keeps keys sorted by time
    private final TreeMap<LocalDateTime, String> index = new TreeMap<>();

    public void add(LocalDateTime timestamp, String event) {
        index.put(timestamp, event);
    }

    // O(log N) lookup — tree traversal
    public String get(LocalDateTime timestamp) {
        return index.get(timestamp);
    }

    // Range query: all events between start and end — O(log N + K) where K = results
    public Map<LocalDateTime, String> range(LocalDateTime start, LocalDateTime end) {
        return index.subMap(start, true, end, true);
    }

    // Find the most recent event before a given time — O(log N)
    public Map.Entry<LocalDateTime, String> lastEventBefore(LocalDateTime time) {
        return index.floorEntry(time);
    }

    public static void main(String[] args) {
        var log = new EventLogIndex();
        log.add(LocalDateTime.of(2025, 1, 15, 10, 0), "KYC started");
        log.add(LocalDateTime.of(2025, 1, 15, 10, 30), "Documents uploaded");
        log.add(LocalDateTime.of(2025, 1, 15, 11, 0), "Under review");
        log.add(LocalDateTime.of(2025, 1, 15, 14, 0), "Approved");

        // Range query: what happened between 10:15 and 11:30?
        var events = log.range(
            LocalDateTime.of(2025, 1, 15, 10, 15),
            LocalDateTime.of(2025, 1, 15, 11, 30)
        );
        // Result: {10:30=Documents uploaded, 11:00=Under review}

        // Floor query: what was the last event before 11:15?
        var last = log.lastEventBefore(
            LocalDateTime.of(2025, 1, 15, 11, 15)
        );
        // Result: 11:00=Under review
    }
}
```

**When to use tree index:**
- You need sorted iteration (events in chronological order)
- You need range queries (all events in a time window)
- You need predecessor/successor queries (last event before X)

---

## 5. Index Type 3: Composite Index (Multi-Level)

**Core idea:** A **map of maps** — the first level maps one key, the second level maps another key. This creates a multi-dimensional index for composite lookups.

**Structure:** `Map<Key1, Map<Key2, Value>>`

**Cost:** O(1) per level — total O(1) for a two-level index.

### Java Implementation: FSM Transition Index

This is the index used in our FSM core library. The natural key for a transition is **(source state, event type)** — a composite key.

```java
import java.util.HashMap;
import java.util.Map;

public class TransitionIndex<S, E, T> {

    // Level 1: source state → (Level 2: event type → transition)
    private final Map<S, Map<E, T>> index = new HashMap<>();

    // Build: insert a transition into the two-level index
    public void put(S source, E eventType, T transition) {
        index.computeIfAbsent(source, k -> new HashMap<>())
             .put(eventType, transition);
    }

    // Lookup: O(1) — two hash lookups
    public T find(S currentState, E eventType) {
        var byEvent = index.get(currentState);
        if (byEvent == null) {
            return null;  // no transitions from this state
        }
        return byEvent.get(eventType);
    }

    public static void main(String[] args) {
        var transitions = new TransitionIndex<String, String, String>();

        // Build the index
        transitions.put("DRAFT",     "submit",  "DRAFT → INFO_COLLECTED");
        transitions.put("INFO_COLLECTED", "validate", "INFO_COLLECTED → VALIDATING");
        transitions.put("VALIDATING", "pass",   "VALIDATING → APPROVED");
        transitions.put("VALIDATING", "fail",   "VALIDATING → REJECTED");

        // O(1) lookup
        System.out.println(transitions.find("DRAFT", "submit"));
        // → "DRAFT → INFO_COLLECTED"

        System.out.println(transitions.find("VALIDATING", "pass"));
        // → "VALIDATING → APPROVED"

        System.out.println(transitions.find("DRAFT", "unknown_event"));
        // → null (no such transition)
    }
}
```

**Visualizing the index structure:**

```
index = {
    "DRAFT": {
        "submit": "DRAFT → INFO_COLLECTED"
    },
    "INFO_COLLECTED": {
        "validate": "INFO_COLLECTED → VALIDATING"
    },
    "VALIDATING": {
        "pass": "VALIDATING → APPROVED",
        "fail": "VALIDATING → REJECTED"
    }
}
```

**Why not just use a flat Map<(S,E), T>?**
You could use `Map<String, Transition>` with a composite key like `"DRAFT:submit"`. But the two-level approach lets you:
1. Query all transitions from a state: `index.get("VALIDATING")` → all outgoing transitions
2. Check if a state has any transitions: `index.containsKey("APPROVED")`
3. Natural structure matches the problem domain

### Real-World Example from Spring State Machine

Spring SM uses this exact pattern in `AbstractStateMachineFactory`:

```java
// From AbstractStateMachineFactory.java
Map<S, State<S, E>> stateMap = new HashMap<>();
// Maps state ID → live State object
// Used during wiring: stateMap.get("DRAFT") → State object

Map<Trigger<S, E>, Transition<S, E>> triggerToTransitionMap = new HashMap<>();
// Maps trigger → transition
// Used at runtime: triggerToTransitionMap.get(trigger) → Transition object
```

The key insight from Spring SM: they index by **trigger** (what causes the transition) rather than by (state, event). This gives O(1) lookup when a trigger fires, with a secondary scan needed to match events to triggers.

---

## 6. Index Type 4: Inverted Index

**Core idea:** In a forward index, you map **document → words**. In an **inverted index**, you flip it: **word → set of documents**. This is the foundation of full-text search (Google, Lucene, Elasticsearch).

**Structure:** `Map<String, Set<String>>` — word → document IDs containing that word.

**Cost:** Build O(N × M) where N = documents, M = words per document. Query O(1) per word.

### Java Implementation: Simple Text Search

```java
import java.util.*;

public class InvertedIndex {

    // The index: lowercase word → set of document IDs
    private final Map<String, Set<String>> index = new HashMap<>();

    // Build: index a document
    public void addDocument(String docId, String content) {
        String[] words = content.toLowerCase().split("\\s+");
        for (String word : words) {
            // Remove punctuation for clean indexing
            word = word.replaceAll("[^a-zA-Z]", "");
            if (!word.isEmpty()) {
                index.computeIfAbsent(word, k -> new HashSet<>()).add(docId);
            }
        }
    }

    // Query: find all documents containing a word — O(1)
    public Set<String> search(String word) {
        return index.getOrDefault(word.toLowerCase(), Set.of());
    }

    // AND query: documents containing ALL words
    public Set<String> searchAnd(String word1, String word2) {
        Set<String> results1 = search(word1);
        Set<String> results2 = search(word2);
        Set<String> intersection = new HashSet<>(results1);
        intersection.retainAll(results2);
        return intersection;
    }

    // OR query: documents containing ANY word
    public Set<String> searchOr(String word1, String word2) {
        Set<String> results1 = search(word1);
        Set<String> results2 = search(word2);
        Set<String> union = new HashSet<>(results1);
        union.addAll(results2);
        return union;
    }

    public static void main(String[] args) {
        var searchEngine = new InvertedIndex();

        // Build the index
        searchEngine.addDocument("doc1", "Spring State Machine is a Java framework");
        searchEngine.addDocument("doc2", "Lucene is a search engine library");
        searchEngine.addDocument("doc3", "Spring Lucene integration for search");

        // Single word search — O(1)
        System.out.println(searchEngine.search("spring"));
        // → {doc1, doc3}

        System.out.println(searchEngine.search("lucene"));
        // → {doc2, doc3}

        // AND search — documents with both words
        System.out.println(searchEngine.searchAnd("spring", "lucene"));
        // → {doc3}

        // OR search — documents with either word
        System.out.println(searchEngine.searchOr("java", "search"));
        // → {doc1, doc2, doc3}
    }
}
```

**Visualizing the inverted index:**

```
index = {
    "spring":  {doc1, doc3},
    "state":   {doc1},
    "machine": {doc1},
    "java":    {doc1},
    "lucene":  {doc2, doc3},
    "search":  {doc2, doc3},
    "engine":  {doc2},
    "library": {doc2},
    "for":     {doc3}
}
```

**Without the index:** To find documents containing "spring," you'd scan all 3 documents, split each into words, and check — O(N × M).
**With the index:** `index.get("spring")` → O(1).

This is exactly how Apache Lucene works (the search library behind Elasticsearch). Lucene's inverted index is far more complex (posting lists, term frequency, TF-IDF scoring), but the core concept is this simple word → documents map.

---

## 7. Build-Time vs Runtime Indexes

### Build-Time Index (Immutable After Construction)

The index is built once when the data structure is created, then only queried.

```
Build phase:  O(N) — insert all items into index
Query phase:  O(1) per lookup
Update:       Not supported (rebuild required)
```

**Example: FSM Transition Index**

```java
// Build once in StateMachineBuilder.build()
for (Transition<S,E,C> t : transitions) {
    index.computeIfAbsent(t.source(), k -> new HashMap<>())
         .put(t.eventType(), t);
}

// Query many times in DefaultStateMachine.fire()
// index is never modified after build
Transition<S,E,C> transition = index.get(currentState).get(eventType);
```

**When to use:** Data is static after creation (FSM transitions, configuration lookup tables, word dictionaries).

### Runtime Index (Mutable)

The index is updated as data changes — add, remove, or modify entries dynamically.

```
Build phase:  O(1) per insert
Query phase:  O(1) per lookup
Update:       O(1) per insert/delete
```

**Example: Live Event Log**

```java
// Index is updated as new events arrive
public void onEvent(String type, String payload) {
    Event event = new Event(type, payload, Instant.now());
    events.put(event.id(), event);
    typeIndex.computeIfAbsent(type, k -> new HashSet<>()).add(event.id());
}

// Query at any time
public List<Event> eventsByType(String type) {
    return typeIndex.getOrDefault(type, Set.of()).stream()
        .map(events::get)
        .toList();
}
```

**When to use:** Data changes over time (search indexes, user session caches, live dashboards).

---

## 8. When to Use Which

| Index Type | Best For | Lookup Cost | Sorted? | Range Query? |
|-----------|----------|-------------|---------|-------------|
| **Hash** (`HashMap`) | Exact key lookup, highest performance | O(1) avg | No | No |
| **Tree** (`TreeMap`) | Sorted iteration, range queries | O(log N) | Yes | Yes |
| **Composite** (`Map<K1, Map<K2, V>>`) | Multi-dimensional lookup | O(1) per level | No | No |
| **Inverted** (`Map<K, Set<V>>`) | Reverse mapping, text search | O(1) per word | No | No |

### Decision Flowchart

```
Need to find item by key?
├── Yes, exact key → HashMap (Hash Index)
├── Yes, and need sorted order → TreeMap (Tree Index)
├── Yes, and key is composite (two dimensions) → Map of Maps (Composite Index)
└── Yes, and need reverse lookup (value → keys) → Inverted Index
```

---

## 9. Tradeoffs

### Memory Overhead

Indexes consume extra memory. A `HashMap` stores entries in an array with load-factor resizing. Rough estimate:

| Structure | Memory per entry | 10,000 entries |
|-----------|-----------------|----------------|
| `HashMap<String, Integer>` | ~48 bytes | ~480 KB |
| `TreeMap<String, Integer>` | ~64 bytes (tree nodes) | ~640 KB |
| No index (raw array) | 0 extra bytes | 0 KB |

The tradeoff: spend memory to save time.

### Build Time

Building an index takes time. For a hash index, each insert is O(1) amortized, so building N entries is O(N). But the constant factor matters:

| Build approach | 1M entries |
|---------------|-----------|
| `HashMap.put()` × 1M | ~50ms |
| Linear scan (no build) | 0ms (no index to build) |

You pay the build cost once, then benefit from fast lookups forever.

### Update Cost

If the data changes after the index is built, you must update the index too. Forgetting to update is a common bug.

```java
// BUG: updated data but forgot to update index
transitions.add(newTransition);  // data updated
// index is now stale — fire() won't find the new transition!

// CORRECT: update both
transitions.add(newTransition);
index.computeIfAbsent(newTransition.source(), k -> new HashMap<>())
     .put(newTransition.eventType(), newTransition);
```

This is why our FSM design makes machines **immutable after build** — no index staleness possible.

---

## 10. Reference Implementations

Index patterns found in Spring State Machine and Easy States:

| Pattern | Location | Type | Key | Complexity | Purpose |
|---------|----------|------|-----|------------|---------|
| `stateMap` | Spring SM `AbstractStateMachineFactory` | `HashMap<S, State>` | State ID | O(1) | Resolve state IDs to live objects |
| `triggerToTransitionMap` | Spring SM `AbstractStateMachine` | `HashMap<Trigger, Transition>` | Trigger ref | O(1) | Timer-trigger shortcut |
| `machines` | Spring SM `DefaultStateMachineService` | `HashMap<String, StateMachine>` | Machine ID | O(1) | Instance cache |
| `variables` | Spring SM `DefaultExtendedState` | `ConcurrentHashMap` | Any key | O(1) | Runtime state storage |
| `transitions` | Easy States `FiniteStateMachineImpl` | `HashSet<Transition>` | *Not used for lookup* | O(N) scan | Linear scan on every `fire()` |
| `states` | Easy States `FiniteStateMachineImpl` | `HashSet<State>` | State name | O(1) | Membership check |

### Key Insight from the Comparison

Easy States stores transitions in a `HashSet` but **never uses it for lookup**. The `fire()` method iterates all transitions with a linear scan:

```java
// Easy States: O(N) linear scan
for (Transition transition : transitions) {
    if (currentState.equals(transition.getSourceState()) &&
        transition.getEventType().equals(event.getClass())) {
        // found
    }
}
```

Spring SM indexes transitions by trigger, giving O(1) for timer-triggered transitions. Our design indexes by (source, event), giving O(1) for all event-driven transitions — the most common case.

The right index depends on your access pattern. There is no universal "best index" — only the right index for your query.
