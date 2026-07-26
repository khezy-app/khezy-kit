package io.github.khezyapp.ast.core.sql;

import java.util.Objects;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;
import io.github.khezyapp.ast.core.sql.model.DbAggregation;
import io.github.khezyapp.ast.core.sql.model.DbColumn;
import io.github.khezyapp.ast.core.sql.model.DbFilter;
import io.github.khezyapp.ast.core.sql.model.DbQuery;
import io.github.khezyapp.ast.core.sql.model.DbTable;
import io.github.khezyapp.ast.core.sql.model.SchemaRegistry;

import java.util.List;
import java.util.Map;

/**
 * Evaluator for database aggregation queries (dbAggregator).
 * <p>
 * Validates the aggregator function against the schema registry before
 * building and executing the query. Supports SUM, COUNT, AVG, MAX, MIN,
 * and other aggregators defined in the registry.
 * </p>
 */
public class DbAggregatorEvaluator implements Evaluator {
    private final DbAccessor dbAccessor;
    private final SchemaRegistry schemaRegistry;

    /**
     * Creates a dbAggregator evaluator.
     *
     * @param dbAccessor     the database accessor
     * @param schemaRegistry the schema registry for validation
     */
    public DbAggregatorEvaluator(final DbAccessor dbAccessor,
                                 final SchemaRegistry schemaRegistry) {
        this.dbAccessor = dbAccessor;
        this.schemaRegistry = schemaRegistry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var tableName = (String) args.named().get("tableName");
        final var fieldName = (String) args.named().get("fieldName");
        final var aggregator = (String) args.named().get("aggregator");
        final var filters = (List<DbFilter>) args.named()
                .getOrDefault("filters", List.of());
        final var groupByNames = (List<String>) args.named()
                .getOrDefault("groupBy", List.of());
        final var groupBy = groupByNames.stream()
                .map(name -> DbColumn.of(DbTable.of(tableName), name))
                .toList();

        if (Objects.isNull(tableName)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "Required named argument 'tableName' is missing",
                            "named:tableName"));
        }
        if (Objects.isNull(fieldName)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "Required named argument 'fieldName' is missing",
                            "named:fieldName"));
        }
        if (Objects.isNull(aggregator)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "Required named argument 'aggregator' is missing",
                            "named:aggregator"));
        }

        if (!schemaRegistry.isAggregatorValid(aggregator)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.SCHEMA_VALIDATION,
                            "Unknown aggregator function '" + aggregator + "'",
                            "named:aggregator"));
        }

        if (ctx.isDryRun() || schemaRegistry.hasTable(tableName)) {
            final var fieldType = schemaRegistry.getFieldType(tableName, fieldName);
            if (!schemaRegistry.isAggregatorValidForType(aggregator, fieldType)) {
                return EvaluationOutcome.failure(
                        EvaluationError.of(StandardErrors.SCHEMA_VALIDATION,
                                "Aggregator '" + aggregator + "' is not valid for field '"
                                        + fieldName + "' of type " + fieldType,
                                "named:aggregator"));
            }
        }

        final var table = DbTable.of(tableName);
        final var column = DbColumn.of(table, fieldName);
        final var aggregateAlias = "agg_" + fieldName;
        final var aggregation = DbAggregation.of(aggregator, column, aggregateAlias);

        final var query = DbQuery.builder()
                .from(table)
                .filters(filters)
                .groupBy(groupBy)
                .aggregations(List.of(aggregation))
                .build();

        if (ctx.isDryRun()) {
            final var dry = dbAccessor.dryRunQuery(query);
            return EvaluationOutcome.success(0.0, Map.of(
                    "meta.dryRun", true,
                    "meta.generatedSql", dry.generatedSql(),
                    "meta.estimatedRowCount", dry.estimatedRowCount(),
                    "meta.availableColumns", dry.availableColumns()
            ));
        }

        try {
            final var sql = dbAccessor.generateSql(query);
            final var result = dbAccessor.executeAggregation(query, ctx.getBody());
            final var value = result.get(aggregateAlias);
            if (Objects.nonNull(value)) {
                return EvaluationOutcome.success(
                        value,
                        Map.of("meta.generatedSql", sql,
                                "evidence.recordCount", result.size()));
            }
            final var defaultValue = schemaRegistry.defaultAggregatorValue(aggregator);
            return EvaluationOutcome.success(
                    defaultValue,
                    Map.of("meta.generatedSql", sql,
                            "evidence.recordCount", 0));
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "DB aggregation error: " + e.getMessage()));
        }
    }
}
