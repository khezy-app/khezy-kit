package io.github.khezyapp.pluginlib;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPluginStoreTest {

    private InMemoryPluginStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryPluginStore();
    }

    private static InstalledPlugin plugin(final String name,
                                          final String version) {
        return new InstalledPlugin(
                name, version, name, "", "",
                new PluginSource.ClasspathSource(),
                true, Instant.now(), Instant.now(), Map.of());
    }

    @Test
    @DisplayName("Should install and retrieve plugin")
    void testInstallAndGet() {
        store.install(plugin("test", "2.0.0"));

        final var loaded = store.get("test", "2.0.0");
        assertTrue(loaded.isPresent());
        assertEquals("test", loaded.get().name());
        assertEquals("2.0.0", loaded.get().version());
        assertTrue(loaded.get().enabled());
    }

    @Test
    @DisplayName("Should return empty for non-existent plugin")
    void testGetNonExistent() {
        assertTrue(store.get("nonexistent", "1.0.0").isEmpty());
    }

    @Test
    @DisplayName("Should overwrite on reinstall")
    void testOverwriteOnReinstall() {
        final var now = Instant.now();
        store.install(new InstalledPlugin(
                "p", "1.0.0", "P", "", "",
                new PluginSource.ClasspathSource(),
                true, now, now, Map.of()));
        store.install(new InstalledPlugin(
                "p", "1.0.0", "P", "", "",
                new PluginSource.ClasspathSource(),
                false, now, now, Map.of()));

        final var loaded = store.get("p", "1.0.0");
        assertTrue(loaded.isPresent());
        assertFalse(loaded.get().enabled());
    }

    @Test
    @DisplayName("Should list all installed plugins")
    void testList() {
        store.install(plugin("a", "1.0.0"));
        store.install(plugin("b", "1.0.0"));

        assertEquals(2, store.list().size());
    }

    @Test
    @DisplayName("Should get all versions of a plugin")
    void testGetAll() {
        store.install(plugin("multi", "1.0.0"));
        store.install(plugin("multi", "2.0.0"));

        assertEquals(2, store.getAll("multi").size());
    }

    @Test
    @DisplayName("Should get latest version by semver")
    void testGetLatest() {
        store.install(plugin("my-plugin", "1.0.0"));
        store.install(plugin("my-plugin", "2.0.0"));
        store.install(plugin("my-plugin", "10.0.0"));

        final var latest = store.getLatest("my-plugin");
        assertTrue(latest.isPresent());
        assertEquals("10.0.0", latest.get().version());
    }

    @Test
    @DisplayName("Should return empty for getLatest on non-existent plugin")
    void testGetLatestNonExistent() {
        assertTrue(store.getLatest("nothing").isEmpty());
    }

    @Test
    @DisplayName("Should uninstall plugin")
    void testUninstall() {
        store.install(plugin("del", "1.0.0"));
        store.uninstall("del", "1.0.0");

        assertTrue(store.get("del", "1.0.0").isEmpty());
    }

    @Test
    @DisplayName("Should set enabled state")
    void testSetEnabled() {
        store.install(plugin("x", "1.0.0"));
        store.setEnabled("x", "1.0.0", false);

        final var loaded = store.get("x", "1.0.0");
        assertTrue(loaded.isPresent());
        assertFalse(loaded.get().enabled());
    }

    @Test
    @DisplayName("Should clear all data")
    void testClear() {
        store.install(plugin("a", "1.0.0"));
        store.install(plugin("b", "1.0.0"));
        store.clear();

        assertTrue(store.list().isEmpty());
    }
}
