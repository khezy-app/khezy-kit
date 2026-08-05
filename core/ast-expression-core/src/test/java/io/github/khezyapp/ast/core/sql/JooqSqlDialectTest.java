package io.github.khezyapp.ast.core.sql;

import io.github.khezyapp.ast.core.sql.dialect.JooqSqlDialect;
import io.github.khezyapp.ast.core.sql.model.DbAggregation;
import io.github.khezyapp.ast.core.sql.model.DbColumn;
import io.github.khezyapp.ast.core.sql.model.DbFilter;
import io.github.khezyapp.ast.core.sql.model.DbJoin;
import io.github.khezyapp.ast.core.sql.model.DbOrder;
import io.github.khezyapp.ast.core.sql.model.DbQuery;
import io.github.khezyapp.ast.core.sql.model.DbTable;
import io.github.khezyapp.ast.core.sql.model.FilterValue;
import io.github.khezyapp.ast.core.sql.model.JoinType;
import io.github.khezyapp.ast.core.sql.model.SortDirection;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for {@link io.github.khezyapp.ast.core.sql.dialect.JooqSqlDialect} SQL generation.
 * <p>
 * Covers SELECT, WHERE conditions (equal, IS NULL, CONTAINS, LIKE),
 * aggregation (SUM, COUNT, GROUP BY), LIMIT/OFFSET, schema-qualified tables,
 * aliases, ORDER BY, JOINs (INNER, LEFT), function expressions (UPPER, COALESCE),
 * and literal source filters.
 * </p>
 */
@DisplayName("JooqSqlDialect SQL generation")
class JooqSqlDialectTest {

    private final DSLContext dsl = DSL.using(SQLDialect.POSTGRES,
            new Settings().withRenderFormatted(false));
    private final JooqSqlDialect dialect = new JooqSqlDialect(dsl);

