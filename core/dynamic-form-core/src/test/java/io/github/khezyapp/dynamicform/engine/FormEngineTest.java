package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.Constraints;
import io.github.khezyapp.dynamicform.model.FieldAction;
import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.model.FormSchema;
import io.github.khezyapp.dynamicform.model.Options;
import io.github.khezyapp.dynamicform.model.RenderType;
import io.github.khezyapp.dynamicform.model.ValueType;
import io.github.khezyapp.dynamicform.spi.ActionResult;
import io.github.khezyapp.dynamicform.spi.Option;
import io.github.khezyapp.dynamicform.value.FormValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormEngineTest {

    private static final FormEngine ENGINE = FormEngine.defaultEngine();
    private static final EvalContext CTX = EvalContext.defaultContext();

    private static FormSchema schema(final FieldSchema... fields) {
        return FormSchema.of("test", 1, "forms.test.title", List.of(fields));
    }

    @Test
    @DisplayName("Should apply defaults for absent visible fields")
    void testDefaultsApplied() {
        final var schema = schema(
            FieldSchema.builder().name("country").renderType(RenderType.SELECT)
                .valueType(ValueType.STRING).defaultValue("US").build()
        );

        final var resolved = ENGINE.resolve(schema, FormValues.empty(), CTX);

        assertEquals("US", resolved.values().get("country"));
        assertTrue(resolved.isValid());
    }

    @Test
    @DisplayName("Should report a missing required field")
    void testMissingRequired() {
        final var schema = schema(
            FieldSchema.builder().name("dateOfBirth").renderType(RenderType.DATE_TIME)
                .valueType(ValueType.DATE_TIME).constraints(Constraints.mandatory()).build()
        );

        final var resolved = ENGINE.resolve(schema, FormValues.empty(), CTX);

        assertFalse(resolved.isValid());
        assertEquals(1, resolved.issues().size());
        assertEquals("dateOfBirth", resolved.issues().get(0).path());
    }

    @Test
    @DisplayName("Should coerce string numbers and booleans")
    void testCoercion() {
        final var schema = schema(
            FieldSchema.builder().name("age").renderType(RenderType.NUMBER)
                .valueType(ValueType.NUMBER).build(),
            FieldSchema.builder().name("active").renderType(RenderType.BOOLEAN)
                .valueType(ValueType.BOOLEAN).build()
        );

        final var resolved = ENGINE.resolve(schema,
            FormValues.of(Map.of("age", "25", "active", "true")), CTX);

        assertEquals(25L, resolved.values().get("age"));
        assertEquals(Boolean.TRUE, resolved.values().get("active"));
        assertTrue(resolved.isValid());
    }

    @Test
    @DisplayName("Should report a wrong-typed value as an issue")
    void testWrongTypeIssue() {
        final var schema = schema(
            FieldSchema.builder().name("age").renderType(RenderType.NUMBER)
                .valueType(ValueType.NUMBER).build()
        );

        final var resolved = ENGINE.resolve(schema, FormValues.of(Map.of("age", "abc")), CTX);

        assertFalse(resolved.isValid());
        assertEquals("age", resolved.issues().get(0).path());
        assertTrue(resolved.issues().get(0).message().contains("not a number"));
    }

    @Test
    @DisplayName("Should ignore value-less NOTICE nodes")
    void testNoticeIgnored() {
        final var schema = schema(
            FieldSchema.builder().name("notes").renderType(RenderType.NOTICE)
                .meta(Map.of("textKey", "forms.test.notes")).build()
        );

        final var resolved = ENGINE.resolve(schema, FormValues.of(Map.of("notes", "provided")), CTX);

        assertTrue(resolved.isValid());
        assertFalse(resolved.values().has("notes"));
    }

    @Test
    @DisplayName("Should resolve GROUP children with scoped issue paths")
    void testGroupNesting() {
        final var schema = schema(
            FieldSchema.builder().name("documents").renderType(RenderType.GROUP)
                .valueType(ValueType.OBJECT)
                .children(List.of(
                    FieldSchema.builder().name("idNumber").renderType(RenderType.STRING)
                        .valueType(ValueType.STRING).constraints(Constraints.mandatory()).build()
                ))
                .build()
        );

        final var resolved = ENGINE.resolve(schema, FormValues.of(Map.of("documents", Map.of())), CTX);

        assertFalse(resolved.isValid());
        assertEquals("documents.idNumber", resolved.issues().get(0).path());
    }

    @Test
    @DisplayName("Should enforce pattern and length constraints")
    void testConstraints() {
        final var schema = schema(
            FieldSchema.builder().name("phone").renderType(RenderType.STRING)
                .valueType(ValueType.STRING)
                .constraints(Constraints.builder().pattern("\\+\\d{8,15}").build())
                .build()
        );

        final var bad = ENGINE.resolve(schema, FormValues.of(Map.of("phone", "012")), CTX);
        assertFalse(bad.isValid());
        assertEquals("phone", bad.issues().get(0).path());

        final var good = ENGINE.resolve(schema, FormValues.of(Map.of("phone", "+85512345678")), CTX);
        assertTrue(good.isValid());
    }

    @Test
    @DisplayName("Should reject values not in the inline option list")
    void testInlineOptions() {
        final var schema = schema(
            FieldSchema.builder().name("idType").renderType(RenderType.SELECT)
                .valueType(ValueType.ENUM)
                .options(Options.inline(List.of(
                    Option.of("Passport", "passport"),
                    Option.of("Driving licence", "dl")
                )))
                .build()
        );

        final var resolved = ENGINE.resolve(schema, FormValues.of(Map.of("idType", "military")), CTX);

        assertFalse(resolved.isValid());
        assertEquals("idType", resolved.issues().get(0).path());
    }

    @Test
    @DisplayName("Should invoke declared actions through registered handlers")
    void testInvokeAction() {
        final var engine = FormEngine.defaultEngine();
        engine.actionRegistry().register("testConnection", ctx -> ActionResult.ok("connected"));

        final var field = FieldSchema.builder().name("server").renderType(RenderType.BUTTON)
            .actions(List.of(FieldAction.of("onClick", "testConnection"))).build();
        final var action = field.actions().get(0);

        final var result = engine.invokeAction(action, field, FormValues.empty(), CTX);

        assertTrue(result.success());
        assertEquals("connected", result.message());
    }
}
