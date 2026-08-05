package io.github.khezyapp.ast.core.index.analyzer;

import io.github.khezyapp.ast.core.index.model.ExpressionIndexMetadata;
import io.github.khezyapp.ast.core.index.model.FunctionalColumn;
import io.github.khezyapp.ast.core.index.model.GinColumn;
import io.github.khezyapp.ast.core.index.model.NonIndexable;
import io.github.khezyapp.ast.core.index.model.PlainColumn;
import io.github.khezyapp.ast.core.model.Node;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry mapping function names to {@link ExpressionIndexResolver} instances.
 * <p>
 * Used by {@link AstIndexAnalyzer} to resolve AST expression nodes into
 * index metadata. Provides a default set of built-in resolvers for common
 * functions like {@code payload}, {@code to_tsvector}, {@code upper},
 * {@code lower}, and {@code trim}.
 * </p>
 */
public final class IndexResolverRegistry {

    private static final Set<String> B_TREE_TRANSFORMS = Set.of(
            "upper", "lower", "trim");

    private final Map<String, ExpressionIndexResolver> resolvers = new ConcurrentHashMap<>();

    /**
     * Registers a resolver for a function name.
     *
     * @param functionName the function name
     * @param resolver     the resolver implementation
     * @return this registry (for chaining)
     */
    public IndexResolverRegistry register(final String functionName,
                                          final ExpressionIndexResolver resolver) {
        Objects.requireNonNull(functionName);
        Objects.requireNonNull(resolver);
        resolvers.put(functionName, resolver);
        return this;
    }

    /**
     * Resolves a node to index metadata using the registered resolvers.
     *
     * @param node the AST expression node
     * @return the resolved index metadata
     */
    public ExpressionIndexMetadata resolve(final Node node) {
        if (node.isConstant()) {
            return new PlainColumn(node.constant().toString());
        }

        final var fn = node.function().value();
        final var resolver = resolvers.get(fn);
        if (Objects.nonNull(resolver)) {
            return resolver.resolve(node);
        }

        return new NonIndexable(node.toString());
    }

    /**
     * Creates a registry with the default built-in resolvers.
     * <p>
     * Includes resolvers for: {@code payload}, {@code field},
     * {@code to_tsvector}, {@code jsonb_extract_path_text},
     * {@code upper}, {@code lower}, {@code trim}.
     * </p>
     *
     * @return a new registry with built-in resolvers
     */
    public static IndexResolverRegistry withBuiltins() {
        final var registry = new IndexResolverRegistry();

        final ExpressionIndexResolver payloadResolver = node -> {
            final var fieldNameNode = node.namedChildren().get("fieldName");
            if (Objects.isNull(fieldNameNode)) {
                return new NonIndexable(node.toString());
            }
            if (fieldNameNode.isConstant()) {
                return new PlainColumn(fieldNameNode.constant().toString());
            }
            return resolveFieldName(fieldNameNode, registry);
        };

        registry.register("payload", payloadResolver);
        registry.register("field", payloadResolver);

        registry.register("to_tsvector", node -> {
            if (node.children().size() < 2) {
                return new NonIndexable(node.toString());
            }
            final var inner = registry.resolve(node.children().get(1));
            if (inner instanceof GinColumn gin) {
                return gin;
            }
            if (inner.isIndexable()) {
                return new GinColumn(inner.columnName(), "tsvector_ops");
            }
            return new NonIndexable(node.toString());
        });

        registry.register("jsonb_extract_path_text", node -> {
            if (node.children().isEmpty()) {
                return new NonIndexable(node.toString());
            }
            final var inner = registry.resolve(node.children().get(0));
            if (inner.isIndexable()) {
                return new GinColumn(inner.columnName(), "jsonb_ops");
            }
            return new NonIndexable(node.toString());
        });

        final ExpressionIndexResolver transformResolver = node -> {
            final var fn = node.function().value();
            if (node.children().isEmpty()) {
                return new NonIndexable(node.toString());
            }
            final var inner = registry.resolve(node.children().get(0));
            if (inner.isIndexable()) {
                return new FunctionalColumn(inner.columnName(), fn);
            }
            return new NonIndexable(node.toString());
        };

        for (final var fn : B_TREE_TRANSFORMS) {
            registry.register(fn, transformResolver);
        }

        return registry;
    }

    private static ExpressionIndexMetadata resolveFieldName(final Node node,
                                                            final IndexResolverRegistry registry) {
        if (node.isConstant()) {
            return new PlainColumn(node.constant().toString());
        }
        return registry.resolve(node);
    }
}
