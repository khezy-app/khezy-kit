package io.github.khezyapp.dhttp.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.khezyapp.dhttp.action.builtin.RootProperty;
import io.github.khezyapp.dhttp.expr.jexl.JexlExpressionEvaluator;
import io.github.khezyapp.dhttp.spec.PostReceive;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ActionRegistryTest {

    private static final String[] BUILT_INS = {
            "rootProperty", "filter", "limit", "setValue", "sortByKey", "setKeyValue", "binaryData"
    };

    @Test
    @DisplayName("Should resolve all seven named built-ins")
    void withBuiltinsResolvesBuiltIns() {
        final var registry = ActionRegistry.withBuiltins();

        for (final String name : BUILT_INS) {
            assertTrue(registry.get(name).isPresent(), "missing built-in: " + name);
        }
    }

    @Test
    @DisplayName("Should return empty for an unknown name")
    void unknownNameIsEmpty() {
        final var registry = ActionRegistry.withBuiltins();

        assertTrue(registry.get("no-such-action").isEmpty());
    }

    @Test
    @DisplayName("Should register and look up a custom factory")
    void customFactoryRegistersAndLooksUp() {
        final var registry = ActionRegistry.withBuiltins();
        final var evaluator = new JexlExpressionEvaluator();

        registry.register("myAction", (final var descriptor, final var eval) -> new RootProperty("items"));

        assertTrue(registry.get("myAction").isPresent());
        final var action = registry.create(
                new PostReceive.CustomPostReceive("myAction", Map.of("property", "items")),
                evaluator);
        assertInstanceOf(RootProperty.class, action);
    }

    @Test
    @DisplayName("Should create the rootProperty action from its sealed variant")
    void createMapsSealedVariant() {
        final var registry = ActionRegistry.withBuiltins();

        final var action = registry.create(new PostReceive.RootProperty("data.items"),
                new JexlExpressionEvaluator());

        assertEquals(new RootProperty("data.items"), action);
    }

    @Test
    @DisplayName("Should throw for an unregistered custom action key")
    void unknownCustomKeyThrows() {
        final var registry = ActionRegistry.withBuiltins();

        assertThrows(IllegalArgumentException.class, () -> registry.create(
                new PostReceive.CustomPostReceive("missing", Map.of()),
                new JexlExpressionEvaluator()));
    }
}
