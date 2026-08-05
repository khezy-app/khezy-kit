package io.github.khezyapp.ast.core.sql;

import io.github.khezyapp.ast.core.sql.model.DbQuery;
import java.util.List;

/**
 * Interface for converting a {@link DbQuery} model into executable SQL strings.
 * <p>
 * Implementations are responsible for dialect-specific SQL rendering
 * (e.g., PostgreSQL, MySQL, H2). The {@link #toSql(DbQuery, SqlRenderStyle)}
 * variant allows controlling parameter placeholder style.
 * </p>
 */
public interface SqlDialect {

    /**
     * Converts a query to SQL with INDEXED parameter placeholders.
     *
     * @param query the database query model
     * @return the generated SQL string
     */
    String toSql(DbQuery query);

    /**
     * Converts a query to SQL with the specified render style.
     *
     * @param query the database query model
     * @param style the parameter placeholder style
     * @return the generated SQL string
     */
    String toSql(DbQuery query, SqlRenderStyle style);

    /**
     * Extracts bind values from a query model in the order they appear.
     *
     * @param query the database query model
     * @return the list of bind values
     */
    List<Object> extractBindValues(DbQuery query);
}
