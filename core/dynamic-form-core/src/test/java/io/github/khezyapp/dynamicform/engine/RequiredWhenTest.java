package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.Condition;
import io.github.khezyapp.dynamicform.model.Constraints;
import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.model.FormSchema;
import io.github.khezyapp.dynamicform.model.Op;
import io.github.khezyapp.dynamicform.model.RenderType;
import io.github.khezyapp.dynamicform.model.RequiredWhen;
import io.github.khezyapp.dynamicform.model.ValueType;
import io.github.khezyapp.dynamicform.value.FormValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequiredWhenTest {

    private static final FormSchema EDD_SCHEMA = FormSchema.of("kyc", 1, "forms.kyc.title", List.of(
        FieldSchema.of("customerType", RenderType.SELECT, ValueType.STRING),
        FieldSchema.builder()
            .name("sourceOfFunds")
            .renderType(RenderType.STRING)
            .valueType(ValueType.STRING)
            .constraints(Constraints.builder()
                .requiredWhen(RequiredWhen.of("customerType", Condition.of(Op.EQ, "LEGAL_PERSON")))
                .build())
            .build()
    ));

    @Test
    @DisplayName("Should require the field only while the dependency matches")
    void testRequiredOnlyWhileConditionHolds() {
        final var engine = FormEngine.defaultEngine();

        final var legal = engine.resolve(EDD_SCHEMA,
            FormValues.of(Map.of("customerType", "LEGAL_PERSON")), EvalContext.defaultContext());
        assertFalse(legal.isValid());
        assertEquals("sourceOfFunds", legal.issues().get(0).path());

        final var individual = engine.resolve(EDD_SCHEMA,
            FormValues.of(Map.of("customerType", "INDIVIDUAL")), EvalContext.defaultContext());
        assertTrue(individual.isValid());
        assertNull(individual.values().get("sourceOfFunds"));
    }

    @Test
    @DisplayName("Should pass validation when the conditional field is populated")
    void testPopulatedConditionalField() {
        final var engine = FormEngine.defaultEngine();

        final var resolved = engine.resolve(EDD_SCHEMA,
            FormValues.of(Map.of("customerType", "LEGAL_PERSON", "sourceOfFunds", "salary")),
            EvalContext.defaultContext());

        assertTrue(resolved.isValid());
        assertEquals("salary", resolved.values().get("sourceOfFunds"));
    }
}
