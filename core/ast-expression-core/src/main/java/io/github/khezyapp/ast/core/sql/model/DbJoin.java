package io.github.khezyapp.ast.core.sql.model;

import java.util.List;

/**
 * Represents a JOIN clause in a database query.
 *
 * @param type            the join type (INNER, LEFT, RIGHT, CROSS)
 * @param targetTable     the table being joined
 * @param sourceColumn    the source column for the ON condition
 * @param targetColumn    the target column for the ON condition
 * @param extraConditions additional ON conditions
 */
public record DbJoin(
        JoinType type,
        DbTable targetTable,
        DbColumn sourceColumn,
        DbColumn targetColumn,
        List<DbFilter> extraConditions
) {
}
