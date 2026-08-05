package io.github.khezyapp.ast.core.index;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.khezyapp.ast.core.index.analyzer.AstIndexAnalyzer;
import io.github.khezyapp.ast.core.index.model.AggregateQueryFamily;
import io.github.khezyapp.ast.core.index.model.ConcreteIndex;
import io.github.khezyapp.ast.core.index.model.FilterOperator;
import io.github.khezyapp.ast.core.index.model.IndexFamily;
import io.github.khezyapp.ast.core.index.model.IndexType;
import io.github.khezyapp.ast.core.index.planner.IndexPlanner;
import io.github.khezyapp.ast.core.model.FunctionId;
import io.github.khezyapp.ast.core.model.Node;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive end-to-end test suite for the dynamic index detection system.
 * <p>
 * Covers field expression extraction, operator classification,
 * {@link io.github.khezyapp.ast.core.index.model.ConcreteIndex} coverage,
 * {@link io.github.khezyapp.ast.core.index.planner.IndexPlanner} planning,
 * filtering, minimization, projection, and 26 real-world use cases including
 * e-commerce queries, multi-tenant analytics, functional indexes, GIN indexes,
 * mixed filter types, and more.
 * </p>
 */
@DisplayName("Dynamic index detection")
class EndToEndIndexDetectionTest {

    // ── Builder helpers ──────────────────────────────────────────────

    private static Node aggregator(final String tableName,
                                   final String fieldName,
                                   final Node... filters) {
        final var named = new HashMap<String, Node>();
        named.put("tableName", Node.constant(tableName));
        named.put("fieldName", Node.constant(fieldName));
        if (filters.length > 0) {
            named.put("filters", Node.function(FunctionId.of("filter_list"), filters));
        }
        return Node.function(FunctionId.of("dbAggregator"), List.of(), named);
    }

    private static Node filterNode(final String operator,
                                   final Node fieldExpr,
                                   final Node value) {
        return Node.function(
                FunctionId.of("filter"),
                List.of(),
                Map.of(
                        "operator", Node.constant(operator),
                        "fieldName", fieldExpr,
                        "value", value
                ));
    }

    private static Node field(final String name) {
        return Node.constant(name);
    }

    private static Node transform(final String fn,
                                  final Node inner) {
        return Node.function(FunctionId.of(fn), inner);
    }

    private static Node payload(final String fieldName) {
        return Node.function(
                FunctionId.of("payload"),
                List.of(),
                Map.of("fieldName", Node.constant(fieldName)));
    }

    private static Node constant(final Object value) {
        return Node.constant(value);
    }

    private static AstIndexAnalyzer analyzer() {
        return new AstIndexAnalyzer();
    }

    private static IndexPlanner planner() {
        return new IndexPlanner();
    }

    // ── Analyzer unit tests ──────────────────────────────────────────

    @Nested
    @DisplayName("FieldExpression extraction")
    class AnalyzerFieldExtractionTests {

        @Test
        @DisplayName("extracts from simple constant node")
        void extractsFromSimpleConstant() {
            final var result = AstIndexAnalyzer.extractFieldExpression(Node.constant("status"));

            assertAll(
                    () -> assertEquals("status", result.columnName()),
                    () -> assertFalse(result.isFunctional()),
                    () -> assertTrue(result.isResolvable())
            );
        }

        @Test
        @DisplayName("extracts from payload access")
        void extractsFromPayloadAccess() {
            final var expr = payload("status");
            final var result = AstIndexAnalyzer.extractFieldExpression(expr);

            assertAll(
                    () -> assertEquals("status", result.columnName()),
                    () -> assertFalse(result.isFunctional())
            );
        }

        @Test
        @DisplayName("extracts from chained payload access recursively")
        void extractsFromChainedPayloadAccess() {
            final var inner = Node.function(
                    FunctionId.of("payload"),
                    List.of(),
                    Map.of("fieldName", Node.constant("status")));
            final var outer = Node.function(
                    FunctionId.of("payload"),
                    List.of(),
                    Map.of("fieldName", inner));
            final var result = AstIndexAnalyzer.extractFieldExpression(outer);

            assertEquals("status", result.columnName());
        }

        @Test
        @DisplayName("extracts from transform function as functional")
        void extractsFromTransformFunction() {
            final var expr = transform("upper", field("email"));
            final var result = AstIndexAnalyzer.extractFieldExpression(expr);

            assertAll(
                    () -> assertEquals("email", result.columnName()),
                    () -> assertTrue(result.isFunctional()),
                    () -> assertEquals("upper", result.transformFunction())
            );
        }

        @Test
        @DisplayName("extracts lower transform as functional")
        void extractsLowerTransform() {
            final var expr = transform("lower", field("last_name"));
            final var result = AstIndexAnalyzer.extractFieldExpression(expr);

            assertAll(
                    () -> assertEquals("last_name", result.columnName()),
                    () -> assertTrue(result.isFunctional()),
                    () -> assertEquals("lower", result.transformFunction())
            );
        }

