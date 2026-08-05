package io.github.khezyapp.ast.core.sql;

import io.github.khezyapp.ast.core.eval.AstEvaluator;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.function.FunctionDefinition;
import io.github.khezyapp.ast.core.function.FunctionRegistry;
import io.github.khezyapp.ast.core.model.CoreFunctions;
import io.github.khezyapp.ast.core.model.Node;
import io.github.khezyapp.ast.core.model.ParamSpec;
import io.github.khezyapp.ast.core.model.ParamType;
import io.github.khezyapp.ast.core.nullstrategy.NullStrategies;
import io.github.khezyapp.ast.core.sql.model.DbQuery;
import io.github.khezyapp.ast.core.sql.model.FieldMetadata;
import io.github.khezyapp.ast.core.sql.model.SchemaRegistry;
import io.github.khezyapp.ast.core.sql.model.TableMetadata;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for dry-run mode in database access and aggregation evaluators.
 * <p>
 * Verifies that dry-run operations return metadata without executing queries,
 * and that aggregator validation against schema works correctly.
 * </p>
 */
@DisplayName("DbAccess dry run")
class DbAccessDryRunTest {

    private FunctionRegistry registry;
    private AstEvaluator evaluator;
    private SchemaRegistry schemaRegistry;

    @BeforeEach
    void setUp() {
        registry = FunctionRegistry.empty(NullStrategies.PROPAGATE);

        schemaRegistry = new SchemaRegistry(List.of(
                new TableMetadata("users", null,
                        Map.of(
                                "id", new FieldMetadata("id", "INTEGER", false, true, false),
                                "name", new FieldMetadata("name", "STRING", true, false, false),
                                "email", new FieldMetadata("email", "STRING", true, false, false)
                        ),
                        Map.of()),
                new TableMetadata("orders", null,
                        Map.of(
                                "id", new FieldMetadata("id", "INTEGER", false, true, false),
                                "userId", new FieldMetadata("userId", "INTEGER", true, false, true),
                                "amount", new FieldMetadata("amount", "FLOAT", true, false, false)
                        ),
                        Map.of())
        ));

        final var dbAccessor = new DbAccessor() {
            @Override
            public List<Map<String, Object>> executeQuery(final DbQuery query,
                                                          final Object focalObject) {
                throw new UnsupportedOperationException("Should not be called in dry run");
            }

            @Override
            public Map<String, Object> executeAggregation(final DbQuery query,
                                                          final Object focalObject) {
                throw new UnsupportedOperationException("Should not be called in dry run");
            }

            @Override
            public DryRunResult dryRunQuery(final DbQuery query) {
                return new DbAccessor.DryRunResult(List.of("col1", "col2"), 100L,
                        "SELECT col1, col2 FROM users WHERE 1=1");
            }

            @Override
            public SchemaRegistry getSchemaRegistry() {
                return schemaRegistry;
            }
        };

        registry.register(FunctionDefinition.builder()
                .functionId(CoreFunctions.DB_ACCESS)
                .evaluator(new DbAccessEvaluator(dbAccessor))
                .namedParam(ParamSpec.required("tableName", ParamType.STRING))
                .namedParam(ParamSpec.optional("columns", ParamType.ANY))
                .namedParam(ParamSpec.optional("filters", ParamType.ANY))
                .namedParam(ParamSpec.optional("limit", ParamType.INTEGER))
                .namedParam(ParamSpec.optional("offset", ParamType.INTEGER))
                .build());
        registry.register(FunctionDefinition.builder()
                .functionId(CoreFunctions.DB_AGGREGATOR)
                .evaluator(new DbAggregatorEvaluator(dbAccessor, schemaRegistry))
                .namedParam(ParamSpec.required("tableName", ParamType.STRING))
                .namedParam(ParamSpec.required("fieldName", ParamType.STRING))
                .namedParam(ParamSpec.required("aggregator", ParamType.STRING))
                .namedParam(ParamSpec.optional("filters", ParamType.ANY))
                .namedParam(ParamSpec.optional("groupBy", ParamType.ANY))
                .build());
        evaluator = new AstEvaluator();
    }

    @Test
    @DisplayName("dry run returns metadata without executing query")
    void dryRunReturnsMetadataWithoutExecutingQuery() {
        final var ctx = new EvaluationContext.Builder(registry)
                .body(Map.of())
                .dryRun(true)
                .build();
        final var node = Node.function(CoreFunctions.DB_ACCESS,
                List.of(),
                Map.of(
                        "tableName", Node.constant("users"),
                        "columns", Node.constant(List.of("col1", "col2"))));
        final var result = evaluator.evaluate(node, ctx);

        assertAll(
                () -> assertTrue(result.errors().isEmpty()),
                () -> assertEquals(true, result.getAttribute("meta.dryRun")),
                () -> assertInstanceOf(List.class, result.returnValue()),
                () -> assertTrue(result.hasAttribute("meta.generatedSql"))
        );
    }

    @Test
    @DisplayName("without dry run calls executeQuery")
    void withoutDryRunCallsExecuteQuery() {
        final var ctx = new EvaluationContext.Builder(registry)
                .body(Map.of())
                .dryRun(false)
                .build();
        final var node = Node.function(CoreFunctions.DB_ACCESS,
                List.of(),
                Map.of(
                        "tableName", Node.constant("users"),
                        "columns", Node.constant(List.of("col1", "col2"))));
        final var result = evaluator.evaluate(node, ctx);

        assertFalse(result.errors().isEmpty());
    }

    @Test
    @DisplayName("aggregator dry run returns metadata")
    void aggregatorDryRunReturnsMetadata() {
        final var ctx = new EvaluationContext.Builder(registry)
                .body(Map.of())
                .dryRun(true)
                .build();
        final var node = Node.function(CoreFunctions.DB_AGGREGATOR,
                List.of(),
                Map.of(
                        "tableName", Node.constant("orders"),
                        "fieldName", Node.constant("amount"),
                        "aggregator", Node.constant("SUM")));
        final var result = evaluator.evaluate(node, ctx);

        assertAll(
                () -> assertTrue(result.errors().isEmpty()),
                () -> assertEquals(true, result.getAttribute("meta.dryRun"))
        );
    }

    @Test
    @DisplayName("aggregator rejects unknown aggregator function")
    void aggregatorRejectsUnknownAggregator() {
        final var ctx = new EvaluationContext.Builder(registry)
                .body(Map.of())
                .dryRun(true)
                .build();
        final var node = Node.function(CoreFunctions.DB_AGGREGATOR,
                List.of(),
                Map.of(
                        "tableName", Node.constant("orders"),
                        "fieldName", Node.constant("amount"),
                        "aggregator", Node.constant("UNKNOWN_FUNC")));
        final var result = evaluator.evaluate(node, ctx);

        assertFalse(result.errors().isEmpty());
    }

    @Test
    @DisplayName("aggregator rejects invalid aggregator for field type")
    void aggregatorRejectsInvalidAggregatorForFieldType() {
        final var ctx = new EvaluationContext.Builder(registry)
                .body(Map.of())
                .dryRun(true)
                .build();
        final var node = Node.function(CoreFunctions.DB_AGGREGATOR,
                List.of(),
                Map.of(
                        "tableName", Node.constant("users"),
                        "fieldName", Node.constant("email"),
                        "aggregator", Node.constant("SUM")));
        final var result = evaluator.evaluate(node, ctx);

        assertFalse(result.errors().isEmpty());
    }
}
