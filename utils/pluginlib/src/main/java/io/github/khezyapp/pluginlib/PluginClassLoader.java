package io.github.khezyapp.pluginlib;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

/**
 * A {@link URLClassLoader} variant that applies a <em>delegate-first</em>
 * strategy for selected class-name prefixes.
 * <p>
 * Standard {@link URLClassLoader} behaviour is to check locally loaded JARs
 * before delegating to the parent. {@code PluginClassLoader} inverts this for
 * classes whose fully qualified name starts with any of the configured
 * prefixes (by default {@code java.}, {@code javax.}, {@code jdk.},
 * {@code sun.}), ensuring that core JDK classes are always resolved by the
 * parent loader.
 * <p>
 * This avoids conflicts when plugin JARs bundle their own copies of standard
 * library classes.
 */
public class PluginClassLoader extends URLClassLoader {

    private static final String[] DEFAULT_DELEGATE_FIRST = {
            "java.", "javax.", "jdk.", "sun."
    };

    private final String[] delegateFirstPrefixes;

    /**
     * Constructs a new {@code PluginClassLoader} for the given URLs.
     *
     * @param urls                  the URLs from which to load classes and resources
     * @param parent                the parent class loader for delegation
     * @param delegateFirstPrefixes class-name prefixes that should always be
     *                              loaded by the parent first; if none are supplied,
     *                              the default set ({@code java.}, {@code javax.},
     *                              {@code jdk.}, {@code sun.}) is used
     */
    public PluginClassLoader(final URL[] urls,
                             final ClassLoader parent,
                             final String... delegateFirstPrefixes) {
        super(urls, parent);
        this.delegateFirstPrefixes = delegateFirstPrefixes.length > 0
                ? delegateFirstPrefixes
                : DEFAULT_DELEGATE_FIRST;
    }

    /**
     * Loads a class with the standard delegation model but applies the
     * delegate-first policy: classes whose name matches one of the
     * {@code delegateFirstPrefixes} are first delegated to the parent loader.
     * <p>
     * The load order is:
     * <ol>
     *   <li>Check if the class is already loaded</li>
     *   <li>If the name matches a delegate-first prefix, try the parent loader</li>
     *   <li>Try to find the class locally via {@link #findClass(String)}</li>
     *   <li>Fall back to {@link URLClassLoader#loadClass(String, boolean)}</li>
     * </ol>
     *
     * @param name    the binary name of the class
     * @param resolve if {@code true}, resolve the class
     * @return the resulting {@link Class} object
     * @throws ClassNotFoundException if the class could not be found
     */
    @Override
    public Class<?> loadClass(final String name,
                              final boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }

            if (isDelegateFirst(name)) {
                try {
                    c = getParent().loadClass(name);
                    if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                } catch (final ClassNotFoundException e) {
                    // fall through to local
                }
            }

            try {
                c = findClass(name);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            } catch (final ClassNotFoundException e) {
                // fall through to parent
            }

            return super.loadClass(name, resolve);
        }
    }

    /**
     * Checks whether the given class name starts with any of the configured
     * delegate-first prefixes.
     */
    private boolean isDelegateFirst(final String className) {
        return Arrays.stream(delegateFirstPrefixes)
                .anyMatch(className::startsWith);
    }
}