        @Test
        @DisplayName("extracts trim transform as functional")
        void extractsTrimTransform() {
            final var expr = transform("trim", field("username"));
            final var result = AstIndexAnalyzer.extractFieldExpression(expr);

            assertAll(
                    () -> assertEquals("username", result.columnName()),
                    () -> assertTrue(result.isFunctional()),
                    () -> assertEquals("trim", result.transformFunction())
            );
        }

        @Test
        @DisplayName("marks composite expression as unresolvable")
        void marksCompositeExpressionAsUnresolvable() {
            final var expr = Node.function(FunctionId.of("concat"), field("first"), field("_last"));
            final var result = AstIndexAnalyzer.extractFieldExpression(expr);

            assertFalse(result.isResolvable());
        }

        @Test
        @DisplayName("marks unknown transform as unresolvable")
        void marksUnknownTransformAsUnresolvable() {
            final var expr = transform("extractYear", field("date"));
            final var result = AstIndexAnalyzer.extractFieldExpression(expr);

            assertFalse(result.isResolvable());
        }
    }

    @Nested
    @DisplayName("Operator classification")
    class AnalyzerOperatorClassificationTests {

        @Test
        @DisplayName("classifies equals operator")
        void classifiesEquals() {
            assertEquals(FilterOperator.EQUAL, AstIndexAnalyzer.classifyOperator("="));
        }

        @Test
        @DisplayName("classifies less than operator")
        void classifiesLessThan() {
            assertEquals(FilterOperator.LESS_THAN, AstIndexAnalyzer.classifyOperator("<"));
        }

        @Test
        @DisplayName("classifies greater or equal operator")
        void classifiesGreaterOrEqual() {
            assertEquals(FilterOperator.GREATER_OR_EQUAL, AstIndexAnalyzer.classifyOperator(">="));
        }

        @Test
        @DisplayName("classifies isInList operator")
        void classifiesIsInList() {
            assertEquals(FilterOperator.IS_IN_LIST, AstIndexAnalyzer.classifyOperator("IsInList"));
        }

        @Test
        @DisplayName("classifies fuzzyMatch operator")
        void classifiesFuzzyMatch() {
            assertEquals(FilterOperator.FUZZY_MATCH, AstIndexAnalyzer.classifyOperator("FuzzyMatch"));
        }

        @Test
        @DisplayName("classifies unknown operator as other")
        void classifiesUnknownAsOther() {
            assertEquals(FilterOperator.OTHER, AstIndexAnalyzer.classifyOperator("UnknownOp"));
        }

        @Test
        @DisplayName("classifies jsonb_contains operator")
        void classifiesJsonbContains() {
            assertEquals(FilterOperator.JSONB_CONTAINS, AstIndexAnalyzer.classifyOperator("jsonb_contains"));
        }

        @Test
        @DisplayName("classifies jsonb_key_exists operator")
        void classifiesJsonbKeyExists() {
            assertEquals(FilterOperator.JSONB_KEY_EXISTS, AstIndexAnalyzer.classifyOperator("jsonb_key_exists"));
        }

        @Test
        @DisplayName("classifies fulltext_match operator")
        void classifiesFulltextMatch() {
            assertEquals(FilterOperator.FULLTEXT_MATCH, AstIndexAnalyzer.classifyOperator("fulltext_match"));
        }

        @Test
        @DisplayName("classifies array_contains operator")
        void classifiesArrayContains() {
            assertEquals(FilterOperator.ARRAY_CONTAINS, AstIndexAnalyzer.classifyOperator("array_contains"));
        }

        @Test
        @DisplayName("classifies jsonb_any_key_exists operator")
        void classifiesAnyKeyExists() {
            assertEquals(FilterOperator.ANY_KEY_EXISTS, AstIndexAnalyzer.classifyOperator("jsonb_any_key_exists"));
        }
    }

    // ── Coverage model tests ─────────────────────────────────────────

    @Nested
    @DisplayName("ConcreteIndex coverage")
    class ConcreteIndexCoverageTests {

        @Test
        @DisplayName("exact match with fixed columns is covered")
        void exactMatchWithFixedColumns() {
            final var family = IndexFamily.builder()
                    .tableName("orders")
                    .addFixed("customer_id")
                    .addFixed("status")
                    .addFlex("region")
                    .last("created_at")
                    .build();
            final var existing = ConcreteIndex.bTree(
                    "orders",
                    List.of("customer_id", "status", "region", "created_at"),
                    List.of(),
                    IndexType.AGGREGATION);

            assertTrue(existing.covers(family));
        }

        @Test
        @DisplayName("table name mismatch is not covered")
        void tableNameMismatch() {
            final var family = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("status")
                    .build();
            final var existing = ConcreteIndex.bTree(
                    "users",
                    List.of("status"),
                    List.of(),
                    IndexType.AGGREGATION);

            assertFalse(existing.covers(family));
        }

