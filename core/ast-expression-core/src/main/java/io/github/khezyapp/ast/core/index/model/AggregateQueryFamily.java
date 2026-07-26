package io.github.khezyapp.ast.core.index.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Describes an aggregate query access pattern for index planning.
 * <p>
 * Captures the table, the target aggregation field, and categorized filter
 * conditions (equality, inequality, other, GIN, functional).
 * </p>
 *
 * @param tableName         the target table name
 * @param fieldName         the aggregated field name
 * @param eqConditions      columns with equality (=) conditions
 * @param ineqConditions    columns with inequality (&lt;, &gt;, &lt;=, &gt;=) conditions
 * @param otherConditions   columns with non-indexable filter conditions
 * @param ginConditions     columns with GIN-indexable conditions
 * @param functionalColumns columns with functional (transform) index requirements
 */
public record AggregateQueryFamily(String tableName,
                                   String fieldName,
                                   Set<String> eqConditions,
                                   Set<String> ineqConditions,
                                   Set<String> otherConditions,
                                   Set<String> ginConditions,
                                   Set<String> functionalColumns) {
    public AggregateQueryFamily(
            final String tableName,
            final String fieldName,
            final Set<String> eqConditions,
            final Set<String> ineqConditions,
            final Set<String> otherConditions,
            final Set<String> ginConditions,
            final Set<String> functionalColumns
    ) {
        this.tableName = Objects.requireNonNull(tableName);
        this.fieldName = Objects.requireNonNull(fieldName);
        this.eqConditions = Collections.unmodifiableSet(new LinkedHashSet<>(eqConditions));
        this.ineqConditions = Collections.unmodifiableSet(new LinkedHashSet<>(ineqConditions));
        this.otherConditions = Collections.unmodifiableSet(new LinkedHashSet<>(otherConditions));
        this.ginConditions = Collections.unmodifiableSet(new LinkedHashSet<>(ginConditions));
        this.functionalColumns = Collections.unmodifiableSet(new LinkedHashSet<>(functionalColumns));
    }

    /**
     * Returns whether this family has any indexable conditions (eq or ineq).
     *
     * @return {@code true} if B-tree indexable conditions exist
     */
    public boolean hasIndexableConditions() {
        return !eqConditions.isEmpty() || !ineqConditions.isEmpty();
    }

    /**
     * Returns whether this family has any GIN-indexable conditions.
     *
     * @return {@code true} if GIN conditions exist
     */
    public boolean hasGinConditions() {
        return !ginConditions.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String tableName;
        private String fieldName;
        private final Set<String> eqConditions = new LinkedHashSet<>();
        private final Set<String> ineqConditions = new LinkedHashSet<>();
        private final Set<String> otherConditions = new LinkedHashSet<>();
        private final Set<String> ginConditions = new LinkedHashSet<>();
        private final Set<String> functionalColumns = new LinkedHashSet<>();

        private Builder() {
        }

        public Builder tableName(final String v) {
            this.tableName = v;
            return this;
        }

        public Builder fieldName(final String v) {
            this.fieldName = v;
            return this;
        }

        public Builder addEq(final String column) {
            eqConditions.add(column);
            return this;
        }

        public Builder addIneq(final String column) {
            ineqConditions.add(column);
            return this;
        }

        public Builder addOther(final String column) {
            otherConditions.add(column);
            return this;
        }

        public Builder addGin(final String column) {
            ginConditions.add(column);
            return this;
        }

        public Builder addFunctional(final String column) {
            functionalColumns.add(column);
            return this;
        }

        public Builder addEqFunctional(final String column) {
            eqConditions.add(column);
            functionalColumns.add(column);
            return this;
        }

        public Builder addIneqFunctional(final String column) {
            ineqConditions.add(column);
            functionalColumns.add(column);
            return this;
        }

        public AggregateQueryFamily build() {
            Objects.requireNonNull(tableName, "tableName is required");
            Objects.requireNonNull(fieldName, "fieldName is required");
            return new AggregateQueryFamily(
                    tableName, fieldName, eqConditions, ineqConditions,
                    otherConditions, ginConditions, functionalColumns);
        }
    }

    private String canonicalKey() {
        return tableName + "|"
                + sortedJoined(eqConditions) + "|"
                + sortedJoined(ineqConditions) + "|"
                + sortedJoined(otherConditions) + "|"
                + sortedJoined(ginConditions) + "|"
                + sortedJoined(functionalColumns);
    }

    private static String sortedJoined(final Set<String> set) {
        return set.stream().sorted().collect(Collectors.joining(","));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AggregateQueryFamily that)) {
            return false;
        }
        return canonicalKey().equals(that.canonicalKey());
    }

    @Override
    public int hashCode() {
        return canonicalKey().hashCode();
    }

    @Override
    public String toString() {
        return "AggregateQueryFamily{"
                + "table=" + tableName
                + ", eq=" + eqConditions
                + ", ineq=" + ineqConditions
                + ", other=" + otherConditions
                + ", gin=" + ginConditions
                + ", functional=" + functionalColumns
                + "}";
    }
}
