package io.github.khezyapp.ast.core.sql;

import java.util.Objects;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;
import io.github.khezyapp.ast.core.sql.model.DbColumn;
import io.github.khezyapp.ast.core.sql.model.DbFilter;
import io.github.khezyapp.ast.core.sql.model.DbQuery;
import io.github.khezyapp.ast.core.sql.model.DbTable;
import java.util.List;
import java.util.Map;

/**
 * Evaluator for database table access (dbAccess).
 * <p>
 * Builds a {@link DbQuery} from named arguments (tableName, columns, filters,
 * limit, offset) and executes it via the provided {@link DbAccessor}.
 * Supports dry-run mode for validation without side effects.
 * </p>
 */
public class DbAccessEvaluator implements Evaluator {
    private final DbAccessor dbAccessor;

    /**
     * Creates a dbAccess evaluator with the given accessor.
     *
     * @param dbAccessor the database accessor
     */
    public DbAccessEvaluator(final DbAccessor dbAccessor) {
        this.dbAccessor = dbAccessor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var tableName = (String) args.named().get("tableName");
        if (Objects.isNull(tableName)) {
            return EvaluationOutcome.failure(
                EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                    "Required named argument 'tableName' is missing",
                    "named:tableName"));
        }

        final var table = DbTable.of(tableName);
        final var columnNames = (List<String>) args.named()
            .getOrDefault("columns", List.of());
        final var columns = columnNames.stream()
            .map(name -> DbColumn.of(table, name))
            .toList();
        final var filters = (List<DbFilter>) args.named()
            .getOrDefault("filters", List.of());
        final var limit = (Number) args.named().get("limit");
        final var offset = (Number) args.named().get("offset");

        final var queryBuilder = DbQuery.builder()
            .from(table)
            .columns(columns)
            .filters(filters);

        if (Objects.nonNull(limit)) {
            queryBuilder.limit(limit.longValue());
        }
        if (Objects.nonNull(offset)) {
            queryBuilder.offset(offset.longValue());
        }

        final var query = queryBuilder.build();

        if (ctx.isDryRun()) {
            final var dry = dbAccessor.dryRunQuery(query);
            return EvaluationOutcome.success(List.of(), Map.of(
                "meta.dryRun", true,
                "meta.generatedSql", dry.generatedSql(),
                "meta.estimatedRowCount", dry.estimatedRowCount(),
                "meta.availableColumns", dry.availableColumns()
            ));
        }

        try {
            final var sql = dbAccessor.generateSql(query);
            final var rows = dbAccessor.executeQuery(query, ctx.getBody());
            return EvaluationOutcome.success(rows, Map.of(
                "meta.generatedSql", sql,
                "meta.rowsScanned", rows.size()
            ));
        } catch (final Exception e) {
            return EvaluationOutcome.failure(
                EvaluationError.of(
                        StandardErrors.RUNTIME_ERROR,
                    "DB access error: " + e.getMessage())
            );
        }
    }
}