        @Test
        @DisplayName("missing prefix columns is not covered")
        void missingPrefixColumns() {
            final var family = IndexFamily.builder()
                    .tableName("orders")
                    .addFixed("customer_id")
                    .addFixed("status")
                    .build();
            final var existing = ConcreteIndex.bTree(
                    "orders",
                    List.of("status"),
                    List.of(),
                    IndexType.AGGREGATION);

            assertFalse(existing.covers(family));
        }

        @Test
        @DisplayName("insufficient included columns is not covered")
        void insufficientIncludedColumns() {
            final var family = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("user_id")
                    .addIncluded("amount")
                    .addIncluded("tax")
                    .build();
            final var existing = ConcreteIndex.bTree(
                    "orders",
                    List.of("user_id"),
                    List.of("amount"),
                    IndexType.AGGREGATION);

            assertFalse(existing.covers(family));
        }

        @Test
        @DisplayName("superset of included columns covers")
        void supersetOfIncludedColumns() {
            final var family = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("user_id")
                    .addIncluded("amount")
                    .build();
            final var existing = ConcreteIndex.bTree(
                    "orders",
                    List.of("user_id"),
                    List.of("amount", "tax", "status"),
                    IndexType.AGGREGATION);

            assertTrue(existing.covers(family));
        }

        @Test
        @DisplayName("wrong last column position is not covered")
        void wrongLastColumnPosition() {
            final var family = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("user_id")
                    .last("created_at")
                    .build();
            final var existing = new ConcreteIndex(
                    "orders",
                    List.of("created_at", "user_id"),
                    List.of(),
                    IndexType.AGGREGATION,
                    "");

            assertFalse(existing.covers(family));
        }

        @Test
        @DisplayName("GIN index covers same column with same operator class")
        void ginCoversSameColumn() {
            final var existing = ConcreteIndex.gin("events", "data", "jsonb_ops");
            final var proposed = ConcreteIndex.gin("events", "data", "jsonb_ops");

            assertTrue(existing.coversGin(proposed));
        }

        @Test
        @DisplayName("GIN index with different table does not cover")
        void ginDifferentTable() {
            final var existing = ConcreteIndex.gin("events", "data", "jsonb_ops");
            final var proposed = ConcreteIndex.gin("other", "data", "jsonb_ops");

            assertFalse(existing.coversGin(proposed));
        }

        @Test
        @DisplayName("GIN index with different column does not cover")
        void ginDifferentColumn() {
            final var existing = ConcreteIndex.gin("events", "data", "jsonb_ops");
            final var proposed = ConcreteIndex.gin("events", "other", "jsonb_ops");

            assertFalse(existing.coversGin(proposed));
        }

        @Test
        @DisplayName("GIN jsonb_ops covers jsonb_path_ops")
        void ginJsonbOpsCoversJsonbPathOps() {
            final var existing = ConcreteIndex.gin("events", "data", "jsonb_ops");
            final var proposed = ConcreteIndex.gin("events", "data", "jsonb_path_ops");

            assertTrue(existing.coversGin(proposed));
        }

        @Test
        @DisplayName("GIN jsonb_path_ops does not cover jsonb_ops")
        void ginJsonbPathOpsDoesNotCoverJsonbOps() {
            final var existing = ConcreteIndex.gin("events", "data", "jsonb_path_ops");
            final var proposed = ConcreteIndex.gin("events", "data", "jsonb_ops");

            assertFalse(existing.coversGin(proposed));
        }
    }

    // ── Planner unit tests ───────────────────────────────────────────

    @Nested
    @DisplayName("IndexPlanner.planIndexFamilies")
    class PlannerPlanIndexFamiliesTests {

        @Test
        @DisplayName("maps eq to flex, other to included, no ineq")
        void mapsEqToFlexOtherToIncludedNoIneq() {
            final var qf = AggregateQueryFamily.builder()
                    .tableName("orders")
                    .fieldName("total")
                    .addEq("status")
                    .addEq("region")
                    .addOther("description")
                    .build();

            final var families = planner().planIndexFamilies(qf);

            assertEquals(1, families.size());
            final var f = families.iterator().next();
            assertAll(
                    () -> assertEquals(Set.of("region", "status"), f.flex()),
                    () -> assertTrue(f.fixed().isEmpty()),
                    () -> assertEquals("", f.last()),
                    () -> assertEquals(Set.of("description"), f.included()),
                    () -> assertTrue(f.functional().isEmpty()));
        }

        @Test
        @DisplayName("creates one family per ineq column")
        void createsOneFamilyPerIneqColumn() {
            final var qf = AggregateQueryFamily.builder()
                    .tableName("products")
                    .fieldName("count")
                    .addEq("category")
                    .addIneq("price")
                    .addIneq("rating")
                    .build();

            final var families = planner().planIndexFamilies(qf);

            assertEquals(2, families.size());
            for (final var f : families) {
                assertEquals(Set.of("category"), f.flex());
                assertTrue(f.fixed().isEmpty());
            }
        }

