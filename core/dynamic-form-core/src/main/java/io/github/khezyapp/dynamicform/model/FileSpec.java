package io.github.khezyapp.dynamicform.model;

import java.util.List;
import java.util.Objects;

/**
 * Upload policy for a {@code FILE} field.
 *
 * @param accept         allowed MIME types / extensions, empty means any type
 * @param maxBytes       per-file size cap, 0 means no limit
 * @param maxCount       maximum number of files for multi-file fields, 0 means unbounded
 * @param uploadProvider the name of a {@code FileUploadProvider} (falls back to the default)
 */
public record FileSpec(
        List<String> accept,
        long maxBytes,
        int maxCount,
        String uploadProvider
) {

    /**
     * Compact canonical constructor that normalises a null accept list.
     */
    public FileSpec {
        accept = Objects.nonNull(accept) ? List.copyOf(accept) : List.of();
    }

    /**
     * Creates a permissive policy accepting anything.
     *
     * @return a permissive spec
     */
    public static FileSpec any() {
        return new FileSpec(null, 0, 0, null);
    }

    /**
     * Creates a size/count-bounded policy.
     *
     * @param maxBytes the per-file size cap
     * @param maxCount the maximum file count
     * @param accept   the allowed MIME types
     * @return a new spec
     */
    public static FileSpec of(final long maxBytes,
                              final int maxCount,
                              final List<String> accept) {
        return new FileSpec(accept, maxBytes, maxCount, null);
    }

    /**
     * Creates a bounded policy with an explicit upload provider.
     *
     * @param maxBytes       the per-file size cap
     * @param maxCount       the maximum file count
     * @param accept         the allowed MIME types
     * @param uploadProvider the {@code FileUploadProvider} name
     * @return a new spec
     */
    public static FileSpec of(final long maxBytes,
                              final int maxCount,
                              final List<String> accept,
                              final String uploadProvider) {
        return new FileSpec(accept, maxBytes, maxCount, uploadProvider);
    }
}
