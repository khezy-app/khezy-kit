package io.github.khezyapp.ast.core.sql.dialect;

import io.github.khezyapp.ast.core.CoreUtils;
import io.github.khezyapp.ast.core.sql.SqlDialect;
import io.github.khezyapp.ast.core.sql.SqlRenderStyle;
import io.github.khezyapp.ast.core.sql.model.*;

import java.util.List;
import java.util.Objects;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.Table;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

/**
 * jOOQ-based implementation of {@link SqlDialect}.
 * <p>
 * Converts a {@link DbQuery} model into executable jOOQ {@link org.jooq.Select}
 * queries and renders them as SQL strings. Supports all standard filter operators,
 * aggregation functions, JOIN types, ORDER BY, LIMIT/OFFSET, and schema-qualified
 * table references.
 * </p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JooqSqlDialect implements SqlDialect {

    private final DSLContext dsl;

    /**
     * Creates a jOOQ SQL dialect with the given DSL context.
     *
     * @param dsl the jOOQ DSL context
     */
    public JooqSqlDialect(final DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public String toSql(final DbQuery query) {
        return toSql(query, SqlRenderStyle.INDEXED);
    }

    @Override
    public String toSql(final DbQuery query,
                        final SqlRenderStyle style) {
        final var q = createQuery(query);
        return switch (style) {
            case INLINED -> q.getSQL(ParamType.INLINED);
            case NAMED -> q.getSQL(ParamType.NAMED);
            case INDEXED -> q.getSQL();
        };
    }

    @Override
    public List<Object> extractBindValues(final DbQuery query) {
        return List.of(createQuery(query).getBindValues().toArray());
    }

    /**
     * Converts a {@link DbQuery} into a jOOQ {@link org.jooq.Select} query.
     *
     * @param query the database query model
     * @return the jOOQ select query
     * @throws IllegalArgumentException if the query has no FROM table
     */
    public org.jooq.Select<?> createQuery(final DbQuery query) {
        if (Objects.isNull(query.from())) {
            throw new IllegalArgumentException("DbQuery must have a FROM table");
        }

        final var fromTable = mapTable(query.from());
        final var q = dsl.selectQuery();

        for (final var sf : collectSelectFields(query, fromTable)) {
            q.addSelect(sf);
        }

        q.addFrom(fromTable);

        var sourceTable = fromTable;
        for (final var join : query.joins()) {
            addJoin(q, join, sourceTable);
            sourceTable = mapTable(join.targetTable());
        }

        for (final var filter : query.filters()) {
            q.addConditions(mapCondition(filter, fromTable));
        }

        if (CoreUtils.isNotEmpty(query.groupBy())) {
            q.addGroupBy(query.groupBy().stream()
                    .map(c -> mapColumn(c, fromTable))
                    .toArray(Field[]::new));
        }

        if (Objects.nonNull(query.limit())) {
            q.addLimit(query.limit().intValue());
        }
        if (Objects.nonNull(query.offset())) {
            q.addOffset(query.offset().intValue());
        }

        for (final var order : query.orderBy()) {
            final var field = mapColumn(order.column(), fromTable);
            if (Objects.requireNonNull(order.direction()) == SortDirection.DESC) {
                q.addOrderBy(field.desc());
            } else {
                q.addOrderBy(field.asc());
            }
        }

        return q;
    }

    private void addJoin(final org.jooq.SelectQuery<?> q,
                         final DbJoin join,
                         final Table<?> fromTable) {
        final var joinTable = mapTable(join.targetTable());
        final var joinType = mapJoinType(join.type());

        final var sourceField = mapColumn(join.sourceColumn(), fromTable);
        final var targetField = mapColumn(join.targetColumn(), joinTable);
        var onCondition = sourceField.equal(targetField);

        for (final var extra : join.extraConditions()) {
            onCondition = onCondition.and(mapCondition(extra, fromTable));
        }

        q.addJoin(joinTable, joinType, onCondition);
    }

    private org.jooq.JoinType mapJoinType(final JoinType type) {
        return switch (type) {
            case INNER -> org.jooq.JoinType.JOIN;
            case LEFT -> org.jooq.JoinType.LEFT_OUTER_JOIN;
            case RIGHT -> org.jooq.JoinType.RIGHT_OUTER_JOIN;
            case CROSS -> org.jooq.JoinType.CROSS_JOIN;
        };
    }

    private SelectFieldOrAsterisk[] collectSelectFields(final DbQuery query,
                                                        final Table<?> fromTable) {
        if (!query.columns().isEmpty()) {
            return query.columns().stream()
                    .map(c -> (SelectFieldOrAsterisk) mapColumn(c, fromTable))
                    .toArray(SelectFieldOrAsterisk[]::new);
        }
        if (!query.aggregations().isEmpty()) {
            return query.aggregations().stream()
                    .map(a -> (SelectFieldOrAsterisk) mapAggregation(a, fromTable))
                    .toArray(SelectFieldOrAsterisk[]::new);
        }
        return new SelectFieldOrAsterisk[]{DSL.asterisk()};
    }

    private Table<?> mapTable(final DbTable table) {
        final var name = CoreUtils.isNotEmpty(table.schema())
                ? DSL.name(table.schema(), table.name())
                : DSL.name(table.name());
        final var t = DSL.table(name);
        if (table.alias() != null) {
            return t.as(table.alias());
        }
        return t;
    }

    private Field<Object> mapColumn(final DbColumn col,
                                    final Table<?> tableRef) {
        final var field = DSL.field(DSL.name(tableRef.getName(), col.name()));
        if (CoreUtils.isNotEmpty(col.alias())) {
            return field.as(col.alias());
        }
        return field;
    }

    private Field<?> mapAggregation(final DbAggregation agg,
                                    final Table<?> tableRef) {
        final var field = mapColumn(agg.column(), tableRef);
        final Field<?> result;
        switch (agg.function().toUpperCase()) {
            case "COUNT" -> result = agg.distinct()
                    ? DSL.countDistinct(field) : DSL.count(field);
            case "SUM" -> result = DSL.sum((Field) field);
            case "AVG" -> result = DSL.avg((Field) field);
            case "MAX" -> result = DSL.max(field);
            case "MIN" -> result = DSL.min(field);
            case "COUNT_DISTINCT" -> result = DSL.countDistinct(field);
            default -> throw new IllegalArgumentException(
                    "Unknown aggregation function: " + agg.function());
        }
        return result.as(agg.alias());
    }

    private Condition mapCondition(final DbFilter filter,
                                   final Table<?> fromTable) {
        final var sourceField = mapFilterValueToField(filter.source(), fromTable);
        final var op = filter.operator();
        final var value = filter.value();

        if (value instanceof FilterValue.Literal l) {
            return mapLiteralCondition(sourceField, op, l.value());
        }
        if (value instanceof FilterValue.ColumnRef cr) {
            final var otherField = mapColumn(cr.column(), fromTable);
            return mapBinaryCondition(sourceField, op, otherField);
        }
        if (value instanceof FilterValue.FunctionExpr fe) {
            final var otherField = mapFunctionExprToField(fe, fromTable);
            return mapBinaryCondition(sourceField, op, otherField);
        }
        if (value instanceof FilterValue.Subquery sq) {
            final var subSelect = (org.jooq.Select) createQuery(sq.query());
            return switch (op) {
                case "IN" -> sourceField.in(subSelect);
                case "NOT_IN" -> sourceField.notIn(subSelect);
                default -> DSL.trueCondition();
            };
        }
        return DSL.trueCondition();
    }

    private Field<Object> mapFilterValueToField(final FilterValue fv,
                                                final Table<?> tableRef) {
        if (fv instanceof FilterValue.ColumnRef cr) {
            return mapColumn(cr.column(), tableRef);
        }
        if (fv instanceof FilterValue.Literal l) {
            return DSL.val(l.value());
        }
        if (fv instanceof FilterValue.FunctionExpr fe) {
            return mapFunctionExprToField(fe, tableRef);
        }
        throw new IllegalArgumentException("Cannot map FilterValue to field: " + fv);
    }

    private Field<Object> mapFunctionExprToField(final FilterValue.FunctionExpr fe,
                                                 final Table<?> tableRef) {
        final var fieldArgs = fe.args().stream()
                .map(a -> mapFilterValueToField(a, tableRef))
                .toArray(Field[]::new);

        final Field<?> result;
        switch (fe.function().toUpperCase()) {
            case "UPPER" -> result = DSL.upper((Field<String>) fieldArgs[0]);
            case "LOWER" -> result = DSL.lower((Field<String>) fieldArgs[0]);
            case "LENGTH" -> result = DSL.length((Field<String>) fieldArgs[0]);
            case "TRIM" -> result = DSL.trim((Field<String>) fieldArgs[0]);
            case "CONCAT" -> result = DSL.concat(fieldArgs);
            case "COALESCE" -> result = DSL.coalesce(fieldArgs[0], fieldArgs[1]);
            case "ABS" -> result = DSL.abs(fieldArgs[0]);
            default -> {
                final var sql = fe.function() + "("
                        + String.join(", ", java.util.stream.Stream.of(fieldArgs)
                        .map(a -> "?").toArray(String[]::new))
                        + ")";
                result = DSL.field(sql, fieldArgs);
            }
        }
        return (Field<Object>) result;
    }

    private Condition mapLiteralCondition(final Field<Object> field,
                                          final String op,
                                          final Object val) {
        return switch (op) {
            case "=", "MATCH" -> field.equal(val);
            case "!=" -> field.notEqual(val);
            case ">" -> field.greaterThan(val);
            case ">=" -> field.greaterOrEqual(val);
            case "<" -> field.lessThan(val);
            case "<=" -> field.lessOrEqual(val);
            case "IN" -> field.in(val);
            case "NOT_IN" -> field.notIn(val);
            case "IS_NULL" -> field.isNull();
            case "IS_NOT_NULL" -> field.isNotNull();
            case "CONTAINS" -> field.like("%" + val + "%");
            case "NOT_CONTAINS" -> field.notLike("%" + val + "%");
            case "STARTS_WITH" -> field.like(val + "%");
            case "ENDS_WITH" -> field.like("%" + val);
            case "WILDCARD" -> field.like(String.valueOf(val));
            default -> throw new IllegalArgumentException("Unknown operator: " + op);
        };
    }

    private Condition mapBinaryCondition(final Field<Object> left,
                                         final String op,
                                         final Field<Object> right) {
        return switch (op) {
            case "=" -> left.equal(right);
            case "!=" -> left.notEqual(right);
            case ">" -> left.greaterThan(right);
            case ">=" -> left.greaterOrEqual(right);
            case "<" -> left.lessThan(right);
            case "<=" -> left.lessOrEqual(right);
            default -> throw new IllegalArgumentException(
                    "Unsupported binary operator: " + op);
        };
    }
}
