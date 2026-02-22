package io.github.khezyapp.storageapi;

import java.time.Instant;

/**
 * Represents a snapshot of file information stored in a provider.
 *
 * @param path        The unique path within the storage
 * @param size        File size in bytes
 * @param contentType The MIME type assigned during upload
 * @param lastModified The timestamp of the last update
 * @param etag        A unique version identifier (useful for caching/S3)
 */
public record StorageMetadata(
        String path,
        long size,
        String contentType,
        Instant lastModified,
        String etag
) {
}
