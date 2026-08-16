package io.github.khezyapp.dhttp.action;

import io.github.khezyapp.dhttp.action.builtin.*;
import io.github.khezyapp.dhttp.expr.ExpressionEvaluator;
import io.github.khezyapp.dhttp.spec.PostReceive;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Name → {@link PostReceiveFactory} mapping that materializes {@link PostReceiveAction}s from
 * {@link PostReceive} descriptors ({@code R7}).
 *
 * <p>Built-in post-receive descriptors map to their conventional names
 * ({@code rootProperty}, {@code filter}, {@code limit}, {@code setValue}, {@code sortByKey},
 * {@code setKeyValue}, {@code binaryData}); {@code CustomPostReceive} descriptors resolve through
 * their {@code actionKey}.</p>
 */
public final class ActionRegistry {

    private static final String ROOT_PROPERTY = "rootProperty";
    private static final String FILTER = "filter";
    private static final String LIMIT = "limit";
    private static final String SET_VALUE = "setValue";
    private static final String SORT_BY_KEY = "sortByKey";
    private static final String SET_KEY_VALUE = "setKeyValue";
    private static final String BINARY_DATA = "binaryData";

    private final Map<String, PostReceiveFactory> factories = new ConcurrentHashMap<>();

    /**
     * @return a new registry preloaded with the seven built-in post-receive actions
     */
    public static ActionRegistry withBuiltins() {
        final var registry = new ActionRegistry();
        registry.registerBuiltins();
        return registry;
    }

    /**
     * Registers a factory under {@code name}.
     *
     * @param name    the canonical action name (used by {@code CustomPostReceive.actionKey})
     * @param factory the factory building the action
     * @return this registry, for chaining
     */
    public ActionRegistry register(final String name,
                                   final PostReceiveFactory factory) {
        factories.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(factory, "factory"));
        return this;
    }

    /**
     * @param name the factory name
     * @return the registered factory, or empty whens unknown
     */
    public Optional<PostReceiveFactory> get(final String name) {
        return Optional.ofNullable(factories.get(name));
    }

    /**
     * Materializes the {@link PostReceiveAction} for a descriptor: sealed built-in variants map to
     * their named factories; {@code CustomPostReceive} resolves through its {@code actionKey}.
     *
     * @param descriptor the post-receive descriptor
     * @param evaluator  the expression evaluator the action binds to
     * @return the concrete action
     */
    public PostReceiveAction create(final PostReceive descriptor,
                                    final ExpressionEvaluator evaluator) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(evaluator, "evaluator");
        final var name = nameOf(descriptor);
        final var factory = factories.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("No post-receive action registered for '" + name + "'");
        }
        return factory.create(descriptor, evaluator);
    }

    private void registerBuiltins() {
        registerRootProperty();
        registerFilter();
        registerLimit();
        registerSetValue();
        registerSortByKey();
        registerSetKeyValue();
        registerBinaryData();
    }

    private void registerRootProperty() {
        register(ROOT_PROPERTY, (final var descriptor, final var evaluator) -> RootProperty.from(descriptor));
    }

    private void registerFilter() {
        register(FILTER, FilterItems::from);
    }

    private void registerLimit() {
        register(LIMIT, (final var descriptor, final var evaluator) -> LimitItems.from(descriptor));
    }

    private void registerSetValue() {
        register(SET_VALUE, SetValue::from);
    }

    private void registerSortByKey() {
        register(SORT_BY_KEY, (final var descriptor, final var evaluator) -> SortByKey.from(descriptor));
    }

    private void registerSetKeyValue() {
        register(SET_KEY_VALUE, SetKeyValue::from);
    }

    private void registerBinaryData() {
        register(BINARY_DATA, (final var descriptor, final var evaluator) -> BinaryData.from(descriptor));
    }

    private static String nameOf(final PostReceive descriptor) {
        if (descriptor instanceof PostReceive.RootProperty) {
            return ROOT_PROPERTY;
        }
        if (descriptor instanceof PostReceive.FilterItems) {
            return FILTER;
        }
        if (descriptor instanceof PostReceive.LimitItems) {
            return LIMIT;
        }
        if (descriptor instanceof PostReceive.SetValue) {
            return SET_VALUE;
        }
        if (descriptor instanceof PostReceive.SortByKey) {
            return SORT_BY_KEY;
        }
        if (descriptor instanceof PostReceive.SetKeyValue) {
            return SET_KEY_VALUE;
        }
        if (descriptor instanceof PostReceive.BinaryData) {
            return BINARY_DATA;
        }
        if (descriptor instanceof PostReceive.CustomPostReceive custom) {
            return custom.actionKey();
        }
        throw new IllegalArgumentException("Unknown post-receive descriptor: " + descriptor);
    }
}
