package io.github.khezyapp.ast.core.eval;

import io.github.khezyapp.ast.core.function.FunctionRegistry;
import io.github.khezyapp.ast.core.model.CoreFunctions;
import io.github.khezyapp.ast.core.model.Node;
import io.github.khezyapp.ast.core.nullstrategy.NullStrategies;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for {@link AstEvaluator} evaluation engine.
 * <p>
 * Covers constant evaluation, arithmetic, boolean logic with short-circuit,
 * isEmpty, nested expressions, caching, error propagation, payload access,
 * custom function registration, string operations, and optimization controls.
 * </p>
 */
@DisplayName("AstEvaluator")
class AstEvaluatorTest {

    private FunctionRegistry registry;
    private AstEvaluator evaluator;

    @BeforeEach
    void setUp() {
        registry = FunctionRegistry.withBuiltins(NullStrategies.PROPAGATE);
        evaluator = new AstEvaluator();
    }

    private EvaluationContext ctx() {
        return ctxWithBody(Map.of("field", "value"));
    }

    private EvaluationContext ctxWithBody(final Object body) {
        return new EvaluationContext.Builder(registry)
            .body(body)
            .build();
    }

    private EvaluationContext ctxWithCache(final EvaluationCache cache) {
        return new EvaluationContext.Builder(registry)
            .body(Map.of())
            .cache(cache)
            .build();
    }

    @Nested
    @DisplayName("constant evaluation")
    class ConstantEvaluationTests {

        @Test
        @DisplayName("evaluates integer constant")
        void evaluatesIntegerConstant() {
            final var node = Node.constant(42);
            final var result = evaluator.evaluate(node, ctx());

            assertAll(
                () -> assertEquals(42, result.returnValue()),
                () -> assertTrue(result.errors().isEmpty()),
                () -> assertFalse(result.isSkipped()),
                () -> assertFalse(result.isCached())
            );
        }

        @Test
        @DisplayName("evaluates string constant")
        void evaluatesStringConstant() {
            final var node = Node.constant("hello");
            final var result = evaluator.evaluate(node, ctx());

            assertEquals("hello", result.returnValue());
        }

        @Test
        @DisplayName("evaluates null constant")
        void evaluatesNullConstant() {
            final var node = Node.constant(null);
            final var result = evaluator.evaluate(node, ctx());

            assertNull(result.returnValue());
        }

        @Test
        @DisplayName("constant node has CONSTANT function")
        void constantNodeHasConstantFunction() {
            final var node = Node.constant(99);
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(CoreFunctions.CONSTANT, result.function());
        }
    }

    @Nested
    @DisplayName("arithmetic evaluation")
    class ArithmeticEvaluationTests {

        @Test
        @DisplayName("adds two integers")
        void addsTwoIntegers() {
            final var node = Node.function(CoreFunctions.ADD,
                Node.constant(1), Node.constant(2));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(3.0, result.returnValue());
        }

        @Test
        @DisplayName("subtracts two integers")
        void subtractsTwoIntegers() {
            final var node = Node.function(CoreFunctions.SUBTRACT,
                Node.constant(10), Node.constant(3));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(7.0, result.returnValue());
        }

        @Test
        @DisplayName("multiplies two integers")
        void multipliesTwoIntegers() {
            final var node = Node.function(CoreFunctions.MULTIPLY,
                Node.constant(4), Node.constant(5));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(20.0, result.returnValue());
        }

        @Test
        @DisplayName("divides two integers")
        void dividesTwoIntegers() {
            final var node = Node.function(CoreFunctions.DIVIDE,
                Node.constant(10), Node.constant(3));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(10.0 / 3.0, result.returnValue());
        }

        @Test
        @DisplayName("division by zero returns error")
        void divisionByZeroReturnsError() {
            final var node = Node.function(CoreFunctions.DIVIDE,
                Node.constant(5), Node.constant(0));
            final var result = evaluator.evaluate(node, ctx());

            assertAll(
                () -> assertFalse(result.errors().isEmpty()),
                () -> assertTrue(
                    result.errors().get(0).errorCode().code().contains("DIVISION_BY_ZERO"))
            );
        }
    }

    @Nested
    @DisplayName("boolean evaluation")
    class BooleanEvaluationTests {

        @Test
        @DisplayName("AND returns true when all true")
        void andReturnsTrueWhenAllTrue() {
            final var node = Node.function(CoreFunctions.AND,
                Node.constant(true), Node.constant(true));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(true, result.returnValue());
        }

        @Test
        @DisplayName("AND returns false when any false")
        void andReturnsFalseWhenAnyFalse() {
            final var node = Node.function(CoreFunctions.AND,
                Node.constant(true), Node.constant(false), Node.constant(true));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(false, result.returnValue());
        }

