package io.github.khezyapp.dynamicform.model;

import java.util.Objects;

/**
 * A single validation or coercion problem, addressed by a dot-path into the resolved values.
 * <p>
 * The same shape the backend returns on a rejected submit is what the UI renders inline — one
 * validation language for both sides (P7).
 *
 * @param path     the value path (e.g. {@code "documents.idNumber"} or {@code "directors[2].idNumber"})
 * @param message  the human-readable problem
 * @param severity the issue severity, defaults to {@link Severity#ERROR}
 */
public record FieldIssue(String path, String message, Severity severity) {

    /**
     * Compact canonical constructor that defaults the severity to {@code ERROR}.
     */
    public FieldIssue {
        path = Objects.requireNonNull(path, "path must not be null");
        message = Objects.requireNonNull(message, "message must not be null");
        severity = Objects.nonNull(severity) ? severity : Severity.ERROR;
    }

    /**
     * Creates an error issue.
     *
     * @param path    the value path
     * @param message the problem
     * @return an error issue
     */
    public static FieldIssue error(final String path,
                                   final String message) {
        return new FieldIssue(path, message, Severity.ERROR);
    }

    /**
     * Creates a warning issue.
     *
     * @param path    the value path
     * @param message the problem
     * @return a warning issue
     */
    public static FieldIssue warning(final String path,
                                     final String message) {
        return new FieldIssue(path, message, Severity.WARNING);
    }
}
