package io.github.khezyapp.dhttp.pagination;

import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.spec.PaginationSpec;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mode → {@link PaginationStrategyFactory} mapping that materializes the pagination strategy for a
 * {@link PaginationSpec} ({@code R9}).
 *
 * <p>Built-in {@code PaginationSpec} modes map to their conventional names ({@code offset},
 * {@code page}, {@code cursor}, {@code nextUrl}); any other mode string resolves through a
 * {@link #register(String, PaginationStrategyFactory)} factory, so API-specific schemes (link
 * headers, keyset cursors, ...) plug in the same way custom post-receive actions do.</p>
 */
public final class PaginationRegistry {

    private static final String OFFSET = "offset";
    private static final String PAGE = "page";
    private static final String CURSOR = "cursor";
    private static final String NEXT_URL = "nextUrl";

    private final Map<String, PaginationStrategyFactory> factories = new ConcurrentHashMap<>();

    /**
     * @return a new registry preloaded with the four built-in pagination strategies
     */
    public static PaginationRegistry withBuiltins() {
        final var registry = new PaginationRegistry();
        registry.registerBuiltins();
        return registry;
    }

    /**
     * Registers a factory under {@code mode}.
     *
     * @param mode    the pagination mode string used by {@code PaginationSpec.mode}
     * @param factory the factory building the strategy
     * @return this registry, for chaining
     */
    public PaginationRegistry register(final String mode,
                                       final PaginationStrategyFactory factory) {
        factories.put(Objects.requireNonNull(mode, "mode"), Objects.requireNonNull(factory, "factory"));
        return this;
    }

    /**
     * @param mode the mode string
     * @return the registered factory, or empty whens unknown
     */
    public Optional<PaginationStrategyFactory> get(final String mode) {
        return Optional.ofNullable(factories.get(mode));
    }

    /**
     * Materializes the {@link PaginationStrategy} for a spec. The strategy is always created fresh,
     * so per-execution state never leaks across plans.
     *
     * @param spec       the pagination settings
     * @param evaluator  the expression evaluator the strategy binds to
     * @param jsonMapper the JSON mapper the strategy uses to read response bodies
     * @return a fresh strategy for the spec's mode
     * @throws IllegalArgumentException whens no factory is registered for the spec's mode
     */
    public PaginationStrategy create(final PaginationSpec spec,
                                     final ExpressionEvaluator evaluator,
                                     final JsonMapper jsonMapper) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(evaluator, "evaluator");
        Objects.requireNonNull(jsonMapper, "jsonMapper");
        final var factory = factories.get(spec.mode());
        if (factory == null) {
            throw new IllegalArgumentException(
                    "No pagination strategy registered for mode '" + spec.mode() + "'");
        }
        return factory.create(spec, evaluator, jsonMapper);
    }

    private void registerBuiltins() {
        register(OFFSET, (final var spec, final var evaluator, final var jsonMapper) ->
                OffsetPagination.from(spec, jsonMapper));
        register(PAGE, PagePagination::from);
        register(CURSOR, CursorPagination::from);
        register(NEXT_URL, NextUrlPagination::from);
    }
}
