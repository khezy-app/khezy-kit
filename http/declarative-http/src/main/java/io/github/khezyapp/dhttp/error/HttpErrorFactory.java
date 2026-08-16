package io.github.khezyapp.dhttp.error;

import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Central factory for {@link HttpApiException}. The single place error messages are built so token
 * redaction can be applied consistently.
 */
public final class HttpErrorFactory {

    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)Bearer\\s+[\\w.\\-]+");

    private HttpErrorFactory() {
    }

    public static HttpApiException of(final int status,
                                      final String operationId,
                                      final int itemIndex,
                                      final String message) {
        return new HttpApiException(status, operationId, itemIndex, redact(message));
    }

    public static HttpApiException of(final int status,
                                      final String operationId,
                                      final int itemIndex,
                                      final String message,
                                      final Throwable cause) {
        return new HttpApiException(status, operationId, itemIndex, redact(message), cause);
    }

    /**
     * Builds an exception from a non-2xx {@link HttpResult}, deriving status and message.
     *
     * @param operationId the failing operation id
     * @param itemIndex   the input item index
     * @param result      the non-2xx response
     */
    public static HttpApiException http(final String operationId,
                                        final int itemIndex,
                                        final HttpResult result) {
        Objects.requireNonNull(result, "result");
        final var message = redact("HTTP " + result.status() + " (non-2xx) for operation '"
                + operationId + "' at item " + itemIndex);
        return new HttpApiException(result.status(), operationId, itemIndex, message);
    }

    private static String redact(final String message) {
        if (Objects.isNull(message)) {
            return null;
        }
        return BEARER_TOKEN.matcher(message).replaceAll("Bearer [REDACTED]");
    }
}
