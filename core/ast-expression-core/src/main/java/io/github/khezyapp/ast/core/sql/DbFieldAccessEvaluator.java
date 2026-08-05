package io.github.khezyapp.ast.core.sql;

import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;
import io.github.khezyapp.ast.core.sql.model.DbColumn;
import io.github.khezyapp.ast.core.sql.model.DbFilter;
import io.github.khezyapp.ast.core.sql.model.DbJoin;
import io.github.khezyapp.ast.core.sql.model.DbQuery;
import io.github.khezyapp.ast.core.sql.model.DbTable;
import io.github.khezyapp.ast.core.sql.model.FilterValue;
import io.github.khezyapp.ast.core.sql.model.JoinType;
import io.github.khezyapp.ast.core.sql.model.LinkMetadata;
import io.github.khezyapp.ast.core.sql.model.SchemaRegistry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluator for cross-table field access via join paths (dbFieldAccess).
 * <p>
 * Resolves a link path from a starting table to a target field, automatically
 * building join chains and executing the query. Supports dry-run mode with
 * fake value generation based on field data type.
 * </p>
 */
public class DbFieldAccessEvaluator implements Evaluator {
    private final DbAccessor dbAccessor;
    private final SchemaRegistry schemaRegistry;

    /**
     * Creates a dbFieldAccess evaluator.
     *
     * @param dbAccessor     the database accessor
     * @param schemaRegistry the schema registry for link resolution
     */
    public DbFieldAccessEvaluator(final DbAccessor dbAccessor,
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
        final var path = (List<String>) args.named().getOrDefault("path", List.of());

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
        if (path.isEmpty()) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "Required named argument 'path' is empty",
                            "named:path"));
        }

        final var linkChain = resolvePath(tableName, path);
        if (linkChain.isEmpty()) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.SCHEMA_VALIDATION,
                            "Failed to resolve path '" + path + "' from table '"
                                    + tableName + "'"));
        }

        final var payload = ctx.getBody() instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.<String, Object>of();

        if (ctx.isDryRun()) {
            return handleDryRun(linkChain, fieldName);
        }

        return executeFieldAccess(linkChain, fieldName, payload);
    }

    private List<LinkMetadata> resolvePath(final String startTable,
                                           final List<String> path) {
        final var links = new ArrayList<LinkMetadata>();
        var currentTable = startTable;
        for (final var linkName : path) {
            final var link = schemaRegistry.getLink(currentTable, linkName);
            if (Objects.isNull(link)) {
                return List.of();
            }
            links.add(link);
            currentTable = link.parentTableName();
        }
        return links;
    }

    private EvaluationOutcome handleDryRun(final List<LinkMetadata> linkChain,
                                           final String fieldName) {
        final var lastTable = linkChain.get(linkChain.size() - 1).parentTableName();
        final var fieldError = schemaRegistry.validateField(lastTable, fieldName);
        if (Objects.nonNull(fieldError)) {
            return EvaluationOutcome.failure(fieldError);
        }

        final var fakeValue = generateFakeValue(lastTable, fieldName);
        return EvaluationOutcome.success(fakeValue, Map.of("meta.dryRun", true));
    }

    private EvaluationOutcome executeFieldAccess(final List<LinkMetadata> linkChain,
                                                 final String fieldName,
                                                 final Map<String, Object> payload) {
        try {
            final var firstLink = linkChain.get(0);
            final var lastLink = linkChain.get(linkChain.size() - 1);
            final var fromTable = DbTable.of(firstLink.parentTableName());
            final var targetTable = DbTable.of(lastLink.parentTableName());
            final var columns = List.of(DbColumn.of(targetTable, fieldName));

            final var fkValue = payload.get(firstLink.childFieldName());
            if (Objects.isNull(fkValue)) {
                return EvaluationOutcome.success(null);
            }

            final var whereFilters = List.of(
                    DbFilter.of(
                            DbColumn.of(fromTable, firstLink.parentFieldName()),
                            "=",
                            new FilterValue.Literal(fkValue)
                    )
            );

            final var joins = buildJoins(linkChain);

            final var query = DbQuery.builder()
                    .from(fromTable)
                    .columns(columns)
                    .filters(whereFilters)
                    .joins(joins)
                    .limit(1L)
                    .build();

            final var sql = dbAccessor.generateSql(query);
            final var rows = dbAccessor.executeQuery(query, payload);
            if (rows.isEmpty()) {
                return EvaluationOutcome.success(null,
                        Map.of("meta.generatedSql", sql));
            }
            final var value = rows.get(0).get(fieldName);
            return EvaluationOutcome.success(value,
                    Map.of("meta.generatedSql", sql));
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "DB field access error: " + e.getMessage()));
        }
    }

    private List<DbJoin> buildJoins(final List<LinkMetadata> linkChain) {
        if (linkChain.size() < 2) {
            return List.of();
        }

        final var joins = new ArrayList<DbJoin>();
        for (var i = 1; i < linkChain.size(); i++) {
            final var link = linkChain.get(i);
            final var sourceTable = DbTable.of(link.childTableName());
            final var targetTable = DbTable.of(link.parentTableName());
            joins.add(
                    new DbJoin(
                            JoinType.LEFT,
                            targetTable,
                            DbColumn.of(sourceTable, link.childFieldName()),
                            DbColumn.of(targetTable, link.parentFieldName()),
                            List.of()
                    )
            );
        }
        return joins;
    }

    private Object generateFakeValue(final String tableName,
                                     final String fieldName) {
        final var field = schemaRegistry.getField(tableName, fieldName);
        if (Objects.isNull(field)) {
            return "fake:" + tableName + "." + fieldName;
        }
        return switch (field.dataType()) {
            case "STRING" -> "fake:" + tableName + "." + fieldName;
            case "INTEGER" -> 1;
            case "BOOLEAN" -> true;
            case "FLOAT" -> 1.0;
            case "TIMESTAMP" -> Instant.now();
            default -> "fake:" + tableName + "." + fieldName;
        };
    }
}
