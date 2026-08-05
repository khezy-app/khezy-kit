package io.github.khezyapp.ast.core.index.model;

import io.github.khezyapp.ast.core.CoreUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Describes an index family for a table — a set of columns that should be
 * covered by a single composite index.
 * <p>
 * An {@code IndexFamily} consists of fixed prefix columns, flexible columns,
 * an optional last column (for range conditions), included (non-key) columns,
 * and functional columns requiring expression indexes.
 * </p>
 *
 * @param tableName  the table name
 * @param fixed      the fixed prefix columns (must match exactly)
 * @param flex       the flexible (optional) columns
 * @param last       the last column (for range conditions)
 * @param included   the included (non-key) columns
 * @param functional the functional (transform) columns
 */
public record IndexFamily(String tableName,
                          List<String> fixed,
                          Set<String> flex,
                          String last,
                          Set<String> included,
                          Set<String> functional) {

    public IndexFamily(
            final String tableName,
            final List<String> fixed,
            final Set<String> flex,
            final String last,
            final Set<String> included,
            final Set<String> functional) {
        this.tableName = Objects.requireNonNull(tableName);
        this.fixed = List.copyOf(fixed);
        this.flex = Collections.unmodifiableSet(new LinkedHashSet<>(flex));
        this.last = Objects.requireNonNullElse(last, "");
        this.included = Collections.unmodifiableSet(new LinkedHashSet<>(included));
        this.functional = Collections.unmodifiableSet(new LinkedHashSet<>(functional));
    }

    public int indexedColumnCount() {
        return fixed.size() + flex.size() + (CoreUtils.isEmpty(last) ? 0 : 1);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String tableName;
        private final List<String> fixed = new ArrayList<>();
        private final Set<String> flex = new LinkedHashSet<>();
        private String last = "";
        private final Set<String> included = new LinkedHashSet<>();
        private final Set<String> functional = new LinkedHashSet<>();

        private Builder() {
        }

        public Builder tableName(final String v) {
            this.tableName = v;
            return this;
        }

        public Builder addFixed(final String column) {
            fixed.add(column);
            return this;
        }

        public Builder addFlex(final String column) {
            flex.add(column);
            return this;
        }

        public Builder last(final String v) {
            this.last = v;
            return this;
        }

        public Builder addIncluded(final String column) {
            included.add(column);
            return this;
        }

        public Builder addFunctional(final String column) {
            functional.add(column);
            return this;
        }

        public Builder fixedColumns(final List<String> columns) {
            fixed.addAll(columns);
            return this;
        }

        public Builder flexColumns(final Set<String> columns) {
            flex.addAll(columns);
            return this;
        }

        public Builder includedColumns(final Set<String> columns) {
            included.addAll(columns);
            return this;
        }

        public Builder functionalColumns(final Set<String> columns) {
            functional.addAll(columns);
            return this;
        }

        public IndexFamily build() {
            Objects.requireNonNull(tableName, "tableName is required");
            return new IndexFamily(tableName, fixed, flex, last, included, functional);
        }
    }

    public IndexFamily withFixedPrefix(final List<String> prefix) {
        final var newFixed = new ArrayList<>(prefix);
        newFixed.addAll(fixed);
        return new IndexFamily(tableName, newFixed, flex, last, included, functional);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndexFamily that)) {
            return false;
        }
        return tableName.equals(that.tableName)
                && fixed.equals(that.fixed)
                && flex.equals(that.flex)
                && last.equals(that.last)
                && included.equals(that.included)
                && functional.equals(that.functional);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(tableName, fixed, flex, last, included, functional);
    }

    @Override
    public String toString() {
        return "IndexFamily{table=" + tableName
                + ", fixed=" + fixed
                + ", flex=" + flex
                + ", last='" + last + '\''
                + ", included=" + included
                + ", functional=" + functional + "}";
    }
}
