package io.github.khezyapp.ast.core.sql.dialect;

import io.github.khezyapp.ast.core.sql.DbAccessor;
import io.github.khezyapp.ast.core.sql.SqlRenderStyle;
import io.github.khezyapp.ast.core.sql.model.DbQuery;
import io.github.khezyapp.ast.core.sql.model.SchemaRegistry;

import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Select;

/**
 * jOOQ-based implementation of {@link DbAccessor}.
 * <p>
 * Executes queries using the provided jOOQ {@link DSLContext}. Delegates SQL
 * generation to {@link JooqSqlDialect} and provides schema registry access.
 * </p>
 */
public class JooqDbAccessor implements DbAccessor {

    private final DSLContext dsl;
    private final JooqSqlDialect dialect;
    private final SchemaRegistry schemaRegistry;

    /**
     * Creates a jOOQ database accessor.
     *
     * @param dsl             the jOOQ DSL context
     * @param schemaRegistry the schema registry
     */
    public JooqDbAccessor(final DSLContext dsl,
                          final SchemaRegistry schemaRegistry) {
        this.dsl = dsl;
        this.dialect = new JooqSqlDialect(dsl);
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public List<Map<String, Object>> executeQuery(final DbQuery query,
                                                  final Object focalObject) {
        final Select<?> select = dialect.createQuery(query);
        final Result<?> result = dsl.fetch(select);
        return result.stream()
                .map(Record::intoMap)
                .toList();
    }

    @Override
    public Map<String, Object> executeAggregation(final DbQuery query,
                                                  final Object focalObject) {
        final Select<?> select = dialect.createQuery(query);
        final Result<?> result = dsl.fetch(select);
        if (result.isEmpty()) {
            return Map.of();
        }
        return result.get(0).intoMap();
    }

    @Override
    public DryRunResult dryRunQuery(final DbQuery query) {
        final var sql = dialect.toSql(query);
        final var availableColumns = query.columns().stream()
                .map(c -> c.table().name() + "." + c.name())
                .toList();
        return new DryRunResult(availableColumns, -1L, sql);
    }

    @Override
    public String generateSql(final DbQuery query) {
        return dialect.toSql(query, SqlRenderStyle.INLINED);
    }

    @Override
    public SchemaRegistry getSchemaRegistry() {
        return schemaRegistry;
    }
}
