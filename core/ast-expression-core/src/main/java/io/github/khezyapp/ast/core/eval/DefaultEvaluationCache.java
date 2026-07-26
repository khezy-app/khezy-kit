package io.github.khezyapp.ast.core.eval;

import io.github.khezyapp.ast.core.result.EvaluationResult;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Default thread-safe implementation of {@link EvaluationCache} backed by
 * a {@link java.util.concurrent.ConcurrentHashMap}.
 */
public final class DefaultEvaluationCache implements EvaluationCache {
    private final ConcurrentHashMap<Long, EvaluationResult> cache = new ConcurrentHashMap<>();

    @Override
    public void put(final long hash,
                    final EvaluationResult result) {
        cache.put(hash, result);
    }

    @Override
    public EvaluationResult get(final long hash) {
        return cache.get(hash);
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