        @Test
        @DisplayName("returns empty for no indexable conditions")
        void returnsEmptyForNoIndexableConditions() {
            final var qf = AggregateQueryFamily.builder()
                    .tableName("logs")
                    .fieldName("count")
                    .addOther("level")
                    .build();

            final var families = planner().planIndexFamilies(qf);

            assertTrue(families.isEmpty());
        }

        @Test
        @DisplayName("carries functional columns through")
        void carriesFunctionalColumnsThrough() {
            final var qf = AggregateQueryFamily.builder()
                    .tableName("users")
                    .fieldName("logins")
                    .addEq("status")
                    .addEqFunctional("email")
                    .addOther("name")
                    .build();

            final var families = planner().planIndexFamilies(qf);
            assertEquals(1, families.size());
            final var f = families.iterator().next();

            assertEquals(Set.of("email"), f.functional());
            assertEquals(Set.of("status", "email"), f.flex());
        }
    }

    @Nested
    @DisplayName("IndexPlanner.filterExisting")
    class PlannerFilterExistingTests {

        @Test
        @DisplayName("removes covered families")
        void removesCoveredFamilies() {
            final var family = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("user_id")
                    .build();
            final var existing = List.of(ConcreteIndex.bTree(
                    "orders",
                    List.of("user_id"),
                    List.of(),
                    IndexType.AGGREGATION));

            final var result = planner().filterExisting(Set.of(family), existing);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("keeps uncovered families")
        void keepsUncoveredFamilies() {
            final var family = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("user_id")
                    .build();
            final var existing = List.of(ConcreteIndex.bTree(
                    "users",
                    List.of("user_id"),
                    List.of(),
                    IndexType.AGGREGATION));

            final var result = planner().filterExisting(Set.of(family), existing);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("partially covered families survive")
        void partiallyCoveredFamiliesSurvive() {
            final var covered = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("status")
                    .build();
            final var uncovered = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("region")
                    .build();
            final var existing = List.of(ConcreteIndex.bTree(
                    "orders",
                    List.of("status"),
                    List.of(),
                    IndexType.AGGREGATION));

            final var result = planner().filterExisting(Set.of(covered, uncovered), existing);

            assertEquals(1, result.size());
            assertEquals(Set.of("region"), result.iterator().next().flex());
        }
    }

    @Nested
    @DisplayName("IndexPlanner.minimize")
    class PlannerMinimizeTests {

        @Test
        @DisplayName("merges families with same indexed columns but different included")
        void mergesFamiliesWithSameIndexedColumns() {
            final var a = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("user_id")
                    .last("created_at")
                    .addIncluded("amount")
                    .build();
            final var b = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("user_id")
                    .last("created_at")
                    .addIncluded("tax")
                    .build();

            final var result = planner().minimize(Set.of(a, b));

            assertEquals(1, result.size());
            final var merged = result.iterator().next();
            assertEquals(Set.of("amount", "tax"), merged.included());
        }

        @Test
        @DisplayName("does not merge families with different last columns")
        void doesNotMergeWithDifferentLast() {
            final var a = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("user_id")
                    .last("created_at")
                    .build();
            final var b = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("user_id")
                    .last("amount")
                    .build();

            final var result = planner().minimize(Set.of(a, b));

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("merges families with matching fixed prefix and matching remainder")
        void mergesWithMatchingFixedPrefix() {
            final var a = IndexFamily.builder()
                    .tableName("orders")
                    .addFixed("tenant_id")
                    .addFlex("status")
                    .addIncluded("amount")
                    .build();
            final var b = IndexFamily.builder()
                    .tableName("orders")
                    .addFixed("tenant_id")
                    .addFlex("status")
                    .addIncluded("tax")
                    .build();

            final var result = planner().minimize(Set.of(a, b));

            assertEquals(1, result.size());
            final var merged = result.iterator().next();
            assertEquals(List.of("tenant_id"), merged.fixed());
            assertEquals(Set.of("amount", "tax"), merged.included());
        }
    }

    @Nested
    @DisplayName("IndexPlanner.projectToConcrete")
    class PlannerProjectToConcreteTests {

        @Test
        @DisplayName("projects flex to sorted indexed, last appended")
        void projectsFlexToSortedIndexed() {
            final var family = IndexFamily.builder()
                    .tableName("orders")
                    .addFlex("status")
                    .addFlex("region")
                    .last("created_at")
                    .addIncluded("amount")
                    .addIncluded("tax")
                    .build();

            final var result = planner().projectToConcrete(Set.of(family));

            assertEquals(1, result.size());
            final var idx = result.get(0);
            assertAll(
                    () -> assertEquals("orders", idx.tableName()),
                    () -> assertEquals(List.of("region", "status", "created_at"), idx.indexed()),
                    () -> assertEquals(List.of("amount", "tax"), idx.included()),
                    () -> assertEquals(IndexType.AGGREGATION, idx.type()));
        }

        @Test
        @DisplayName("projects functional type when functional columns present")
        void projectsFunctionalType() {
            final var family = IndexFamily.builder()
                    .tableName("users")
                    .addFlex("status")
                    .addFunctional("email")
                    .build();

            final var result = planner().projectToConcrete(Set.of(family));

            assertEquals(1, result.size());
            assertEquals(IndexType.FUNCTIONAL, result.get(0).type());
        }

        @Test
        @DisplayName("generates name with table and columns prefix")
        void generatesNameWithTableAndColumns() {
            final var idx = ConcreteIndex.bTree(
                    "orders",
                    List.of("status", "created_at"),
                    List.of(),
                    IndexType.AGGREGATION);

            final var name = IndexPlanner.generateName(idx);

            assertTrue(name.startsWith("idx_orders_status_created_at_"));
            assertTrue(name.length() <= 63);
        }
    }

    // ── Real-World Use-Case Tests (end-to-end) ───────────────────────

    @Nested
    @DisplayName("Real-world use cases (end-to-end)")
    class RealWorldUseCaseTests {

        @Test
        @DisplayName("UC-1: E-commerce order listing with eq + range")
        void ecommerceOrderListing() {
            final var root = aggregator("orders", "amount",
                    filterNode("=", field("customer_id"), constant(42)),
                    filterNode("=", field("status"), constant("shipped")),
                    filterNode(">", field("created_at"), constant("2024-06-01")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertAll(
                    () -> assertEquals(1, qfs.size()),
                    () -> assertEquals(Set.of("customer_id", "status"), qf.eqConditions()),
                    () -> assertEquals(Set.of("created_at"), qf.ineqConditions()));

            final var planned = planner().planIndexFamilies(qf);
            assertEquals(1, planned.size());
            final var family = planned.iterator().next();

            assertAll(
                    () -> assertEquals(Set.of("customer_id", "status"), family.flex()),
                    () -> assertEquals("created_at", family.last()),
                    () -> assertTrue(family.functional().isEmpty()));

            final var concretes = planner().projectToConcrete(planned);
            assertEquals(1, concretes.size());
            final var ci = concretes.get(0);
            assertEquals(3, ci.indexed().size());
            assertEquals(IndexType.AGGREGATION, ci.type());
        }

        @Test
        @DisplayName("UC-2: Multi-tenant analytics with BETWEEN")
        void multiTenantAnalytics() {
            final var root = aggregator("events", "count",
                    filterNode("=", field("tenant_id"), constant("acme")),
                    filterNode("=", field("event_type"), constant("purchase")),
                    filterNode(">=", field("occurred_at"), constant("2024-01-01")),
                    filterNode("<=", field("occurred_at"), constant("2024-03-31")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertAll(
                    () -> assertEquals(Set.of("event_type", "tenant_id"), qf.eqConditions()),
                    () -> assertEquals(Set.of("occurred_at"), qf.ineqConditions()));

            final var families = planner().planIndexFamilies(qf);
            assertEquals(1, families.size());

            final var concretes = planner().projectToConcrete(families);
            assertEquals(1, concretes.size());
            assertAll(
                    () -> assertEquals(3, concretes.get(0).indexed().size()),
                    () -> assertTrue(concretes.get(0).indexed().contains("event_type")),
                    () -> assertTrue(concretes.get(0).indexed().contains("tenant_id")),
                    () -> assertEquals("occurred_at", concretes.get(0).indexed().get(2)));
        }

        @Test
        @DisplayName("UC-3: Case-insensitive auth lookup (functional index)")
        void caseInsensitiveAuthLookup() {
            final var root = aggregator("users", "logins",
                    filterNode("=", transform("upper", field("email")), constant("USER@EXAMPLE.COM")),
                    filterNode("=", field("status"), constant("active")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertAll(
                    () -> assertEquals(Set.of("email", "status"), qf.eqConditions()),
                    () -> assertEquals(Set.of("email"), qf.functionalColumns()));

            final var families = planner().planIndexFamilies(qf);
            assertEquals(1, families.size());

            final var concretes = planner().projectToConcrete(families);
            assertEquals(1, concretes.size());
            assertEquals(IndexType.FUNCTIONAL, concretes.get(0).type());
        }

        @Test
        @DisplayName("UC-4: Existing composite index covers new query (filtered out)")
        void existingIndexCoversNewQuery() {
            final var root = aggregator("orders", "amount",
                    filterNode("=", field("customer_id"), constant(42)),
                    filterNode("=", field("status"), constant("shipped")),
                    filterNode(">", field("created_at"), constant("2024-06-01")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var families = planner().planIndexFamilies(qfs.iterator().next());

            final var existing = List.of(ConcreteIndex.bTree(
                    "orders",
                    List.of("customer_id", "status", "created_at"),
                    List.of("amount"),
                    IndexType.AGGREGATION));

            final var afterFilter = planner().filterExisting(families, existing);

            assertTrue(afterFilter.isEmpty());
        }

        @Test
        @DisplayName("UC-5: Partial coverage survives when index is missing a column")
        void partialCoverageSurvives() {
            final var root = aggregator("orders", "amount",
                    filterNode("=", field("customer_id"), constant(42)),
                    filterNode("=", field("category"), constant("food")),
                    filterNode(">", field("created_at"), constant("2024-06-01")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var families = planner().planIndexFamilies(qfs.iterator().next());

            final var existing = List.of(ConcreteIndex.bTree(
                    "orders",
                    List.of("customer_id", "created_at"),
                    List.of(),
                    IndexType.AGGREGATION));

            final var afterFilter = planner().filterExisting(families, existing);

            assertEquals(1, afterFilter.size());
        }

        @Test
        @DisplayName("UC-6: Multiple range conditions generate separate index families")
        void multipleRangeConditions() {
            final var root = aggregator("products", "count",
                    filterNode("=", field("category"), constant("electronics")),
                    filterNode(">=", field("price"), constant(100)),
                    filterNode("<=", field("price"), constant(500)),
                    filterNode(">=", field("rating"), constant(4)));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertEquals(Set.of("category"), qf.eqConditions());
            assertEquals(Set.of("price", "rating"), qf.ineqConditions());

            final var families = planner().planIndexFamilies(qf);

            assertEquals(2, families.size());
            for (final var f : families) {
                assertEquals(Set.of("category"), f.flex());
            }

            final var concretes = planner().projectToConcrete(families);
            assertEquals(2, concretes.size());
        }

        @Test
        @DisplayName("UC-7: No filters at all yields empty output")
        void noFiltersYieldsEmpty() {
            final var root = aggregator("logs", "count");

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertTrue(qf.eqConditions().isEmpty());
            assertTrue(qf.ineqConditions().isEmpty());

            final var families = planner().planIndexFamilies(qf);

            assertTrue(families.isEmpty());

            final var concretes = planner().projectToConcrete(families);
            assertTrue(concretes.isEmpty());
        }

        @Test
        @DisplayName("UC-8: Case-insensitive search + range (mixed functional + plain range)")
        void functionalPlusRange() {
            final var root = aggregator("users", "count",
                    filterNode("=", transform("upper", field("last_name")), constant("SOK")),
                    filterNode(">", field("created_at"), constant("2024-01-01")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertAll(
                    () -> assertTrue(qf.eqConditions().contains("last_name")),
                    () -> assertTrue(qf.ineqConditions().contains("created_at")),
                    () -> assertTrue(qf.functionalColumns().contains("last_name")));

            final var families = planner().planIndexFamilies(qf);
            assertEquals(1, families.size());

            final var concretes = planner().projectToConcrete(families);
            assertEquals(1, concretes.size());
            assertEquals(IndexType.FUNCTIONAL, concretes.get(0).type());
        }

        @Test
        @DisplayName("UC-9: Multiple range columns produce multiple families after split")
        void multipleRangeColumnsProduceMultipleFamilies() {
            final var root = aggregator("transactions", "amount",
                    filterNode("=", field("user_id"), constant(42)),
                    filterNode("=", field("status"), constant("completed")),
                    filterNode(">", field("created_at"), constant("2024-06-01")),
                    filterNode(">", field("amount"), constant(100)));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var families = planner().planIndexFamilies(qfs.iterator().next());

            assertEquals(2, families.size());

            final var minimized = planner().minimize(families);
            assertEquals(2, minimized.size());
        }

        @Test
        @DisplayName("UC-10: Duplicate aggregations are deduplicated")
        void duplicateAggregationsDeduplicated() {
            final var ruleA = aggregator("orders", "revenue",
                    filterNode("=", field("customer_id"), constant(42)),
                    filterNode(">", field("created_at"), constant("2024-06-01")));
            final var ruleB = aggregator("orders", "revenue",
                    filterNode("=", field("customer_id"), constant(42)),
                    filterNode(">", field("created_at"), constant("2024-06-01")));

            final var root = Node.function(
                    FunctionId.of("rule_set"),
                    ruleA, ruleB);

            final var qfs = analyzer().extractQueryFamilies(root);

            assertEquals(1, qfs.size());
        }

        @Test
        @DisplayName("UC-11: Chained payload access resolves recursively")
        void chainedPayloadAccess() {
            final var innerPayload = Node.function(
                    FunctionId.of("payload"),
                    List.of(),
                    Map.of("fieldName", field("nested_field")));
            final var outerPayload = Node.function(
                    FunctionId.of("payload"),
                    List.of(),
                    Map.of("fieldName", innerPayload));

            final var root = aggregator("events", "value",
                    filterNode("=", outerPayload, constant("expected")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertEquals(Set.of("nested_field"), qf.eqConditions());
        }

        @Test
        @DisplayName("UC-12: Payload field access filter")
        void payloadFieldAccessFilter() {
            final var root = aggregator("orders", "total",
                    filterNode("=", payload("status"), constant("active")));

            final var qfs = analyzer().extractQueryFamilies(root);

            assertEquals(1, qfs.size());
            final var qf = qfs.iterator().next();
            assertEquals(Set.of("status"), qf.eqConditions());
            assertTrue(qf.functionalColumns().isEmpty());
        }

        @Test
        @DisplayName("UC-13: Nested aggregators at different AST depths")
        void nestedAggregators() {
            final var ordersAgg = aggregator("orders", "total",
                    filterNode("=", field("status"), constant("active")));
            final var invoicesAgg = aggregator("invoices", "amount",
                    filterNode(">", field("total"), constant(100)));

            final var root = Node.function(
                    FunctionId.of("concat"), ordersAgg, invoicesAgg);

            final var qfs = analyzer().extractQueryFamilies(root);

            assertEquals(2, qfs.size());
            final var tableNames = qfs.stream()
                    .map(AggregateQueryFamily::tableName)
                    .sorted()
                    .toList();
            assertEquals(List.of("invoices", "orders"), tableNames);

            final var allConcretes = qfs.stream()
                    .flatMap(qf -> planner().projectToConcrete(
                            planner().minimize(planner().planIndexFamilies(qf))).stream())
                    .toList();

            assertEquals(2, allConcretes.size());
        }

        @Test
        @DisplayName("UC-14: Mixed filter types with eq, ineq, IsInList, fuzzy match")
        void mixedFilterTypes() {
            final var root = aggregator("tickets", "count",
                    filterNode("=", field("assignee_id"), constant(42)),
                    filterNode("IsInList", field("priority"), constant("high")),
                    filterNode("FuzzyMatch", field("title"), constant("urgent")),
                    filterNode(">", field("created_at"), constant("2024-06-01")));

            final var qfs = analyzer().extractQueryFamilies(root);

            assertEquals(1, qfs.size());
            final var qf = qfs.iterator().next();
            assertAll(
                    () -> assertEquals(Set.of("assignee_id"), qf.eqConditions()),
                    () -> assertEquals(Set.of("created_at"), qf.ineqConditions()),
                    () -> assertTrue(qf.otherConditions().contains("priority")),
                    () -> assertTrue(qf.otherConditions().contains("title")));

            final var families = planner().planIndexFamilies(qf);
            assertEquals(1, families.size());
            final var family = families.iterator().next();
            assertAll(
                    () -> assertEquals(Set.of("assignee_id"), family.flex()),
                    () -> assertEquals("created_at", family.last()),
                    () -> assertTrue(family.included().contains("priority")),
                    () -> assertTrue(family.included().contains("title")));
        }

        @Test
        @DisplayName("UC-15: No indexable conditions yields empty output")
        void noIndexableConditions() {
            final var root = aggregator("orders", "amount",
                    filterNode("IsInList", field("status"), constant("pending")),
                    filterNode("FuzzyMatch", field("description"), constant("urgent")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertTrue(qf.eqConditions().isEmpty());
            assertTrue(qf.ineqConditions().isEmpty());

            final var families = planner().planIndexFamilies(qf);
            assertTrue(families.isEmpty());
        }

        @Test
        @DisplayName("UC-16: Value side expression does not affect index detection")
        void valueSideExpressionDoesNotAffect() {
            final var root = aggregator("orders", "amount",
                    filterNode(">", field("total"), payload("threshold")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertEquals(Set.of("total"), qf.ineqConditions());

            final var families = planner().planIndexFamilies(qf);
            assertEquals(1, families.size());

            final var concretes = planner().projectToConcrete(families);
            assertEquals(1, concretes.size());
            assertEquals(List.of("total"), concretes.get(0).indexed());
        }

        @Test
        @DisplayName("UC-17: JSONB containment without B-tree conditions suggests GIN index")
        void jsonbContainmentSuggestsGin() {
            final var root = aggregator("events", "count",
                    filterNode("jsonb_contains", field("data"), constant("{\"status\":\"active\"}")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertAll(
                    () -> assertTrue(qf.eqConditions().isEmpty()),
                    () -> assertTrue(qf.ineqConditions().isEmpty()),
                    () -> assertTrue(qf.hasGinConditions()),
                    () -> assertTrue(qf.ginConditions().contains("data")));

            final var ginIndexes = planner().planGinIndexes(qf);
            assertEquals(1, ginIndexes.size());
            final var gin = ginIndexes.iterator().next();
            assertAll(
                    () -> assertEquals("events", gin.tableName()),
                    () -> assertEquals(List.of("data"), gin.indexed()),
                    () -> assertTrue(gin.isGin()),
                    () -> assertEquals("jsonb_ops", gin.operatorClass()));
        }

        @Test
        @DisplayName("UC-18: B-tree + GIN mixed conditions prioritize B-tree over GIN")
        void btreeAndGinMixedPrioritizesBtree() {
            final var root = aggregator("events", "count",
                    filterNode("=", field("status"), constant("active")),
                    filterNode("jsonb_contains", field("data"), constant("{\"key\":\"val\"}")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertAll(
                    () -> assertEquals(Set.of("status"), qf.eqConditions()),
                    () -> assertTrue(qf.ginConditions().contains("data")));

            assertTrue(qf.hasIndexableConditions());
            final var ginIndexes = planner().planGinIndexes(qf);
            assertTrue(ginIndexes.isEmpty());

            final var families = planner().planIndexFamilies(qf);
            assertEquals(1, families.size());
        }

        @Test
        @DisplayName("UC-19: JSONB key existence suggests GIN index")
        void jsonbKeyExistsSuggestsGin() {
            final var root = aggregator("profiles", "count",
                    filterNode("jsonb_key_exists", field("metadata"), constant("role")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertTrue(qf.ginConditions().contains("metadata"));

            final var ginIndexes = planner().planGinIndexes(qf);
            assertEquals(1, ginIndexes.size());
            assertAll(
                    () -> assertTrue(ginIndexes.iterator().next().isGin()),
                    () -> assertEquals(List.of("metadata"), ginIndexes.iterator().next().indexed()));
        }

        @Test
        @DisplayName("UC-20: Full-text search suggests GIN index")
        void fulltextSearchSuggestsGin() {
            final var root = aggregator("documents", "count",
                    filterNode("fulltext_match", field("search_vector"), constant("query")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertTrue(qf.ginConditions().contains("search_vector"));

            final var ginIndexes = planner().planGinIndexes(qf);
            assertEquals(1, ginIndexes.size());
            assertTrue(ginIndexes.iterator().next().isGin());
        }

        @Test
        @DisplayName("UC-21: Existing GIN index covers new GIN proposal")
        void existingGinCoversNewProposal() {
            final var qf = AggregateQueryFamily.builder()
                    .tableName("events")
                    .fieldName("count")
                    .addGin("data")
                    .build();

            final var proposals = planner().planGinIndexes(qf);
            assertEquals(1, proposals.size());

            final var existingGin = List.of(
                    ConcreteIndex.gin("events", "data", "jsonb_ops"));

            final var afterFilter = planner().filterExistingGin(proposals, existingGin);

            assertTrue(afterFilter.isEmpty());
        }

        @Test
        @DisplayName("UC-22: GIN-only with multiple JSONB columns suggests multiple GIN indexes")
        void multipleGinColumns() {
            final var root = aggregator("profiles", "count",
                    filterNode("jsonb_contains", field("preferences"), constant("{\"theme\":\"dark\"}")),
                    filterNode("jsonb_key_exists", field("metadata"), constant("role")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertTrue(qf.hasGinConditions());
            assertEquals(Set.of("preferences", "metadata"), qf.ginConditions());

            final var ginIndexes = planner().planGinIndexes(qf);
            assertEquals(2, ginIndexes.size());
        }

        @Test
        @DisplayName("UC-23: Array containment suggests GIN array_ops index")
        void arrayContainmentSuggestsGin() {
            final var root = aggregator("products", "count",
                    filterNode("array_contains", field("tags"), constant("[\"sale\"]")));

            final var qfs = analyzer().extractQueryFamilies(root);
            final var qf = qfs.iterator().next();

            assertTrue(qf.ginConditions().contains("tags"));

            final var ginIndexes = planner().planGinIndexes(qf);
            assertEquals(1, ginIndexes.size());
        }

        @Test
        @DisplayName("UC-24: GIN index with different column not covered by existing")
        void ginDifferentColumnNotCovered() {
            final var qf = AggregateQueryFamily.builder()
                    .tableName("events")
                    .fieldName("count")
                    .addGin("data")
                    .build();

            final var proposals = planner().planGinIndexes(qf);
            final var existingGin = List.of(
                    ConcreteIndex.gin("events", "other_col", "jsonb_ops"));

            final var afterFilter = planner().filterExistingGin(proposals, existingGin);

            assertEquals(1, afterFilter.size());
        }

        @Test
        @DisplayName("UC-25: GIN index with matching column but different table not covered")
        void ginDifferentTableNotCovered() {
            final var qf = AggregateQueryFamily.builder()
                    .tableName("events")
                    .fieldName("count")
                    .addGin("data")
                    .build();

            final var proposals = planner().planGinIndexes(qf);
            final var existingGin = List.of(
                    ConcreteIndex.gin("other_table", "data", "jsonb_ops"));

            final var afterFilter = planner().filterExistingGin(proposals, existingGin);

            assertEquals(1, afterFilter.size());
        }

        @Test
        @DisplayName("UC-26: GIN generateName produces correct prefix")
        void ginGenerateName() {
            final var idx = ConcreteIndex.gin("events", "data", "jsonb_ops");

            final var name = IndexPlanner.generateName(idx);

            assertTrue(name.startsWith("idx_events_data_gin_"));
            assertTrue(name.length() <= 63);
        }
    }
}
