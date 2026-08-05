package io.github.khezyapp.ast.core.builtin;

import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.function.FunctionRegistry;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.nullstrategy.NullStrategies;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for {@link PayloadEvaluator}.
 * <p>
 * Covers single-level and nested dot-path field access, null handling,
 * missing field errors, and error handling for non-Map bodies.
 * </p>
 */
@DisplayName("PayloadEvaluator")
class PayloadEvaluatorTest {

    private FunctionRegistry registry;
    private PayloadEvaluator evaluator;

    @BeforeEach
    void setUp() {
        registry = FunctionRegistry.withBuiltins(NullStrategies.PROPAGATE);
        evaluator = new PayloadEvaluator();
    }

    private EvaluationContext ctxWithBody(final Object body) {
        return new EvaluationContext.Builder(registry).body(body).build();
    }

    private EvaluationOutcome evaluate(final String fieldName,
                                       final Object body) {
        final var args = new Arguments(List.of(), Map.of("fieldName", fieldName));
        return evaluator.evaluate(ctxWithBody(body), args);
    }

    @Nested
    @DisplayName("single-level access")
    class SingleLevelTests {

        @Test
        @DisplayName("returns value for existing key")
        void returnsValueForExistingKey() {
            final var result = evaluate("name", Map.of("name", "Khezy"));

            assertTrue(result.errors().isEmpty());
            assertEquals("Khezy", result.value());
        }

        @Test
        @DisplayName("returns null when key maps to null")
        void returnsNullWhenKeyIsNull() {
            final var body = new HashMap<String, Object>();
            body.put("name", null);
            final var result = evaluate("name", body);

            assertTrue(result.errors().isEmpty());
            assertNull(result.value());
        }

        @Test
        @DisplayName("returns MISSING_FIELD when key does not exist")
        void missingFieldWhenKeyNotFound() {
            final var result = evaluate("missing", Map.of("name", "Khezy"));

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.MISSING_FIELD.code(),
                    result.errors().get(0).errorCode().code());
        }
    }

    @Nested
    @DisplayName("nested access with dot path")
    class NestedAccessTests {

        @Test
        @DisplayName("two-level nested access")
        void twoLevelNestedAccess() {
            final var body = Map.of("address", Map.of("city", "PP"));
            final var result = evaluate("address.city", body);

            assertTrue(result.errors().isEmpty());
            assertEquals("PP", result.value());
        }

        @Test
        @DisplayName("three-level deep nested access")
        void threeLevelDeepAccess() {
            final var body = Map.of("a", Map.of("b", Map.of("c", "deep")));
            final var result = evaluate("a.b.c", body);

            assertTrue(result.errors().isEmpty());
            assertEquals("deep", result.value());
        }

        @Test
        @DisplayName("returns null when nested value is null")
        void nullNestedValue() {
            final var inner = new HashMap<String, Object>();
            inner.put("city", null);
            final var body = Map.of("address", inner);
            final var result = evaluate("address.city", body);

            assertTrue(result.errors().isEmpty());
            assertNull(result.value());
        }

        @Test
        @DisplayName("returns null when intermediate value is null")
        void nullIntermediateValue() {
            final var body = new HashMap<String, Object>();
            body.put("address", null);
            final var result = evaluate("address.city", body);

            assertTrue(result.errors().isEmpty());
            assertNull(result.value());
        }

        @Test
        @DisplayName("returns MISSING_FIELD when intermediate key not found")
        void missingFieldAtIntermediateKey() {
            final var body = Map.of("address", Map.of("city", "PP"));
            final var result = evaluate("address.missing", body);

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.MISSING_FIELD.code(),
                    result.errors().get(0).errorCode().code());
        }

        @Test
        @DisplayName("returns error when intermediate value is not a Map")
        void errorWhenIntermediateNotMap() {
            final var body = Map.of("address", "not-a-map");
            final var result = evaluate("address.city", body);

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.RUNTIME_ERROR.code(),
                    result.errors().get(0).errorCode().code());
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("returns MISSING_NAMED_ARG when fieldName is null")
        void missingFieldNameArg() {
            final var body = Map.of("name", "Khezy");
            final var args = new Arguments(List.of(), Map.of());
            final var result = evaluator.evaluate(ctxWithBody(body), args);

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.MISSING_NAMED_ARG.code(),
                    result.errors().get(0).errorCode().code());
        }

        @Test
        @DisplayName("returns error when root body is not a Map")
        void rootBodyNotMap() {
            final var result = evaluate("name", "not-a-map");

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.RUNTIME_ERROR.code(),
                    result.errors().get(0).errorCode().code());
        }
    }
}