        @Test
        @DisplayName("AND short-circuits on first false")
        void andShortCircuitsOnFirstFalse() {
            final var node = Node.function(CoreFunctions.AND,
                Node.constant(false), Node.constant(true));
            final var result = evaluator.evaluate(node, ctx());

            assertAll(
                () -> assertEquals(false, result.returnValue()),
                () -> assertEquals(2, result.children().size()),
                () -> assertFalse(result.children().get(0).isSkipped()),
                () -> assertTrue(result.children().get(1).isSkipped())
            );
        }

        @Test
        @DisplayName("OR returns true when any true")
        void orReturnsTrueWhenAnyTrue() {
            final var node = Node.function(CoreFunctions.OR,
                Node.constant(false), Node.constant(true));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(true, result.returnValue());
        }

        @Test
        @DisplayName("OR returns false when all false")
        void orReturnsFalseWhenAllFalse() {
            final var node = Node.function(CoreFunctions.OR,
                Node.constant(false), Node.constant(false));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(false, result.returnValue());
        }

        @Test
        @DisplayName("OR short-circuits on first true")
        void orShortCircuitsOnFirstTrue() {
            final var node = Node.function(CoreFunctions.OR,
                Node.constant(true), Node.constant(false));
            final var result = evaluator.evaluate(node, ctx());

            assertAll(
                () -> assertEquals(true, result.returnValue()),
                () -> assertEquals(2, result.children().size()),
                () -> assertFalse(result.children().get(0).isSkipped()),
                () -> assertTrue(result.children().get(1).isSkipped())
            );
        }

        @Test
        @DisplayName("NOT negates true to false")
        void notNegatesTrueToFalse() {
            final var node = Node.function(CoreFunctions.NOT, Node.constant(true));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(false, result.returnValue());
        }

        @Test
        @DisplayName("NOT negates false to true")
        void notNegatesFalseToTrue() {
            final var node = Node.function(CoreFunctions.NOT, Node.constant(false));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(true, result.returnValue());
        }
    }

    @Nested
    @DisplayName("isEmpty evaluation")
    class IsEmptyEvaluationTests {

        @Test
        @DisplayName("null is empty")
        void nullIsEmpty() {
            final var node = Node.function(CoreFunctions.IS_EMPTY, Node.constant(null));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(true, result.returnValue());
        }

        @Test
        @DisplayName("empty string is empty")
        void emptyStringIsEmpty() {
            final var node = Node.function(CoreFunctions.IS_EMPTY, Node.constant(""));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(true, result.returnValue());
        }

        @Test
        @DisplayName("non-empty string is not empty")
        void nonEmptyStringIsNotEmpty() {
            final var node = Node.function(CoreFunctions.IS_EMPTY, Node.constant("foo"));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(false, result.returnValue());
        }

        @Test
        @DisplayName("empty list is empty")
        void emptyListIsEmpty() {
            final var node = Node.function(CoreFunctions.IS_EMPTY, Node.constant(List.of()));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(true, result.returnValue());
        }
    }

    @Nested
    @DisplayName("pipeline integration")
    class PipelineIntegrationTests {

        @Test
        @DisplayName("nested arithmetic evaluates correctly")
        void nestedArithmeticEvaluatesCorrectly() {
            // (1 + 2) * (10 - 4)
            final var add = Node.function(CoreFunctions.ADD,
                Node.constant(1), Node.constant(2));
            final var sub = Node.function(CoreFunctions.SUBTRACT,
                Node.constant(10), Node.constant(4));
            final var node = Node.function(CoreFunctions.MULTIPLY, add, sub);
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(18.0, result.returnValue());
        }

        @Test
        @DisplayName("all children present in result tree")
        void allChildrenPresentInResultTree() {
            final var child1 = Node.constant(1);
            final var child2 = Node.constant(2);
            final var node = Node.function(CoreFunctions.ADD, child1, child2);
            final var result = evaluator.evaluate(node, ctx());

            assertAll(
                () -> assertEquals(2, result.children().size()),
                () -> assertEquals(1, result.children().get(0).returnValue()),
                () -> assertEquals(2, result.children().get(1).returnValue())
            );
        }
    }

    @Nested
    @DisplayName("caching")
    class CachingTests {

        @Test
        @DisplayName("cache stores and retrieves evaluation")
        void cacheStoresAndRetrievesEvaluation() {
            final var cache = new DefaultEvaluationCache();
            final var ctx = ctxWithCache(cache);
            final var node = Node.constant(42);

            final var first = evaluator.evaluate(node, ctx);
            assertFalse(first.isCached());

            final var second = evaluator.evaluate(node, ctx);
            assertAll(
                () -> assertEquals(first.returnValue(), second.returnValue()),
                () -> assertTrue(second.isCached())
            );
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("unknown function throws")
        void unknownFunctionThrows() {
            final var unknown = Node.function(
                io.github.khezyapp.ast.core.model.FunctionId.of("unknown:fn"));
            assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(unknown, ctx()));
        }

