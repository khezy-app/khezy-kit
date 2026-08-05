package io.github.khezyapp.ast.core.sql;

import io.github.khezyapp.ast.core.sql.model.DbQuery;
import io.github.khezyapp.ast.core.sql.model.SchemaRegistry;

import java.util.List;
import java.util.Map;

/**
 * Interface for executing database queries and aggregations.
 * <p>
 * Provides methods for actual query execution ({@link #executeQuery},
 * {@link #executeAggregation}), dry-run analysis ({@link #dryRunQuery}),
 * and schema access. Implementations typically wrap jOOQ or JDBC.
 * </p>
 */
public interface DbAccessor {

    /**
     * Executes a SELECT query and returns the result rows.
     *
     * @param query       the query model
     * @param focalObject the focal object (e.g., payload) for context
     * @return the list of result rows as maps
     */
    List<Map<String, Object>> executeQuery(DbQuery query,
                                           Object focalObject);

    /**
     * Executes an aggregation query and returns a single result map.
     *
     * @param query       the query model
     * @param focalObject the focal object for context
     * @return the aggregation result map
     */
    Map<String, Object> executeAggregation(DbQuery query,
                                           Object focalObject);

    /**
     * Performs a dry-run of a query, returning metadata without executing.
     *
     * @param query the query model
     * @return dry-run result with SQL and metadata
     */
    DryRunResult dryRunQuery(DbQuery query);

    /**
     * Generates the SQL for a query (convenience method).
     *
     * @param query the query model
     * @return the generated SQL string
     */
    default String generateSql(final DbQuery query) {
        return dryRunQuery(query).generatedSql();
    }

    /**
     * Returns the schema registry used by this accessor.
     *
     * @return the schema registry
     */
    SchemaRegistry getSchemaRegistry();

    /**
     * Result of a dry-run query operation.
     *
     * @param availableColumns the list of column names that would be returned
     * @param estimatedRowCount estimated number of result rows (-1 if unknown)
     * @param generatedSql      the generated SQL string
     */
    record DryRunResult(
            List<String> availableColumns,
            long estimatedRowCount,
            String generatedSql
    ) {
    }
}
