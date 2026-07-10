package io.github.khezyapp.pluginlib;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction for persisting and querying installed plugins.
 * <p>
 * Implementations manage the lifecycle of {@link InstalledPlugin} records,
 * including install, uninstall, lookup, enable/disable toggling, and bulk
 * retrieval. The store does <em>not</em> handle class loading or
 * instantiation — it is purely a metadata catalog.
 *
 * @see InMemoryPluginStore
 */
public interface PluginStore {

    /**
     * Installs the given plugin into the store. If a plugin with the same
     * name and version already exists it is replaced.
     *
     * @param plugin the plugin metadata to persist
     * @return the installed plugin (typically the same instance)
     */
    InstalledPlugin install(InstalledPlugin plugin);

    /**
     * Removes the plugin identified by the given name and version from the store.
     *
     * @param name    plugin name
     * @param version plugin version
     */
    void uninstall(String name,
                   String version);

    /**
     * Returns an immutable snapshot of every installed plugin in the store.
     *
     * @return a list of all installed plugins (never {@code null})
     */
    List<InstalledPlugin> list();

    /**
     * Looks up a single plugin by its exact name and version.
     *
     * @param name    plugin name
     * @param version plugin version
     * @return an {@link Optional} containing the plugin if found, or empty
     */
    Optional<InstalledPlugin> get(String name,
                                  String version);

    /**
     * Returns all installed versions of the plugin with the given name.
     *
     * @param name plugin name
     * @return a list of matching plugins (never {@code null})
     */
    List<InstalledPlugin> getAll(String name);

    /**
     * Returns the plugin with the highest version for the given name.
     * Version comparison follows semantic versioning (major.minor.patch).
     *
     * @param name plugin name
     * @return an {@link Optional} containing the latest version if any exist
     */
    Optional<InstalledPlugin> getLatest(String name);

    /**
     * Enables or disables the plugin identified by the given name and version.
     * When enabled the plugin is considered active; when disabled it is inactive.
     *
     * @param name    plugin name
     * @param version plugin version
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    void setEnabled(String name,
                    String version,
                    boolean enabled);

    /**
     * Removes every plugin from the store, leaving it empty.
     */
    void clear();
}
