package io.github.khezyapp.storageapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface StorageService {
    /**
     * Uploads a file using an InputStream.
     *
     * @param path The destination path (e.g., "avatars/user-1.jpg")
     * @param inputStream The source data stream
     * @param contentType The MIME type (e.g., "image/jpeg")
     * @throws IOException If the transfer fails
     */
    void upload(String path,
                InputStream inputStream,
                String contentType) throws IOException;

    /**
     * Downloads a file as an InputStream.
     *
     * @param path The path to the file
     * @return An Optional containing the stream, or empty if not found
     */
    Optional<InputStream> download(String path) throws IOException;

    /**
     * Deletes a file from the storage provider.
     */
    void delete(String path) throws IOException;

    /**
     * Checks if a file exists at the given path.
     */
    boolean exists(String path);

    /**
     * Generates a temporary public URL for the file (useful for S3/GCS).
     * For local file systems, this might return a local server URL.
     */
    String getUrl(String path);

    /**
     * Generates a secure, time-limited URL for access.
     *
     * @param path The file path
     * @param options Security and expiration settings
     * @return The signed URL as a String
     */
    String getSignedUrl(String path, SignedUrlOptions options);

    /**
     * Retrieves descriptive information about a file without downloading the content.
     *
     * @param path The path to the file
     * @return StorageMetadata containing size, content-type, and timestamps
     * @throws IOException If the file is not found or metadata is inaccessible
     */
    StorageMetadata getMetadata(String path) throws IOException;
}
