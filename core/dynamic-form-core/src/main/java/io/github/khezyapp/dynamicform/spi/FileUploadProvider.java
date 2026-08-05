package io.github.khezyapp.dynamicform.spi;

import io.github.khezyapp.dynamicform.engine.EvalContext;
import io.github.khezyapp.dynamicform.model.FieldSchema;

/**
 * Storage backend for {@code FILE} field uploads.
 * <p>
 * Consumers implement this SPI to persist bytes (S3, GCS, disk, DB…) and return a stable reference.
 * The engine validates {@code accept} / {@code maxBytes} / {@code maxCount} before persisting, then
 * stores the returned {@link UploadedRef} as the field's value. The core ships an in-memory
 * implementation ({@link InMemoryFileUploadProvider}) as the default.
 */
@FunctionalInterface
public interface FileUploadProvider {

    /**
     * Persists the raw bytes and returns a stable reference to the stored object.
     *
     * @param bytes    the raw file content
     * @param fileName the client-provided file name
     * @param mime     the declared MIME type
     * @param field    the owning FILE field (for policy and tracing)
     * @param ctx      the evaluation context
     * @return the stable reference to the stored object
     */
    UploadedRef save(byte[] bytes, String fileName, String mime, FieldSchema field, EvalContext ctx);
}