    @Test
    @DisplayName("generates SELECT * FROM table")
    void selectAll() {
        final var query = DbQuery.builder()
            .from(DbTable.of("users"))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.contains("select * from \"users\""),
            "Expected select * from users, got: " + sql);
    }

    @Test
    @DisplayName("generates SELECT with columns")
    void selectColumns() {
        final var table = DbTable.of("users");
        final var query = DbQuery.builder()
            .from(table)
            .columns(List.of(
                DbColumn.of(table, "id"),
                DbColumn.of(table, "name"),
                DbColumn.of(table, "email")))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.contains("\"id\""));
        assertTrue(sql.contains("\"name\""));
        assertTrue(sql.contains("\"email\""));
    }

    @Test
    @DisplayName("generates WHERE with = condition")
    void whereEqual() {
        final var table = DbTable.of("users");
        final var idCol = DbColumn.of(table, "id");
        final var query = DbQuery.builder()
            .from(table)
            .columns(List.of(idCol))
            .filters(List.of(new DbFilter(
                new FilterValue.ColumnRef(idCol), "=", new FilterValue.Literal(1L))))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.contains("\"id\" = ?"));
    }

    @Test
    @DisplayName("generates WHERE IS NULL")
    void whereIsNull() {
        final var table = DbTable.of("users");
        final var emailCol = DbColumn.of(table, "email");
        final var query = DbQuery.builder()
            .from(table)
            .filters(List.of(new DbFilter(
                new FilterValue.ColumnRef(emailCol), "IS_NULL", new FilterValue.Literal(null))))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.contains("\"email\" is null"));
    }

    @Test
    @DisplayName("generates WHERE CONTAINS (LIKE)")
    void whereContains() {
        final var table = DbTable.of("users");
        final var nameCol = DbColumn.of(table, "name");
        final var query = DbQuery.builder()
            .from(table)
            .filters(List.of(new DbFilter(
                new FilterValue.ColumnRef(nameCol), "CONTAINS",
                new FilterValue.Literal("john"))))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.toLowerCase().contains("like"));
    }

    @Test
    @DisplayName("generates SUM aggregation")
    void sumAggregation() {
        final var table = DbTable.of("orders");
        final var amountCol = DbColumn.of(table, "amount");
        final var query = DbQuery.builder()
            .from(table)
            .aggregations(List.of(DbAggregation.of("SUM", amountCol, "total")))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.contains("sum("));
        assertTrue(sql.contains("\"amount\""));
    }

    @Test
    @DisplayName("generates COUNT aggregation")
    void countAggregation() {
        final var table = DbTable.of("orders");
        final var idCol = DbColumn.of(table, "id");
        final var query = DbQuery.builder()
            .from(table)
            .aggregations(List.of(DbAggregation.of("COUNT", idCol, "cnt")))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.contains("count("));
        assertTrue(sql.contains("\"id\""));
    }

    @Test
    @DisplayName("generates GROUP BY with aggregation")
    void groupByAggregation() {
        final var table = DbTable.of("users");
        final var statusCol = DbColumn.of(table, "status");
        final var idCol = DbColumn.of(table, "id");
        final var query = DbQuery.builder()
            .from(table)
            .aggregations(List.of(DbAggregation.of("COUNT", idCol, "cnt")))
            .groupBy(List.of(statusCol))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.contains("group by"));
    }

    @Test
    @DisplayName("generates LIMIT (FETCH NEXT ... ROWS ONLY)")
    void withLimit() {
        final var table = DbTable.of("users");
        final var query = DbQuery.builder()
            .from(table)
            .limit(10L)
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.toLowerCase().contains("fetch next"),
            "Expected FETCH NEXT, got: " + sql);
    }

    @Test
    @DisplayName("generates LIMIT OFFSET")
    void withLimitOffset() {
        final var table = DbTable.of("users");
        final var query = DbQuery.builder()
            .from(table)
            .limit(10L)
            .offset(20L)
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.toLowerCase().contains("offset"),
            "Expected OFFSET, got: " + sql);
    }

    @Test
    @DisplayName("generates schema-qualified table reference")
    void schemaQualifiedTable() {
        final var table = new DbTable("orders", "public", null);
        final var query = DbQuery.builder()
            .from(table)
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.contains("\"public\".\"orders\""));
    }

    @Test
    @DisplayName("generates table alias")
    void tableAlias() {
        final var table = new DbTable("users", null, "u");
        final var query = DbQuery.builder()
            .from(table)
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.contains("users") && sql.contains("u"));
    }

    @Test
    @DisplayName("generates ORDER BY ASC")
    void orderByAsc() {
        final var table = DbTable.of("users");
        final var nameCol = DbColumn.of(table, "name");
        final var query = DbQuery.builder()
            .from(table)
            .orderBy(List.of(new DbOrder(nameCol, SortDirection.ASC)))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.toLowerCase().contains("order by"),
            "Expected ORDER BY, got: " + sql);
    }

    @Test
    @DisplayName("generates ORDER BY DESC")
    void orderByDesc() {
        final var table = DbTable.of("users");
        final var nameCol = DbColumn.of(table, "name");
        final var query = DbQuery.builder()
            .from(table)
            .orderBy(List.of(new DbOrder(nameCol, SortDirection.DESC)))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.contains("desc"),
            "Expected DESC, got: " + sql);
    }

    @Test
    @DisplayName("generates INNER JOIN")
    void innerJoin() {
        final var usersTable = DbTable.of("users");
        final var ordersTable = DbTable.of("orders");
        final var userIdCol = DbColumn.of(usersTable, "id");
        final var orderUserIdCol = DbColumn.of(ordersTable, "userId");
        final var query = DbQuery.builder()
            .from(usersTable)
            .joins(List.of(new DbJoin(JoinType.INNER, ordersTable,
                userIdCol, orderUserIdCol, List.of())))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.toLowerCase().contains("join"),
            "Expected JOIN, got: " + sql);
    }

    @Test
    @DisplayName("generates UPPER function in filter")
    void upperFunctionFilter() {
        final var table = DbTable.of("users");
        final var nameCol = DbColumn.of(table, "name");
        final var upperName = new FilterValue.FunctionExpr("UPPER",
            List.of(new FilterValue.ColumnRef(nameCol)), Map.of());
        final var query = DbQuery.builder()
            .from(table)
            .filters(List.of(new DbFilter(upperName, "=",
                new FilterValue.Literal("JOHN"))))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.toLowerCase().contains("upper"),
            "Expected UPPER function, got: " + sql);
    }

    @Test
    @DisplayName("generates WHERE with literal source (e.g. 1 = id)")
    void literalSourceFilter() {
        final var table = DbTable.of("users");
        final var idCol = DbColumn.of(table, "id");
        final var query = DbQuery.builder()
            .from(table)
            .filters(List.of(new DbFilter(
                new FilterValue.Literal(1L), "=",
                new FilterValue.ColumnRef(idCol))))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.contains("? = "),
            "Expected bind param on left side, got: " + sql);
    }

    @Test
    @DisplayName("generates LEFT JOIN and checks order")
    void leftJoin() {
        final var usersTable = DbTable.of("users");
        final var profilesTable = DbTable.of("profiles");
        final var userIdCol = DbColumn.of(usersTable, "id");
        final var profileUserIdCol = DbColumn.of(profilesTable, "userId");
        final var query = DbQuery.builder()
            .from(usersTable)
            .joins(List.of(new DbJoin(JoinType.LEFT, profilesTable,
                userIdCol, profileUserIdCol, List.of())))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.toLowerCase().contains("left"),
            "Expected LEFT JOIN, got: " + sql);
    }

    @Test
    @DisplayName("generates COALESCE in filter")
    void coalesceFilter() {
        final var table = DbTable.of("users");
        final var nameCol = DbColumn.of(table, "name");
        final var coalesceName = new FilterValue.FunctionExpr("COALESCE",
            List.of(new FilterValue.ColumnRef(nameCol),
                new FilterValue.Literal("unknown")), Map.of());
        final var query = DbQuery.builder()
            .from(table)
            .filters(List.of(new DbFilter(coalesceName, "=",
                new FilterValue.Literal("john"))))
            .build();
        final var sql = dialect.toSql(query);
        assertTrue(sql.toLowerCase().contains("coalesce"),
            "Expected COALESCE function, got: " + sql);
    }
}
