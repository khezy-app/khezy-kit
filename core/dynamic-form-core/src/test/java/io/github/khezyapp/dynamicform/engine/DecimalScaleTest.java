package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.Constraints;
import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.model.FormSchema;
import io.github.khezyapp.dynamicform.model.RenderType;
import io.github.khezyapp.dynamicform.model.ValueType;
import io.github.khezyapp.dynamicform.value.FormValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DecimalScaleTest {

    private static FormSchema decimalForm(final Constraints constraints) {
        return FormSchema.of("shares", 1, "forms.shares.title", List.of(
            FieldSchema.builder().name("ownership").renderType(RenderType.DECIMAL)
                .valueType(ValueType.DECIMAL).constraints(constraints).build()
        ));
    }

    @Test
    @DisplayName("Should coerce a string to DECIMAL honoring scale")
    void testScaleCoercion() {
        final var engine = FormEngine.defaultEngine();
        final var schema = decimalForm(Constraints.builder().scale(2).build());

        final var resolved = engine.resolve(schema, FormValues.of(Map.of("ownership", "99.5")),
            EvalContext.defaultContext());

        assertTrue(resolved.isValid());
        final var value = (BigDecimal) resolved.values().get("ownership");
        assertEquals(new BigDecimal("99.50"), value);
        assertEquals(2, value.scale());
    }

    @Test
    @DisplayName("Should flag DECIMAL(p,s) integer overflow")
    void testPrecisionOverflow() {
        final var engine = FormEngine.defaultEngine();
        final var schema = decimalForm(Constraints.builder().scale(2).precision(5).build());

        final var resolved = engine.resolve(schema, FormValues.of(Map.of("ownership", "1234.5")),
            EvalContext.defaultContext());

        assertFalse(resolved.isValid());
        assertEquals("ownership", resolved.issues().get(0).path());
        assertTrue(resolved.issues().get(0).message().contains("DECIMAL(5,2)"));
    }

    @Test
    @DisplayName("Should accept a value within DECIMAL(p,s) bounds")
    void testPrecisionAccepted() {
        final var engine = FormEngine.defaultEngine();
        final var schema = decimalForm(Constraints.builder().scale(2).precision(5).build());

        final var resolved = engine.resolve(schema, FormValues.of(Map.of("ownership", "999.99")),
            EvalContext.defaultContext());

        assertTrue(resolved.isValid());
        assertEquals(new BigDecimal("999.99"), resolved.values().get("ownership"));
    }

    @Test
    @DisplayName("Should enforce min and max numeric bounds")
    void testNumericBounds() {
        final var engine = FormEngine.defaultEngine();
        final var schema = decimalForm(Constraints.builder().min(0).max(100).build());

        final var over = engine.resolve(schema, FormValues.of(Map.of("ownership", "150")),
            EvalContext.defaultContext());
        assertFalse(over.isValid());
        assertTrue(over.issues().get(0).message().contains("<= 100"));

        final var ok = engine.resolve(schema, FormValues.of(Map.of("ownership", "75")),
            EvalContext.defaultContext());
        assertTrue(ok.isValid());
    }
}
