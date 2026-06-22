package io.github.khezyapp.templates.runner;

/**
 * Exception thrown when a shell command fails to execute, times out,
 * returns a non-zero exit code, is blocked by security policy, or is
 * interrupted.
 */
public class ShellExecutionException extends Exception {

    /**
     * Creates a new ShellExecutionException with a detail message.
     *
     * @param message the detail message
     */
    public ShellExecutionException(final String message) {
        super(message);
    }

    /**
     * Creates a new ShellExecutionException with a detail message and cause.
     *
     * @param message the detail message
     * @param cause   the root cause
     */
    public ShellExecutionException(final String message,
                                   final Throwable cause) {
        super(message, cause);
    }
}
