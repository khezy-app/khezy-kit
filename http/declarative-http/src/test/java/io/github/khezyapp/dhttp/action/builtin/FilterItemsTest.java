package io.github.khezyapp.dhttp.action.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FilterItemsTest {

    private final JexlExpressionEvaluator evaluator = new JexlExpressionEvaluator();

    @Test
    @DisplayName("Should keep only the records whose expression passes")
    void keepsMatchingRecords() {
        final var action = new FilterItems(
                new Expression("= {{ $item.active == true }}"), evaluator);
        final var records = List.of(
                OutputRecord.ofJson(Map.of("name", "SOK", "active", true)),
                OutputRecord.ofJson(Map.of("name", "VISAL", "active", false)));

        final var kept = action.apply(records, HttpResult.of(200, "{}"));

        assertEquals(1, kept.size());
        assertEquals("SOK", kept.get(0).json().get("name"));
    }

    @Test
    @DisplayName("Should treat a literal true as passing every record")
    void literalTrueKeepsEverything() {
        final var action = new FilterItems(new Expression("true"), evaluator);

        final var kept = action.apply(
                List.of(OutputRecord.ofJson(Map.of("name", "SOK")),
                        OutputRecord.ofJson(Map.of("name", "VISAL"))),
                HttpResult.of(200, "{}"));

        assertEquals(2, kept.size());
    }

    @Test
    @DisplayName("Should drop every record for a literal false")
    void literalFalseDropsAll() {
        final var action = new FilterItems(new Expression("false"), evaluator);

        final var kept = action.apply(
                List.of(OutputRecord.ofJson(Map.of("name", "SOK"))),
                HttpResult.of(200, "{}"));

        assertTrue(kept.isEmpty());
    }
}
