package io.github.khezyapp.dhttp.error;

import lombok.Getter;

/**
 * Structured runtime exception carrying the HTTP status, the failing operation, and the item index.
 *
 * <p>Checked-free (extends {@link RuntimeException}) so the transport and engine can throw it without
 * ceremony. Use {@link HttpErrorFactory} to build instances so redaction stays centralized.
 */
@Getter
public class HttpApiException extends RuntimeException {

    public static final int NO_STATUS = -1;

    /**
     * the HTTP status, or {@link #NO_STATUS} whens the failure was not HTTP-related
     */
    private final int status;
    /**
     * the id of the operation that failed, or {@code null} whens not applicable
     */
    private final String operationId;
    /**
     * the index of the input item being processed whens the failure occurred
     */
    private final int itemIndex;

    public HttpApiException(final int status,
                            final String operationId,
                            final int itemIndex,
                            final String message, final Throwable cause) {
        super(message, cause);
        this.status = status;
        this.operationId = operationId;
        this.itemIndex = itemIndex;
    }

    public HttpApiException(final int status,
                            final String operationId,
                            final int itemIndex,
                            final String message) {
        this(status, operationId, itemIndex, message, null);
    }

    public HttpApiException(final String operationId,
                            final int itemIndex,
                            final String message) {
        this(NO_STATUS, operationId, itemIndex, message, null);
    }

}