        @Test
        @DisplayName("child error propagates to parent")
        void childErrorPropagatesToParent() {
            // AND(true, 1/0) — the division error should propagate
            final var divByZero = Node.function(CoreFunctions.DIVIDE,
                Node.constant(1), Node.constant(0));
            final var node = Node.function(CoreFunctions.AND,
                Node.constant(true), divByZero);
            final var result = evaluator.evaluate(node, ctx());

            assertAll(
                () -> assertFalse(result.errors().isEmpty()),
                () -> assertTrue(result.flattenErrors().stream()
                    .anyMatch(e -> e.errorCode().code().contains("DIVISION_BY_ZERO")))
            );
        }
    }

    @Nested
    @DisplayName("result tree navigation")
    class ResultTreeNavigationTests {

        @Test
        @DisplayName("flatten returns all nodes in tree")
        void flattenReturnsAllNodesInTree() {
            final var node = Node.function(CoreFunctions.ADD,
                Node.constant(1),
                Node.function(CoreFunctions.MULTIPLY,
                    Node.constant(2), Node.constant(3)));
            final var result = evaluator.evaluate(node, ctx());

            final var flat = result.flatten();
            assertEquals(5, flat.size());
        }

        @Test
        @DisplayName("flattenErrors aggregates nested errors")
        void flattenErrorsAggregatesNestedErrors() {
            final var divByZero = Node.function(CoreFunctions.DIVIDE,
                Node.constant(1), Node.constant(0));
            final var node = Node.function(CoreFunctions.ADD,
                Node.constant(1), divByZero);
            final var result = evaluator.evaluate(node, ctx());

            final var errors = result.flattenErrors();
            assertFalse(errors.isEmpty());
        }
    }

    @Nested
    @DisplayName("payload evaluation")
    class PayloadEvaluationTests {

        @Test
        @DisplayName("reads value from payload body")
        void readsValueFromPayloadBody() {
            final var body = Map.of("name", "alice", "age", 30);
            final var ctx = ctxWithBody(body);

            final var node = Node.function(CoreFunctions.PAYLOAD,
                List.of(), Map.of("fieldName", Node.constant("name")));
            final var result = evaluator.evaluate(node, ctx);

            assertEquals("alice", result.returnValue());
        }

        @Test
        @DisplayName("returns error for missing field")
        void returnsErrorForMissingField() {
            final var body = Map.of("name", "alice");
            final var ctx = ctxWithBody(body);

            final var node = Node.function(CoreFunctions.PAYLOAD,
                List.of(), Map.of("fieldName", Node.constant("missing")));
            final var result = evaluator.evaluate(node, ctx);

            assertFalse(result.errors().isEmpty());
        }
    }

    @Nested
    @DisplayName("custom function")
    class CustomFunctionTests {

        @Test
        @DisplayName("evaluates custom registered function")
        void evaluatesCustomRegisteredFunction() {
            final var customId = io.github.khezyapp.ast.core.model.FunctionId.of("custom:double");
            final var registry2 = FunctionRegistry.empty(NullStrategies.PROPAGATE);
            registry2.register(io.github.khezyapp.ast.core.function.FunctionDefinition.builder()
                .functionId(customId)
                .evaluator((ctx, args) ->
                    io.github.khezyapp.ast.core.result.EvaluationOutcome.success(
                        ((Number) args.positional().get(0)).doubleValue() * 2))
                .positionalParam(io.github.khezyapp.ast.core.model.ParamSpec.required("x",
                    io.github.khezyapp.ast.core.model.ParamType.ANY))
                .build());

            final var ctx = new EvaluationContext.Builder(registry2)
                .body(Map.of())
                .build();
            final var node = Node.function(customId, Node.constant(5));
            final var result = new AstEvaluator().evaluate(node, ctx);

            assertEquals(10.0, result.returnValue());
        }
    }

    @Nested
    @DisplayName("string evaluation")
    class StringEvaluationTests {

        @Test
        @DisplayName("stringContains finds substring")
        void stringContainsFindsSubstring() {
            final var node = Node.function(CoreFunctions.STRING_CONTAINS,
                List.of(Node.constant("hello world")),
                Map.of("substring", Node.constant("world")));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(true, result.returnValue());
        }

        @Test
        @DisplayName("stringContains returns false on mismatch")
        void stringContainsReturnsFalseOnMismatch() {
            final var node = Node.function(CoreFunctions.STRING_CONTAINS,
                List.of(Node.constant("hello world")),
                Map.of("substring", Node.constant("xyz")));
            final var result = evaluator.evaluate(node, ctx());

            assertEquals(false, result.returnValue());
        }
    }

    @Nested
    @DisplayName("optimization control")
    class OptimizationControlTests {

        @Test
        @DisplayName("withoutOptimizations preserves caching")
        void withoutOptimizationsPreservesCaching() {
            final var cache = new DefaultEvaluationCache();
            final var ctx = new EvaluationContext.Builder(registry)
                .body(Map.of())
                .cache(cache)
                .build()
                .withoutOptimizations();
            final var node = Node.constant(42);

            final var first = evaluator.evaluate(node, ctx);
            assertFalse(first.isCached());

            final var second = evaluator.evaluate(node, ctx);
            assertTrue(second.isCached());
        }
    }
}
