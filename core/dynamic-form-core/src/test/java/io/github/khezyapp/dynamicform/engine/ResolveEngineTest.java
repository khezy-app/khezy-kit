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

class ResolveEngineTest {

    private static FieldSchema simple(final String name) {
        return FieldSchema.of(name, RenderType.STRING, ValueType.STRING);
    }

    private static FieldSchema conditional(final String name,
                                           final String dep,
                                           final Object depValue) {
        return FieldSchema.builder()
            .name(name)
            .renderType(RenderType.STRING)
            .valueType(ValueType.STRING)
            .visibility(Visibility.show(dep, Condition.of(Op.EQ, depValue)))
            .build();
    }

    @Test
    @DisplayName("Should resolve chained dependencies in dependency order")
    void testChainDependencies() {
        final var fields = List.of(
            conditional("city", "country", "US"),
            simple("country"),
            FieldSchema.builder()
                .name("district")
                .renderType(RenderType.STRING)
                .valueType(ValueType.STRING)
                .defaultValue("KAMPOT")
                .visibility(Visibility.show("city", Condition.of(Op.EQ, "PHNOM_PENH")))
                .build()
        );
        final var raw = FormValues.of(Map.of("country", "US", "city", "PHNOM_PENH"));

        final var resolved = ResolveEngine.resolve(fields, raw, EvalContext.defaultContext(), FormRuntime.defaults());

        assertEquals("US", resolved.values().get("country"));
        assertEquals("PHNOM_PENH", resolved.values().get("city"));
        assertEquals("KAMPOT", resolved.values().get("district"));
        assertTrue(resolved.isValid());
    }

    @Test
    @DisplayName("Should pick the duplicate-name declaration whose visibility holds (n8n resource->operation)")
    void testDuplicateNameVisibility() {
        final var httpOperation = FieldSchema.builder()
            .name("operation")
            .renderType(RenderType.STRING)
            .valueType(ValueType.STRING)
            .defaultValue("http-call")
            .visibility(Visibility.show("resource", Condition.of(Op.EQ, "http")))
            .build();
        final var ftpOperation = FieldSchema.builder()
            .name("operation")
            .renderType(RenderType.STRING)
            .valueType(ValueType.STRING)
            .defaultValue("ftp-call")
            .visibility(Visibility.show("resource", Condition.of(Op.EQ, "ftp")))
            .build();
        final var fields = List.of(simple("resource"), httpOperation, ftpOperation);

        final var http = ResolveEngine.resolve(fields, FormValues.of(Map.of("resource", "http")),
            EvalContext.defaultContext(), FormRuntime.defaults());
        assertEquals("http-call", http.values().get("operation"));

        final var ftp = ResolveEngine.resolve(fields, FormValues.of(Map.of("resource", "ftp")),
            EvalContext.defaultContext(), FormRuntime.defaults());
        assertEquals("ftp-call", ftp.values().get("operation"));
    }

    @Test
    @DisplayName("Should throw SchemaException on ambiguous duplicate declarations")
    void testAmbiguousDeclaration() {
        final var fields = List.of(
            simple("resource"),
            conditional("operation", "resource", "http"),
            conditional("operation", "resource", "http")
        );

        final var exception = assertThrows(SchemaException.class, () ->
            ResolveEngine.resolve(fields, FormValues.of(Map.of("resource", "http")),
                EvalContext.defaultContext(), FormRuntime.defaults()));

        assertTrue(exception.getMessage().contains("ambiguous declaration"));
    }

    @Test
    @DisplayName("Should throw SchemaException on a dangling dependency")
    void testDanglingDependency() {
        final var fields = List.of(conditional("state", "country", "US"));

        final var exception = assertThrows(SchemaException.class, () ->
            ResolveEngine.resolve(fields, FormValues.empty(), EvalContext.defaultContext(), FormRuntime.defaults()));

        assertTrue(exception.getMessage().contains("unresolvable dependency"));
    }

    @Test
    @DisplayName("Should throw SchemaException on a dependency cycle")
    void testDependencyCycle() {
        final var fields = List.of(
            conditional("a", "b", "x"),
            conditional("b", "a", "x")
        );

        final var exception = assertThrows(SchemaException.class, () ->
            ResolveEngine.resolve(fields, FormValues.empty(), EvalContext.defaultContext(), FormRuntime.defaults()));

        assertTrue(exception.getMessage().contains("unresolvable dependency"));
    }

    @Test
    @DisplayName("Should drop the value of a hidden field")
    void testHiddenValueDropped() {
        final var fields = List.of(
            simple("flag"),
            FieldSchema.builder()
                .name("secret")
                .renderType(RenderType.STRING)
                .valueType(ValueType.STRING)
                .visibility(Visibility.show("flag", Condition.of(Op.EQ, true)))
                .build()
        );

        final var resolved = ResolveEngine.resolve(fields,
            FormValues.of(Map.of("flag", "false", "secret", "leaked")),
            EvalContext.defaultContext(), FormRuntime.defaults());

        assertFalse(resolved.values().has("secret"));
    }

    @Test
    @DisplayName("Should detect static cycles via DependencyGraph")
    void testDependencyGraphCycleDetection() {
        final var fields = List.of(
            conditional("a", "b", "x"),
            conditional("b", "a", "x")
        );

        assertTrue(DependencyGraph.hasCycle(fields));
        assertEquals(List.of("a", "b"), DependencyGraph.findCycle(fields));
    }
}
