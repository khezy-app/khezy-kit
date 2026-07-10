package io.github.khezyapp.pluginlib;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PluginManagerTest {

    interface Greeter {
        String greet();
    }

    public static class HelloGreeter implements Greeter {
        @Override
        public String greet() {
            return "Hello";
        }
    }

    public static class HiGreeter implements Greeter {
        @Override
        public String greet() {
            return "Hi";
        }
    }

    @Test
    @DisplayName("Should build manager with default store")
    void testBuild() {
        final var manager = PluginManager.of(Greeter.class).build();
        assertNotNull(manager);
        assertInstanceOf(InMemoryPluginStore.class, manager.getStore());
        manager.close();
    }

    @Test
    @DisplayName("Should load eager with custom loader")
    void testLoadEager() {
        final var manager = PluginManager.of(Greeter.class)
                .loader(() -> List.of(
                        new PluginCandidate<>("hello", "1.0.0", HelloGreeter.class),
                        new PluginCandidate<>("hi", "2.0.0", HiGreeter.class)))
                .build();

        final var plugins = manager.loadEager();
        assertEquals(2, plugins.size());

        final var names = plugins.stream().map(g -> g.greet()).collect(Collectors.toSet());
        assertTrue(names.contains("Hello"));
        assertTrue(names.contains("Hi"));
        manager.close();
    }

    @Test
    @DisplayName("Should load lazy and cache instances")
    void testLoadLazy() {
        final var manager = PluginManager.of(Greeter.class)
                .loader(() -> List.of(
                        new PluginCandidate<>("hello", "1.0.0", HelloGreeter.class)))
                .build();

        final var stream = manager.loadLazy();
        final var list = stream.toList();
        assertEquals(1, list.size());
        assertEquals("Hello", list.get(0).greet());

        // Second call should return cached
        final var same = manager.loadLazy().toList();
        assertSame(list.get(0), same.get(0));
        manager.close();
    }

    @Test
    @DisplayName("Should get plugin by name (latest version)")
    void testGetByName() {
        final var manager = PluginManager.of(Greeter.class)
                .loader(() -> List.of(
                        new PluginCandidate<>("my-plugin", "1.0.0", HelloGreeter.class),
                        new PluginCandidate<>("my-plugin", "2.0.0", HiGreeter.class)))
                .build();
        manager.loadEager();

        final var plugin = manager.get("my-plugin");
        assertTrue(plugin.isPresent());
        assertEquals("Hi", plugin.get().greet());
        manager.close();
    }

    @Test
    @DisplayName("Should get plugin by name and version")
    void testGetByNameAndVersion() {
        final var manager = PluginManager.of(Greeter.class)
                .loader(() -> List.of(
                        new PluginCandidate<>("my-plugin", "1.0.0", HelloGreeter.class),
                        new PluginCandidate<>("my-plugin", "2.0.0", HiGreeter.class)))
                .build();
        manager.loadEager();

        final var v1 = manager.get("my-plugin", "1.0.0");
        assertTrue(v1.isPresent());
        assertEquals("Hello", v1.get().greet());

        final var v2 = manager.get("my-plugin", "2.0.0");
        assertTrue(v2.isPresent());
        assertEquals("Hi", v2.get().greet());
        manager.close();
    }

    @Test
    @DisplayName("Should get all versions of a plugin")
    void testGetAll() {
        final var manager = PluginManager.of(Greeter.class)
                .loader(() -> List.of(
                        new PluginCandidate<>("my-plugin", "1.0.0", HelloGreeter.class),
                        new PluginCandidate<>("my-plugin", "2.0.0", HiGreeter.class)))
                .build();
        manager.loadEager();

        final var all = manager.getAll("my-plugin");
        assertEquals(2, all.size());
        manager.close();
    }

    @Test
    @DisplayName("Should return empty optional for missing plugin")
    void testGetMissing() {
        final var manager = PluginManager.of(Greeter.class)
                .loader(() -> List.of(
                        new PluginCandidate<>("hello", "1.0.0", HelloGreeter.class)))
                .build();
        manager.loadEager();

        assertTrue(manager.get("nonexistent").isEmpty());
        assertTrue(manager.get("nonexistent", "1.0.0").isEmpty());
        manager.close();
    }

    @Test
    @DisplayName("Should get plugins after eager load")
    void testGetPlugins() {
        final var manager = PluginManager.of(Greeter.class)
                .loader(() -> List.of(
                        new PluginCandidate<>("a", "1.0.0", HelloGreeter.class)))
                .build();
        manager.loadEager();

        final var plugins = manager.getPlugins();
        assertEquals(1, plugins.size());
        manager.close();
    }

    @Test
    @DisplayName("Should return empty list before loading")
    void testGetPluginsBeforeLoad() {
        final var manager = PluginManager.of(Greeter.class)
                .loader(() -> List.of(
                        new PluginCandidate<>("a", "1.0.0", HelloGreeter.class)))
                .build();

        assertTrue(manager.getPlugins().isEmpty());
        manager.close();
    }
}
