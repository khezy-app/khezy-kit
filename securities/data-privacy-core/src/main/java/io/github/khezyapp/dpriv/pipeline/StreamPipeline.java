package io.github.khezyapp.dpriv.pipeline;

import io.github.khezyapp.dpriv.api.AuditRecord;
import io.github.khezyapp.dpriv.api.GuardrailCheck;
import io.github.khezyapp.dpriv.api.GuardrailsConfig;
import io.github.khezyapp.dpriv.api.ScanOutcome;
import io.github.khezyapp.dpriv.api.StreamCheck;
import io.github.khezyapp.dpriv.redact.StreamRedactor;
import io.github.khezyapp.dpriv.stream.MatchAccumulator;
import io.github.khezyapp.dpriv.stream.TextChunker;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The streaming half of the facade (design §10.2, §12.1). Implements the two-pass model over a
 * {@link Reader}: pass 1 runs every streamable deterministic {@link GuardrailCheck} (obtained via
 * {@link GuardrailCheck#toStream()}) concurrently on the common {@link java.util.concurrent.ForkJoinPool}
 * — each check owns its own {@link MatchAccumulator} — and merges the per-family matches; pass 2
 * feeds the merged {@code maskEntities} to the {@link StreamRedactor}. The classificatory (LLM)
 * stage never participates: a check whose {@link GuardrailCheck#toStream()} throws
 * {@link UnsupportedOperationException} is skipped, so {@code scan(Reader)} / {@code redact(Reader, Writer)}
 * are deterministic-only and parity-equal to the in-memory paths.
 *
 * <p>The library never closes caller-owned {@link Reader}s or {@link Writer}s; the redacting writer is
 * flushed on success.
 */
public final class StreamPipeline {

    private final List<StreamCheck> streamChecks;

    /**
     * Builds the streaming pipeline from the same stage lists the in-memory {@link GuardrailPipeline}
     * uses. Only checks that support streaming run; the rest (notably {@code LlmCheck}) are excluded.
     *
     * @param config           the policy; never null
     * @param preflight        the deterministic preflight checks, in stage order; never null
     * @param classificatory   the LLM-as-judge checks (unused by streaming); never null
     */
    public StreamPipeline(final GuardrailsConfig config,
                          final List<GuardrailCheck> preflight,
                          final List<GuardrailCheck> classificatory) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(preflight, "preflight");
        Objects.requireNonNull(classificatory, "classificatory");
        final var checks = new ArrayList<StreamCheck>();
        for (final var check : preflight) {
            try {
                checks.add(check.toStream());
            } catch (final UnsupportedOperationException ignored) {
                // non-streamable checks (e.g. LlmCheck) never participate in streaming
            }
        }
        this.streamChecks = List.copyOf(checks);
    }

    /**
     * Scans a streaming input (pass 1 only), assembling a {@link ScanOutcome} from the merged
     * streamable checks. Produces the same {@code entityTypes} / {@code maskEntities} / {@code
     * auditRecords} derivation as the in-memory {@code scan(String)}.
     *
     * @param input the streaming input; never null
     * @return the scan outcome
     */
    public ScanOutcome scan(final Reader input) {
        Objects.requireNonNull(input, "input");
        final var text = readFully(input);
        if (streamChecks.isEmpty()) {
            return new ScanOutcome(text, null, false, List.of(), Map.of(), List.of());
        }
        final var futures = streamChecks.stream()
                .map(check -> CompletableFuture.supplyAsync(() -> {
                    final var sink = new MatchAccumulator();
                    check.scan(new StringReader(text), sink);
                    return sink;
                }))
                .toList();
        final var merged = new MatchAccumulator();
        final var errors = new ArrayList<String>();
        for (var i = 0; i < futures.size(); i++) {
            try {
                final var sink = futures.get(i).join();
                for (final var entry : sink.toMaskEntities().entrySet()) {
                    merged.addAll(entry.getKey(), entry.getValue());
                }
            } catch (final CompletionException ex) {
                errors.add(errorMessage(streamChecks.get(i), ex));
            }
        }
        final var maskEntities = merged.toMaskEntities();
        return new ScanOutcome(
                text,
                primaryEntityType(maskEntities),
                !maskEntities.isEmpty(),
                List.copyOf(errors),
                maskEntities,
                auditRecords(merged, text)
        );
    }

    /**
     * Redacts a streaming input: pass 1 scans to build the full {@code maskEntities}, pass 2 streams
     * the redaction through {@link StreamRedactor}. Deterministic and parity-equal to the in-memory
     * {@code redact(String)}. Fail-closed (G4): throws when any stream check errored, because the
     * output would be under-redacted.
     *
     * @param input the streaming input; never null
     * @param out   the destination writer; never null (not closed by this method)
     * @throws IllegalStateException if a stream check failed (design §13)
     */
    public void redact(final Reader input,
                       final Writer out) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(out, "out");
        final var text = readFully(input);
        final var outcome = scan(new StringReader(text));
        if (!outcome.errorMessages().isEmpty()) {
            throw new IllegalStateException("redaction aborted on check error: " + outcome.errorMessages());
        }
        try {
            final var redactor = new StreamRedactor(new TextChunker(new StringReader(text)), outcome.maskEntities());
            redactor.redact(out);
            out.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException("failed to stream-redact input", e);
        }
    }

    private static String readFully(final Reader reader) {
        final var builder = new StringBuilder();
        final var buffer = new char[8192];
        var read = 0;
        try {
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("failed to read streaming input", e);
        }
        return builder.toString();
    }

    private static String primaryEntityType(final Map<String, List<String>> masks) {
        return masks.keySet().stream().findFirst().orElse(null);
    }

    private static List<AuditRecord> auditRecords(final MatchAccumulator merged,
                                                  final String text) {
        final var records = new ArrayList<AuditRecord>();
        for (final var entityType : merged.entityTypes()) {
            records.add(new AuditRecord(entityType, List.of(), text));
        }
        return List.copyOf(records);
    }

    private static String errorMessage(final StreamCheck check,
                                       final Throwable throwable) {
        final var cause = Objects.nonNull(throwable.getCause()) ? throwable.getCause() : throwable;
        final var message = cause.getMessage();
        return check.name() + " failed: " + (Objects.nonNull(message) ? message : cause.toString());
    }
}
