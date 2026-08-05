package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.CollectionSpec;
import io.github.khezyapp.dynamicform.model.Constraints;
import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.model.FormSchema;
import io.github.khezyapp.dynamicform.model.RenderType;
import io.github.khezyapp.dynamicform.model.ValueType;
import io.github.khezyapp.dynamicform.value.FormValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectionSpecTest {

    private static final FieldSchema DIRECTOR_ITEM = FieldSchema.builder()
        .name("idNumber")
        .renderType(RenderType.STRING)
        .valueType(ValueType.STRING)
        .constraints(Constraints.mandatory())
        .build();

    private static FormSchema collectionForm(final CollectionSpec spec) {
        return FormSchema.of("directors", 1, "forms.directors.title", List.of(
            FieldSchema.builder()
                .name("directors")
                .renderType(RenderType.COLLECTION)
                .valueType(ValueType.ARRAY)
                .collection(spec)
                .build()
        ));
    }

    @Test
    @DisplayName("Should resolve each row and report index-path issues")
    void testRowIssues() {
        final var schema = collectionForm(CollectionSpec.of(List.of(DIRECTOR_ITEM)));
        final var engine = FormEngine.defaultEngine();

        final var resolved = engine.resolve(schema, FormValues.of(Map.of("directors", List.of(
            Map.of("idNumber", "N1"),
            Map.of("idNumber", "")
        ))), EvalContext.defaultContext());

        assertFalse(resolved.isValid());
        assertEquals(1, resolved.issues().size());
        assertEquals("directors[1].idNumber", resolved.issues().get(0).path());
    }

    @Test
    @DisplayName("Should enforce minItems")
    void testMinItems() {
        final var schema = collectionForm(CollectionSpec.of(2, null, List.of(DIRECTOR_ITEM)));
        final var engine = FormEngine.defaultEngine();

        final var resolved = engine.resolve(schema,
            FormValues.of(Map.of("directors", List.of(Map.of("idNumber", "N1")))),
            EvalContext.defaultContext());

        assertFalse(resolved.isValid());
        assertTrue(resolved.issues().stream()
            .anyMatch(issue -> issue.path().equals("directors") && issue.message().contains("at least 2")));
    }

    @Test
    @DisplayName("Should enforce maxItems")
    void testMaxItems() {
        final var schema = collectionForm(CollectionSpec.of(null, 2, List.of(DIRECTOR_ITEM)));
        final var engine = FormEngine.defaultEngine();

        final var resolved = engine.resolve(schema, FormValues.of(Map.of("directors", List.of(
            Map.of("idNumber", "N1"),
            Map.of("idNumber", "N2"),
            Map.of("idNumber", "N3")
        ))), EvalContext.defaultContext());

        assertFalse(resolved.isValid());
        assertTrue(resolved.issues().stream()
            .anyMatch(issue -> issue.path().equals("directors") && issue.message().contains("exceed 2")));
    }

    @Test
    @DisplayName("Should flag a non-list collection value")
    void testNonListValue() {
        final var schema = collectionForm(CollectionSpec.of(List.of(DIRECTOR_ITEM)));
        final var engine = FormEngine.defaultEngine();

        final var resolved = engine.resolve(schema, FormValues.of(Map.of("directors", "N1")),
            EvalContext.defaultContext());

        assertFalse(resolved.isValid());
        assertEquals("directors", resolved.issues().get(0).path());
    }
}
