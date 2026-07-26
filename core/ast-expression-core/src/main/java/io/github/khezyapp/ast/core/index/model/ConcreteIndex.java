package io.github.khezyapp.ast.core.index.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a concrete (physical) database index definition.
 * <p>
 * Contains the table name, indexed columns, included columns, index type,
 * and operator class (for GIN indexes). Factory methods {@link #bTree}
 * and {@link #gin} provide convenient construction.
 * </p>
 *
 * @param tableName     the table name
 * @param indexed       the indexed columns in order
 * @param included      the included (non-key) columns
 * @param type          the index type
 * @param operatorClass the operator class (for GIN indexes)
 */
public record ConcreteIndex(
        String tableName,
        List<String> indexed,
        List<String> included,
        IndexType type,
        String operatorClass
) {
    public ConcreteIndex {
        Objects.requireNonNull(tableName);
        indexed = List.copyOf(indexed);
        included = List.copyOf(included);
        Objects.requireNonNull(type);
        Objects.requireNonNull(operatorClass);
    }

    /**
     * Creates a B-tree index definition.
     *
     * @param tableName the table name
     * @param indexed   the indexed columns
     * @param included  the included columns
     * @param type      the index type (must not be GIN)
     * @return a new B-tree index
     * @throws IllegalArgumentException if type is GIN
     */
    public static ConcreteIndex bTree(final String tableName,
                                      final List<String> indexed,
                                      final List<String> included,
                                      final IndexType type) {
        if (type == IndexType.GIN) {
            throw new IllegalArgumentException("Use gin() factory for GIN indexes");
        }
        return new ConcreteIndex(tableName, indexed, included, type, "");
    }

    /**
     * Creates a GIN index definition.
     *
     * @param tableName     the table name
     * @param column        the indexed column
     * @param operatorClass the GIN operator class
     * @return a new GIN index
     */
    public static ConcreteIndex gin(final String tableName,
                                    final String column,
                                    final String operatorClass) {
        return new ConcreteIndex(
                tableName,
                List.of(column),
                List.of(),
                IndexType.GIN,
                operatorClass);
    }

    /**
     * Returns whether this is a GIN index.
     *
     * @return {@code true} if GIN
     */
    public boolean isGin() {
        return type == IndexType.GIN;
    }

    public boolean covers(final IndexFamily family) {
        if (isGin()) {
            return false;
        }
        if (!tableName.equals(family.tableName())) {
            return false;
        }
        if (indexed.size() < family.fixed().size()) {
            return false;
        }

        for (var i = 0; i < family.fixed().size(); i++) {
            if (!indexed.get(i).equals(family.fixed().get(i))) {
                return false;
            }
        }

        final var start = family.fixed().size();
        final var flexCount = family.flex().size();
        if (start + flexCount > indexed.size()) {
            return false;
        }

        final var flexSlice = indexed.subList(start, start + flexCount);
        if (!Set.copyOf(flexSlice).equals(family.flex())) {
            return false;
        }

        if (!family.last().isEmpty()) {
            if (family.indexedColumnCount() > indexed.size()) {
                return false;
            }
            if (!indexed.get(family.indexedColumnCount() - 1).equals(family.last())) {
                return false;
            }
        }

        return Set.copyOf(included).containsAll(family.included());
    }

    public boolean coversGin(final ConcreteIndex proposed) {
        if (!isGin() || !proposed.isGin()) {
            return false;
        }
        if (!tableName.equals(proposed.tableName)) {
            return false;
        }
        if (indexed.size() != 1 || !indexed.get(0).equals(proposed.indexed.get(0))) {
            return false;
        }
        return "jsonb_ops".equals(operatorClass) || operatorClass.equals(proposed.operatorClass);
    }
}
