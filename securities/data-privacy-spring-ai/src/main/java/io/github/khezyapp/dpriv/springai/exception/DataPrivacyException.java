package io.github.khezyapp.dpriv.springai.exception;

/**
 * Base class for all advisor failures (design §8.9). Consumers may catch this type to handle
 * any data-privacy failure uniformly.
 */
public class DataPrivacyException extends RuntimeException {

    public DataPrivacyException(final String message) {
        super(message);
    }

    public DataPrivacyException(final String message,
                                final Throwable cause) {
        super(message, cause);
    }
}
