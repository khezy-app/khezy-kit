package io.github.khezyapp.dpriv.stream;

import java.util.*;

/**
 * The sink every streamed check writes to (design §10.3). Dedupes by {@code (entityType, token)}
 * value while preserving first-seen token order per entity type.
 */
public final class MatchAccumulator {

    private final Map<String, LinkedHashSet<String>> tokensByType = new LinkedHashMap<>();

    /**
     * Inserts a {@code (entityType, token)} match, deduplicating by value.
     *
     * @param entityType the entity type
     * @param token      the matched token
     */
    public void add(final String entityType,
                    final String token) {
        tokensByType.computeIfAbsent(entityType, key -> new LinkedHashSet<>()).add(token);
    }

    /**
     * Inserts all tokens for an entity type, deduplicating and preserving order.
     *
     * @param entityType the entity type
     * @param tokens     the tokens to insert
     */
    public void addAll(final String entityType,
                       final List<String> tokens) {
        final var set = tokensByType.computeIfAbsent(entityType, key -> new LinkedHashSet<>());
        set.addAll(tokens);
    }

    /**
     * The distinct entity types seen so far, in first-seen order.
     *
     * @return unmodifiable set of entity types
     */
    public Set<String> entityTypes() {
        return Collections.unmodifiableSet(tokensByType.keySet());
    }

    /**
     * The deduplicated, first-seen-ordered tokens for an entity type.
     *
     * @param entityType the entity type
     * @return unmodifiable list of tokens; empty if the entity type is absent
     */
    public List<String> tokens(final String entityType) {
        final var set = tokensByType.get(entityType);
        if (Objects.isNull(set)) {
            return List.of();
        }
        return List.copyOf(set);
    }

    /**
     * Snapshot of {@code entityType -> token list} for redaction.
     *
     * @return unmodifiable map of unmodifiable lists
     */
    public Map<String, List<String>> toMaskEntities() {
        final var result = new LinkedHashMap<String, List<String>>();
        for (final var entry : tokensByType.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Number of distinct entity types accumulated.
     *
     * @return the count
     */
    public int entityTypeCount() {
        return tokensByType.size();
    }
}
