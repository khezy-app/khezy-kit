package io.github.khezyapp.dhttp.expr.jexl;

import io.github.khezyapp.dhttp.expr.DoaNamespace;
import io.github.khezyapp.dhttp.expr.EvaluationScope;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.github.khezyapp.doa.adapters.BeanAdapter;
import io.github.khezyapp.doa.adapters.ListAdapter;
import io.github.khezyapp.doa.adapters.MapAdapter;
import io.github.khezyapp.doa.adapters.RecordAdapter;
import io.github.khezyapp.doa.builder.AccessorFactoryImpl;
import io.github.khezyapp.doa.cache.MapCache;
import io.github.khezyapp.doa.engine.DefaultPathParser;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;

/**
 * Creates and memoizes {@link JexlEngine} instances. Every engine gets the {@code doa} namespace
 * (the dynamic-object bridge) registered, so consumers only add their own namespaces/options.
 */
public final class JexlEngineFactory {

    private static final Map<String, Object> DEFAULT_NAMESPACES = Map.of(
            "doa", new DoaNamespace(
                    new AccessorFactoryImpl()
                            .registerAdapter(new MapAdapter())
                            .registerAdapter(new BeanAdapter(
                                    new MapCache<>(250),
                                    new MapCache<>(250)
                            ))
                            .registerAdapter(new RecordAdapter(new MapCache<>(250)))
                            .registerAdapter(new ListAdapter())
                            .withParser(new DefaultPathParser(new MapCache<>(250)))
                            .build())
    );

    private static final JexlEngine DEFAULT = create(builder -> { });

    private static final ConcurrentHashMap<ScopeCustomizer, JexlEngine> CACHE =
            new ConcurrentHashMap<>();

    private JexlEngineFactory() {
    }

    /**
     * @return the shared engine with the {@code doa} namespace and expression caching enabled
     */
    public static JexlEngine defaultEngine() {
        return DEFAULT;
    }

    /**
     * Builds (or reuses for the same {@code customizer}) an engine customized by the caller, always
     * including the {@code doa} namespace.
     *
     * @param customizer configures namespaces/options on the builder, or {@code null} for defaults
     * @return the memoized engine
     */
    public static JexlEngine cached(final ScopeCustomizer customizer) {
        if (Objects.isNull(customizer)) {
            return DEFAULT;
        }
        return CACHE.computeIfAbsent(customizer, JexlEngineFactory::create);
    }

    /**
     * @param scope the bindings to expose to JEXL
     * @return a fresh {@link MapContext} containing the scope's bindings (per-evaluation isolation)
     */
    public static MapContext context(final EvaluationScope scope) {
        final var ctx = new MapContext();
        for (final var e : scope.bindings().entrySet()) {
            ctx.set(e.getKey(), e.getValue());
        }
        return ctx;
    }

    private static JexlEngine create(final ScopeCustomizer customizer) {
        final var builder = new JexlBuilder();
        builder.cache(512);
        builder.permissions(JexlPermissions.UNRESTRICTED);
        if (Objects.nonNull(customizer)) {
            customizer.customize(builder);
        }
        final var namespaces = new HashMap<>(builder.namespaces());
        namespaces.putAll(DEFAULT_NAMESPACES);
        builder.namespaces(namespaces);
        return builder.create();
    }

    /**
     * Configures a {@link JexlBuilder} before the engine is created.
     */
    @FunctionalInterface
    public interface ScopeCustomizer {

        void customize(JexlBuilder builder);
    }
}
