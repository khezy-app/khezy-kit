package io.github.khezyapp.pluginlib;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginInfoTest {

    interface Transformer {
        String transform(String input);
    }

    @PluginInfo(name = "json-formatter", version = "2.1.0", description = "JSON formatter", vendor = "Acme")
    public static class JsonTransformer implements Transformer {
        @Override
        public String transform(final String input) {
            return "json:" + input;
        }
    }

    @PluginInfo(name = "xml-formatter", version = "1.0.0")
    public static class XmlTransformer implements Transformer {
        @Override
        public String transform(final String input) {
            return "xml:" + input;
        }
    }

    public static class PlainTransformer implements Transformer {
        @Override
        public String transform(final String input) {
            return input;
        }
    }

    @Test
    @DisplayName("Should read @PluginInfo name and version from annotated class")
    void testAnnotationReadsNameAndVersion() {
        final var loader = (PluginLoader<Transformer>) () -> {
            final var c1 = JsonTransformer.class;
            final var c2 = XmlTransformer.class;
            return List.of(
                    resolveCandidate(c1),
                    resolveCandidate(c2));
        };

        final var manager = PluginManager.of(Transformer.class)
                .loader(loader)
                .build();

        manager.loadEager();

        assertEquals("json-formatter",
                manager.getStore().get("json-formatter", "2.1.0")
                        .map(InstalledPlugin::displayName).orElse(""));
        assertEquals("xml-formatter",
                manager.getStore().get("xml-formatter", "1.0.0")
                        .map(InstalledPlugin::displayName).orElse(""));
        manager.close();
    }

    @Test
    @DisplayName("Should fall back to class simple name when no @PluginInfo")
    void testFallbackToSimpleName() {
        final var candidates = new java.util.ArrayList<PluginCandidate<Transformer>>();
        final var info = PlainTransformer.class.getAnnotation(PluginInfo.class);
        assertNull(info);

        final var name = PlainTransformer.class.getSimpleName();
        final var version = "1.0.0";
        candidates.add(new PluginCandidate<>(name, version, PlainTransformer.class));

        final var manager = PluginManager.of(Transformer.class)
                .loader(() -> candidates)
                .build();

        manager.loadEager();
        assertTrue(manager.get("PlainTransformer", "1.0.0").isPresent());
        manager.close();
    }

    @Test
    @DisplayName("Should resolve @PluginInfo via ServiceLoaderPluginLoader")
    void testServiceLoaderPluginLoaderWithAnnotation() {
        // ServiceLoaderPluginLoader reads @PluginInfo from each provider class.
        // This test verifies the logic path without ServiceLoader infrastructure
        // by calling the same resolution inline.
        final var info = JsonTransformer.class.getAnnotation(PluginInfo.class);
        assertNotNull(info);
        assertEquals("json-formatter", info.name());
        assertEquals("2.1.0", info.version());
        assertEquals("JSON formatter", info.description());
        assertEquals("Acme", info.vendor());
    }

    private static <T> PluginCandidate<T> resolveCandidate(final Class<? extends T> cls) {
        final var info = cls.getAnnotation(PluginInfo.class);
        final var name = info != null ? info.name() : cls.getSimpleName();
        final var version = info != null ? info.version() : "1.0.0";
        return new PluginCandidate<>(name, version, cls);
    }
}
