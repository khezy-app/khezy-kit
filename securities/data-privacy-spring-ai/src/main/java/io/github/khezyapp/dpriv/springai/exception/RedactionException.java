package io.github.khezyapp.dpriv.springai.exception;

/**
 * The SANITIZE pipeline failed (a check errored) and failOnError=true: the request was aborted
 * before the model call — unredacted text was never sent (design §8.4, G10).
 */
public final class RedactionException extends DataPrivacyException {

    public RedactionException(final String message,
                              final Throwable cause) {
        super(message, cause);
    }
}
