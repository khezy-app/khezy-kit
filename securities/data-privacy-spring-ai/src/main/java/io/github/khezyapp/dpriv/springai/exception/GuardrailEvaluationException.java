package io.github.khezyapp.dpriv.springai.exception;

/**
 * The judge itself failed (LLM unreachable, malformed verdict) and failOnError=true (design §8.8,
 * G15). Infra problem — retry with backoff may be appropriate.
 */
public final class GuardrailEvaluationException extends DataPrivacyException {

    public GuardrailEvaluationException(final String message,
                                        final Throwable cause) {
        super(message, cause);
    }
}
