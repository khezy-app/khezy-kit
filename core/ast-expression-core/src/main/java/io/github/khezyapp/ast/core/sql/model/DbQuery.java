package io.github.khezyapp.ast.core.sql.model;

import java.util.List;

/**
 * Complete database query model including SELECT, FROM, WHERE, GROUP BY,
 * aggregation, ORDER BY, JOIN, LIMIT, and OFFSET clauses.
 * <p>
 * Built using the fluent {@link Builder}.
 * </p>
 *
 * @param from         the FROM table
 * @param columns      the SELECT columns
 * @param filters      the WHERE filter conditions
 * @param groupBy      the GROUP BY columns
 * @param aggregations the aggregation functions
 * @param orderBy      the ORDER BY specifications
 * @param joins        the JOIN specifications
 * @param limit        the LIMIT count (may be {@code null})
 * @param offset       the OFFSET count (may be {@code null})
 */
public record DbQuery(
    DbTable from,
    List<DbColumn> columns,
    List<DbFilter> filters,
    List<DbColumn> groupBy,
    List<DbAggregation> aggregations,
    List<DbOrder> orderBy,
    List<DbJoin> joins,
    Long limit,
    Long offset
) {
    /**
     * Creates a new query builder.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for constructing a {@link DbQuery}.
     */
    public static final class Builder {
        private DbTable from;
        private List<DbColumn> columns;
        private List<DbFilter> filters;
        private List<DbColumn> groupBy;
        private List<DbAggregation> aggregations;
        private List<DbOrder> orderBy;
        private List<DbJoin> joins;
        private Long limit;
        private Long offset;

        private Builder() { }

        /**
         * Sets the FROM table.
         *
         * @param from the source table
         * @return this builder
         */
        public Builder from(final DbTable from) {
            this.from = from;
            return this;
        }

        /**
         * Sets the SELECT columns.
         *
         * @param columns the column list
         * @return this builder
         */
        public Builder columns(final List<DbColumn> columns) {
            this.columns = columns;
            return this;
        }

        /**
         * Sets the WHERE filter conditions.
         *
         * @param filters the filter list
         * @return this builder
         */
        public Builder filters(final List<DbFilter> filters) {
            this.filters = filters;
            return this;
        }

        /**
         * Sets the GROUP BY columns.
         *
         * @param groupBy the group-by column list
         * @return this builder
         */
        public Builder groupBy(final List<DbColumn> groupBy) {
            this.groupBy = groupBy;
            return this;
        }

        /**
         * Sets the aggregation functions.
         *
         * @param aggregations the aggregation list
         * @return this builder
         */
        public Builder aggregations(final List<DbAggregation> aggregations) {
            this.aggregations = aggregations;
            return this;
        }

        /**
         * Sets the ORDER BY specifications.
         *
         * @param orderBy the order-by list
         * @return this builder
         */
        public Builder orderBy(final List<DbOrder> orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        /**
         * Sets the JOIN specifications.
         *
         * @param joins the join list
         * @return this builder
         */
        public Builder joins(final List<DbJoin> joins) {
            this.joins = joins;
            return this;
        }

        /**
         * Sets the LIMIT count.
         *
         * @param limit the maximum number of rows
         * @return this builder
         */
        public Builder limit(final Long limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Sets the OFFSET count.
         *
         * @param offset the number of rows to skip
         * @return this builder
         */
        public Builder offset(final Long offset) {
            this.offset = offset;
            return this;
        }

        /**
         * Builds the {@link DbQuery}. Defaults empty collections for null fields.
         *
         * @return the completed query
         */
        public DbQuery build() {
            return new DbQuery(
                from,
                columns != null ? columns : List.of(),
                filters != null ? filters : List.of(),
                groupBy != null ? groupBy : List.of(),
                aggregations != null ? aggregations : List.of(),
                orderBy != null ? orderBy : List.of(),
                joins != null ? joins : List.of(),
                limit, offset);
        }
    }
}
