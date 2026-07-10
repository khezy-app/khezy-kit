package io.github.khezyapp.pluginlib;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositePluginLoaderTest {

    @Test
    @DisplayName("Should combine results from multiple loaders")
    void testCombine() {
        final var loader1 = (PluginLoader<String>) () -> List.of(
                new PluginCandidate<>("a", "1.0.0", String.class));
        final var loader2 = (PluginLoader<String>) () -> List.<PluginCandidate<String>>of(
                new PluginCandidate<>("b", "1.0.0", String.class));

        final var composite = new CompositePluginLoader<>(List.of(loader1, loader2));
        final var results = composite.loadPlugins();

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Should deduplicate by name+version+class")
    void testDeduplication() {
        final var loader1 = (PluginLoader<String>) () -> List.of(
                new PluginCandidate<>("dup", "1.0.0", String.class));
        final var loader2 = (PluginLoader<String>) () -> List.of(
                new PluginCandidate<>("dup", "1.0.0", String.class));

        final var composite = new CompositePluginLoader<>(List.of(loader1, loader2));
        final var results = composite.loadPlugins();

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Should keep same name different versions")
    void testMultiVersion() {
        final var loader1 = (PluginLoader<String>) () -> List.of(
                new PluginCandidate<>("p", "1.0.0", String.class));
        final var loader2 = (PluginLoader<String>) () -> List.of(
                new PluginCandidate<>("p", "2.0.0", String.class));

        final var composite = new CompositePluginLoader<>(List.of(loader1, loader2));
        final var results = composite.loadPlugins();

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Should handle empty loader list")
    void testEmpty() {
        final var composite = new CompositePluginLoader<>(List.of());
        assertTrue(composite.loadPlugins().isEmpty());
    }

    @Test
    @DisplayName("Should close closeable children")
    void testClose() {
        final var closeable = new PluginLoader<String>() {
            @Override
            public List<PluginCandidate<String>> loadPlugins() {
                return List.of();
            }
        };
        final var composite = new CompositePluginLoader<>(List.of(closeable));
        assertDoesNotThrow(composite::close);
    }
}
