package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.Condition;
import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.model.Op;
import io.github.khezyapp.dynamicform.model.RenderType;
import io.github.khezyapp.dynamicform.model.ValueType;
import io.github.khezyapp.dynamicform.model.Visibility;
import io.github.khezyapp.dynamicform.value.FormValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VisibilityEvaluatorTest {

    private static FieldSchema field(final Visibility visibility) {
        return FieldSchema.builder()
                .name("sample")
                .renderType(RenderType.STRING)
                .valueType(ValueType.STRING)
                .visibility(visibility)
                .build();
    }

    @Test
    @DisplayName("Should show only when every show condition matches (AND)")
    void testShowIsAnd() {
        final var visibility = Visibility.of(Map.of(
                "country", List.of(Condition.of(Op.EQ, "US")),
                "customerType", List.of(Condition.of(Op.EQ, "LEGAL_PERSON"))
        ));
        final var values = FormValues.of(Map.of("country", "US", "customerType", "LEGAL_PERSON"));

        assertTrue(VisibilityEvaluator.isVisible(field(visibility), values, EvalContext.defaultContext()));
        assertFalse(VisibilityEvaluator.isVisible(field(visibility),
                values.with("customerType", "INDIVIDUAL"), EvalContext.defaultContext()));
    }

    @Test
    @DisplayName("Should hide when any hide condition matches (OR)")
    void testHideIsOr() {
        final var visibility = new Visibility(null, Map.of(
                "country", List.of(Condition.of(Op.EQ, "US"), Condition.of(Op.EQ, "KH"))
        ));
        final var values = FormValues.of(Map.of("country", "US"));

        assertFalse(VisibilityEvaluator.isVisible(field(visibility), values, EvalContext.defaultContext()));
        assertTrue(VisibilityEvaluator.isVisible(field(visibility),
                values.with("country", "TH"), EvalContext.defaultContext()));
    }

    @Test
    @DisplayName("Should match every operator")
    void testAllOperators() {
        assertTrue(matches(Op.EQ, "US", "US"));
        assertFalse(matches(Op.EQ, "US", "KH"));

        assertTrue(matches(Op.NOT, "US", "KH"));
        assertFalse(matches(Op.NOT, "US", "US"));

        assertTrue(matches(Op.GTE, 3, 3));
        assertTrue(matches(Op.LTE, 3, 5));
        assertTrue(matches(Op.GT, 4, 3));
        assertTrue(matches(Op.LT, 2, 3));

        assertTrue(matches(Op.BETWEEN, 5, List.of(1, 10)));
        assertFalse(matches(Op.BETWEEN, 11, List.of(1, 10)));

        assertTrue(matches(Op.STARTS_WITH, "passport-US", "passport"));
        assertTrue(matches(Op.ENDS_WITH, "ID-789", "789"));
        assertTrue(matches(Op.INCLUDES, List.of("A", "B"), "B"));
        assertTrue(matches(Op.INCLUDES, "hello world", "world"));
        assertTrue(matches(Op.REGEX, "KHL-1234", "KHL-\\d{4}"));

        assertTrue(matches(Op.EXISTS, "anything", null));
    }

    @Test
    @DisplayName("Should treat null values as unknown for non-EXISTS ops")
    void testNullValue() {
        assertFalse(matches(Op.EXISTS, null, null));
        assertTrue(matches(Op.EQ, null, null));
        assertFalse(matches(Op.GTE, null, 1));
    }

    @Test
    @DisplayName("Should resolve @version, @deployment, and @feature context references")
    void testContextReferences() {
        final var ctx = EvalContext.builder()
                .schemaVersion(2)
                .deployment("cloud")
                .feature("beta", true)
                .build();

        assertTrue(matchesAt("@version", Condition.of(Op.GTE, 2), FormValues.empty(), ctx));
        assertTrue(matchesAt("@deployment", Condition.of(Op.EQ, "cloud"), FormValues.empty(), ctx));
        assertTrue(matchesAt("@feature:beta", Condition.of(Op.EQ, true), FormValues.empty(), ctx));
    }

    private static boolean matches(final Op op,
                                   final Object value,
                                   final Object operand) {
        return VisibilityEvaluator.matches(Condition.of(op, operand), value);
    }

    private static boolean matchesAt(final String reference,
                                     final Condition condition,
                                     final FormValues values,
                                     final EvalContext ctx) {
        final var dependency = VisibilityEvaluator.resolveReference(reference, values, ctx, "");
        return VisibilityEvaluator.matches(condition, dependency);
    }
}
