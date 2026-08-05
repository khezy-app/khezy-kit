package io.github.khezyapp.ast.core.eval;

import io.github.khezyapp.ast.core.result.EvaluationResult;

/**
 * Cache interface for storing and retrieving evaluated AST node results.
 * <p>
 * Implementations should be thread-safe. The cache key is the hash of the
 * AST {@link io.github.khezyapp.ast.core.model.Node}. Caching avoids
 * re-evaluating identical subtrees during a single evaluation pass.
 * </p>
 */
public interface EvaluationCache {

    /**
     * Stores an evaluation result by node hash.
     *
     * @param hash   the node hash code
     * @param result the evaluation result
     */
    void put(long hash, EvaluationResult result);

    /**
     * Retrieves a cached evaluation result.
     *
     * @param hash the node hash code
     * @return the cached result, or {@code null} if not cached
     */
    EvaluationResult get(long hash);

    /**
     * Clears all cached results.
     */
    void clear();
}
