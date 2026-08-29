package io.github.khezyapp.dpriv.internal;

import io.github.khezyapp.dpriv.api.GuardrailCheck;
import io.github.khezyapp.dpriv.api.GuardrailResult;
import io.github.khezyapp.dpriv.pipeline.StageResult;
import io.github.khezyapp.dpriv.redact.Redactor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Evaluates every {@link GuardrailCheck} in a stage concurrently on the common
 * {@link java.util.concurrent.ForkJoinPool} (the library owns no threads) and joins + collects the
 * results in stage order so the resulting {@link StageResult#validations()} is deterministic. A
 * check that throws is captured as an error message in {@link StageResult#errors()}; the runner
 * always returns (design §13 — failures are contained, never dropped, never a pass).
 */
public final class ParallelStageRunner {

    private final Redactor redactor;

    /**
     * Creates the runner backed by the given redactor (used to compute the stage's combined
     * {@code cleanedValue} from the merged masks).
     *
     * @param redactor the redactor; never null
     */
    public ParallelStageRunner(final Redactor redactor) {
        this.redactor = Objects.requireNonNull(redactor, "redactor");
    }

    /**
     * Runs every check over {@code input} in parallel and returns the aggregated stage result.
     *
     * @param stageName the stage name
     * @param checks    the checks to run, in stage order
     * @param input     the text to check
     * @return the aggregated stage result
     */
    public StageResult run(final String stageName,
                           final List<GuardrailCheck> checks,
                           final String input) {
        Objects.requireNonNull(stageName, "stageName");
        Objects.requireNonNull(checks, "checks");
        Objects.requireNonNull(input, "input");
        if (checks.isEmpty()) {
            return StageResult.aggregate(stageName, List.of(), input);
        }
        final var futures = checks.stream()
                .map(check -> CompletableFuture.supplyAsync(() -> check.run(input)))
                .toList();
        final var validations = new ArrayList<GuardrailResult>();
        final var errors = new ArrayList<String>();
        for (var i = 0; i < futures.size(); i++) {
            try {
                validations.add(futures.get(i).join());
            } catch (final CompletionException ex) {
                errors.add(errorMessage(checks.get(i), ex));
            }
        }
        final var cleanedValue = redactor.redact(input, StageResult.mergeMaskEntities(validations));
        return StageResult.aggregate(stageName, validations, cleanedValue, errors);
    }

    private static String errorMessage(final GuardrailCheck check,
                                       final Throwable throwable) {
        final var cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        final var message = cause.getMessage();
        return check.name() + " failed: " + (message != null ? message : cause.toString());
    }
}
