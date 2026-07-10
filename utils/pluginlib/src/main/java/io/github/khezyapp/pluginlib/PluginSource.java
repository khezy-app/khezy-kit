package io.github.khezyapp.pluginlib;

import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Discriminated union that identifies where a plugin was loaded from.
 * <p>
 * A plugin may originate from three kinds of sources:
 * <ul>
 *   <li>{@link ClasspathSource} — discovered via {@link java.util.ServiceLoader}
 *       on the application classpath</li>
 *   <li>{@link FileSource} — loaded from a JAR file on the local filesystem</li>
 *   <li>{@link UrlSource} — loaded from a remote URL</li>
 * </ul>
 * This is a sealed interface; all permitted subtypes are declared inside this file.
 */
public sealed interface PluginSource {

    /**
     * Indicates that the plugin was discovered on the application classpath
     * (e.g. via {@link java.util.ServiceLoader}).
     */
    record ClasspathSource() implements PluginSource { }

    /**
     * Indicates that the plugin was loaded from a JAR file on the local filesystem.
     *
     * @param jarPath absolute or relative path to the JAR file
     */
    record FileSource(Path jarPath) implements PluginSource {
        /**
         * Compact canonical constructor that validates {@code jarPath} is non-{@code null}.
         */
        public FileSource {
            Objects.requireNonNull(jarPath, "jarPath must not be null");
        }
    }

    /**
     * Indicates that the plugin was loaded from a remote URL.
     *
     * @param url the remote URL from which the plugin JAR was retrieved
     */
    record UrlSource(URL url) implements PluginSource {
        /**
         * Compact canonical constructor that validates {@code url} is non-{@code null}.
         */
        public UrlSource {
            Objects.requireNonNull(url, "url must not be null");
        }
    }
}
