package io.github.khezyapp.dynamicform.spi;

import io.github.khezyapp.dynamicform.engine.EvalContext;
import io.github.khezyapp.dynamicform.model.FieldSchema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default in-memory {@link FileUploadProvider}.
 * <p>
 * Bytes are kept in a {@link ConcurrentHashMap} keyed by a generated reference, so FILE fields work
 * out of the box without a storage backend. It is intended for prototyping and testing — real
 * consumers register their own storage provider.
 */
public final class InMemoryFileUploadProvider implements FileUploadProvider {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    @Override
    public UploadedRef save(final byte[] bytes,
                            final String fileName,
                            final String mime,
                            final FieldSchema field,
                            final EvalContext ctx) {
        final var ref = field.name() + "-" + counter.incrementAndGet();
        this.store.put(ref, bytes.clone());
        return UploadedRef.of(ref, "inmem://" + ref, bytes, mime);
    }

    /**
     * Retrieves the raw bytes of a previously saved reference.
     *
     * @param ref the stable reference returned by {@link #save}
     * @return the stored bytes, or {@code null} if the reference is unknown
     */
    public byte[] retrieve(final String ref) {
        return this.store.get(ref);
    }
}
