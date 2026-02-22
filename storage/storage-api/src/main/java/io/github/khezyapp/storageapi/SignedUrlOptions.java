package io.github.khezyapp.storageapi;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration options for generating a time-limited, secure access URL.
 *
 * @param expiration The duration for which the generated URL remains valid.
 * @param readOnly If true, restricts the URL to read operations only;
 * otherwise allows operations based on provider permissions.
 * @param customHeaders A map of additional HTTP headers (e.g., Content-Disposition, Cache-Control)
 * to be included in the signed request.
 */
public record SignedUrlOptions(
        Duration expiration,
        boolean readOnly,
        Map<String, String> customHeaders
) {

    /**
     * A convenience factory for creating a standard read-only signature.
     *
     * @param duration Validity period
     * @return A default read-only SignedUrlOptions instance
     */
    public static SignedUrlOptions defaultRead(final Duration duration) {
        return new SignedUrlOptions(duration, true, Map.of());
    }
}
