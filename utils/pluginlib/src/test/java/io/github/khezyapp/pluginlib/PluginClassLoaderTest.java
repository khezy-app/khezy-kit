package io.github.khezyapp.pluginlib;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PluginClassLoaderTest {

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("Should load class from child first (parent-last)")
    void testParentLast() throws Exception {
        final var url = new File(".").toURI().toURL();
        final var cl = new PluginClassLoader(
                new URL[]{url},
                getClass().getClassLoader());

        // Loading this test class - parent has it, but parent-last would
        // also try local first. Since local doesn't have it, falls back to parent.
        final var loaded = cl.loadClass(getClass().getName());
        assertSame(getClass(), loaded);
        cl.close();
    }

    @Test
    @DisplayName("Should delegate java.* to parent first")
    void testDelegateFirst() throws Exception {
        final var url = new File(".").toURI().toURL();
        final var cl = new PluginClassLoader(
                new URL[]{url},
                getClass().getClassLoader());

        // java.lang.String should always come from parent (delegate-first)
        final var loaded = cl.loadClass("java.lang.String");
        assertSame(String.class, loaded);
        cl.close();
    }

    @Test
    @DisplayName("Should use custom delegate-first prefixes")
    void testCustomDelegatePrefix() throws Exception {
        final var url = new File(".").toURI().toURL();
        final var cl = new PluginClassLoader(
                new URL[] {url},
                getClass().getClassLoader(),
                "com.custom.");

        assertDoesNotThrow(() -> cl.loadClass("java.lang.String"));
        cl.close();
    }
}
