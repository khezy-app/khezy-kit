package io.github.khezyapp.dhttp.expr.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.expr.EvaluationScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JexlExpressionEvaluatorTest {

    private final JexlExpressionEvaluator evaluator = new JexlExpressionEvaluator();

    record Sample(String name, int count) {
    }

    @Test
    @DisplayName("Should return a bound parameter as its runtime type")
    void evaluatesParameter() {
        final var scope = EvaluationScope.create()
                .bind(EvaluationScope.PARAMETER, Map.of("count", 5));

        final var result = evaluator.evaluate("={{ $parameter.count }}", scope, Integer.class);

        assertEquals(5, result);
    }

    @Test
    @DisplayName("Should return the evaluated object, not a string, for a single expression")
    void returnsRuntimeObject() {
        final var scope = EvaluationScope.create()
                .bind(EvaluationScope.PARAMETER, Map.of("ids", List.of(1, 2)));

        final var result = evaluator.evaluate("={{ $parameter.ids }}", scope, Object.class);

        assertEquals(List.of(1, 2), result);
    }

    @Test
    @DisplayName("Should navigate nested structures through the doa namespace")
    void navigatesThroughDoaNamespace() {
        final var response = Map.of("data", Map.of("items", List.of(Map.of("id", "abc"))));
        final var scope = EvaluationScope.create().bind(EvaluationScope.RESPONSE, response);

        final var result = evaluator.evaluate(
                "={{ doa:get($response, \"data.items[0].id\") }}", scope, Object.class);

        assertEquals("abc", result);
    }

    @Test
    @DisplayName("Should render a string template with embedded expressions")
    void interpolatesTemplate() {
        final var scope = EvaluationScope.create().bind("name", "SOK");

        final var result = evaluator.evaluate("= Hello Mr. {{ name }}", scope, String.class);

        assertEquals("Hello Mr. SOK", result);
    }

    @Test
    @DisplayName("Should render several expressions and text into a single string")
    void rendersMultipleExpressions() {
        final var scope = EvaluationScope.create()
                .bind("first", "VISAL")
                .bind("last", "CHEA");

        final var result = evaluator.evaluate("= {{ first }} {{ last }}!", scope, String.class);

        assertEquals("VISAL CHEA!", result);
    }

    @Test
    @DisplayName("Should interpolate a bare template without a leading equals")
    void interpolatesBareTemplate() {
        final var scope = EvaluationScope.create().bind("name", "SOK");

        assertEquals("Hello SOK", evaluator.evaluate("Hello {{name}}", scope, String.class));
    }

    @Test
    @DisplayName("Should return the body as a literal whens no braces are present")
    void passesThroughLiteral() {
        final var scope = EvaluationScope.create();

        assertEquals("plain", evaluator.evaluate("plain", scope, String.class));
        assertEquals("plain", evaluator.evaluate("=plain", scope, String.class));
    }

    @Test
    @DisplayName("Should detect expression markers")
    void detectsExpressions() {
        assertTrue(evaluator.isExpression("={{ $some }}"));
        assertTrue(evaluator.isExpression("= plain"));
        assertFalse(evaluator.isExpression("hello"));
        assertFalse(evaluator.isExpression("Hello {{name}}"));
    }

    @Test
    @DisplayName("Should convert an evaluated value into an arbitrary record type via JSON")
    void convertsToRecordType() {
        final var scope = EvaluationScope.create()
                .bind(EvaluationScope.PARAMETER, Map.of("name", "SOK", "count", 3));

        final var result = evaluator.evaluate("={{ $parameter }}", scope, Sample.class);

        assertEquals(new Sample("SOK", 3), result);
    }

    @Test
    @DisplayName("Should convert a string value into the requested numeric type")
    void convertsStringToNumber() {
        final var scope = EvaluationScope.create().bind("$raw", "42");

        final var result = evaluator.evaluate("={{ $raw }}", scope, Integer.class);

        assertEquals(42, result);
    }

    @Test
    @DisplayName("Should convert an evaluated value into a generic collection via JSON")
    void convertsToGenericCollection() {
        final var scope = EvaluationScope.create()
                .bind(EvaluationScope.PARAMETER, Map.of("ids", List.of(1, 2, 3)));

        final var result = evaluator.evaluate("={{ $parameter.ids }}", scope, List.class);

        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    @DisplayName("Should isolate scope across concurrent evaluations")
    void concurrentIsolation() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            final var futures = new ArrayList<Future<Integer>>();
            for (int i = 0; i < 20; i++) {
                final var idx = i;
                futures.add(pool.submit(() -> {
                    final var scope = EvaluationScope.create()
                            .bind(EvaluationScope.PARAMETER, Map.of("count", idx));
                    return evaluator.evaluate("={{ $parameter.count }}", scope, Integer.class);
                }));
            }
            for (int i = 0; i < futures.size(); i++) {
                assertEquals(i, futures.get(i).get(5, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdown();
        }
    }
}
