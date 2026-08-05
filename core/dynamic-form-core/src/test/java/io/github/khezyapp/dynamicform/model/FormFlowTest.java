package io.github.khezyapp.dynamicform.model;

import io.github.khezyapp.dynamicform.engine.EvalContext;
import io.github.khezyapp.dynamicform.engine.FormEngine;
import io.github.khezyapp.dynamicform.value.FormValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormFlowTest {

    private static FormSchema step(final String id,
                                   final String fieldName) {
        return FormSchema.of(id, 1, "forms." + id + ".title", List.of(
                FieldSchema.of(fieldName, RenderType.STRING, ValueType.STRING)
        ));
    }

    @Test
    @DisplayName("Should require at least one step")
    void testRequiresSteps() {
        assertThrows(IllegalArgumentException.class, () -> FormFlow.of("kyc", List.of()));
    }

    @Test
    @DisplayName("Should expose steps by index and count")
    void testStepAccess() {
        final var flow = FormFlow.of("kyc", List.of(
                step("personal", "name"),
                step("documents", "idNumber")
        ));

        assertEquals(2, flow.stepCount());
        assertEquals("personal", flow.stepAt(0).id());
        assertEquals("documents", flow.stepAt(1).id());
    }

    @Test
    @DisplayName("Should resolve each wizard step as a plain schema")
    void testMultiStepResolve() {
        final var flow = FormFlow.of("kyc", List.of(
                step("personal", "name"),
                step("documents", "idNumber")
        ));
        final var engine = FormEngine.defaultEngine();

        final var first = engine.resolve(flow.stepAt(0), FormValues.of(Map.of("name", "VISAL")),
                EvalContext.defaultContext());
        final var second = engine.resolve(flow.stepAt(1), FormValues.of(Map.of("idNumber", "A123")),
                EvalContext.defaultContext());

        assertTrue(first.isValid());
        assertEquals("VISAL", first.values().get("name"));
        assertTrue(second.isValid());
        assertEquals("A123", second.values().get("idNumber"));
    }
}
