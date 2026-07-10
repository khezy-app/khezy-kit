package io.github.khezyapp.pluginlib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares metadata for a plugin service implementation.
 * <p>
 * When a plugin provider class is annotated with {@code @PluginInfo},
 * the metadata (name, version, description, vendor) is extracted
 * automatically by {@link ServiceLoaderPluginLoader} and
 * {@link DirectoryPluginLoader} instead of falling back to defaults.
 * <p>
 * This annotation is retained at runtime so that it can be read via
 * reflection during plugin discovery.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginInfo {

    /**
     * The unique name of the plugin.
     *
     * @return plugin name
     */
    String name();

    /**
     * The semantic version of the plugin (defaults to {@code "1.0.0"}).
     *
     * @return version string
     */
    String version() default "1.0.0";

    /**
     * A short human-readable description of what the plugin does.
     *
     * @return description text
     */
    String description() default "";

    /**
     * The organisation or individual that created the plugin.
     *
     * @return vendor name
     */
    String vendor() default "";
}
