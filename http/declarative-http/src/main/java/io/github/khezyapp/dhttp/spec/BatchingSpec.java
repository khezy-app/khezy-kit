package io.github.khezyapp.dhttp.spec;

/**
 * Batching throttle settings (§10.2, mirrors n8n {@code requestOptions.batching.batch}).
 *
 * <p>Each input item still produces its own request; the engine merely paces the loop by sleeping
 * {@code batchIntervalMillis} before every {@code batchSize}-th item.
 *
 * @param batchSize          the number of items per batch; must be positive
 * @param batchIntervalMillis the pause in milliseconds between batches; zero disables pacing
 */
public record BatchingSpec(int batchSize,
                           long batchIntervalMillis) {

    public BatchingSpec {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (batchIntervalMillis < 0) {
            throw new IllegalArgumentException("batchIntervalMillis must be non-negative");
        }
    }
}
