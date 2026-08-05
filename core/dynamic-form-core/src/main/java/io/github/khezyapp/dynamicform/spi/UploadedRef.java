package io.github.khezyapp.dynamicform.spi;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A stable reference to an uploaded object, produced by a {@link FileUploadProvider}.
 *
 * @param ref      the stable identifier used to retrieve the object
 * @param url      optional URL under which the object can be fetched
 * @param size     the size in bytes
 * @param mime     the MIME type of the object
 * @param checksum the SHA-256 checksum of the object bytes
 */
public record UploadedRef(String ref, String url, long size, String mime, String checksum) {

    /**
     * Creates a reference, deriving {@code size} and {@code checksum} from the raw bytes.
     *
     * @param ref   the stable identifier
     * @param url   the fetch URL, may be {@code null}
     * @param bytes the object bytes
     * @param mime  the MIME type
     * @return a fully populated reference
     */
    public static UploadedRef of(final String ref,
                                 final String url,
                                 final byte[] bytes,
                                 final String mime) {
        return new UploadedRef(ref, url, bytes.length, mime, sha256(bytes));
    }

    private static String sha256(final byte[] bytes) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            final var hex = new StringBuilder();
            for (final byte b : digest.digest(bytes)) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
