package io.github.khezyapp.dhttp.action.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.spec.Expression;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.doa.DynamicObjects;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SetKeyValueTest {

    private final JexlExpressionEvaluator evaluator = new JexlExpressionEvaluator();

    @Test
    @DisplayName("Should set literal values on each record")
    void setsLiteralValues() {
        final var action = new SetKeyValue(
                Map.of("city", new Expression("Battambang")), evaluator);
        final var records = List.of(OutputRecord.ofJson(Map.of("name", "SOK")));

        final var result = action.apply(records, HttpResult.of(200, "{}"));

        assertEquals("Battambang", result.get(0).json().get("city"));
        assertEquals("SOK", result.get(0).json().get("name"));
    }

    @Test
    @DisplayName("Should resolve dotted keys through DynamicObjects")
    void setsDottedPath() {
        final var action = new SetKeyValue(
                Map.of("address.city", new Expression("Phnom Penh")), evaluator);
        final var records = List.of(OutputRecord.ofJson(Map.of("name", "VISAL")));

        final var result = action.apply(records, HttpResult.of(200, "{}"));

        final var address = DynamicObjects.get(result.get(0).json(), "address");
        assertEquals(Map.of("city", "Phnom Penh"), address);
    }
}
