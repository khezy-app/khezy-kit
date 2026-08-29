package io.github.khezyapp.dpriv.pipeline;

import io.github.khezyapp.dpriv.api.GuardrailResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The aggregated result of one pipeline stage (design §6). Immutable. Holds the per-check
 * {@link GuardrailResult}s in stage order, the merged {@code maskEntities} (unique, first-seen
 * order across checks), the stage's combined {@code cleanedValue}, the stage verdict, and any
 * captured check errors (design §13 — a thrown check is recorded here, never dropped).
 *
 * @param stageName     the stage name (e.g. {@code "preflight"}, {@code "classify"})
 * @param validations   the per-check results, in stage order
 * @param maskEntities  the merged {@code entityType -> tokens} map
 * @param cleanedValue  the combined sanitized text for the stage
 * @param passed        whether no check in the stage flagged content
 * @param errors        captured error messages for checks that threw, in stage order
 */
public record StageResult(
        String stageName,
        List<GuardrailResult> validations,
        Map<String, List<String>> maskEntities,
        String cleanedValue,
        boolean passed,
        List<String> errors) {

    /**
     * Canonical constructor: rejects null components and freezes the collections.
     */
    public StageResult {
        stageName = Objects.requireNonNull(stageName, "stageName");
        validations = List.copyOf(Objects.requireNonNull(validations, "validations"));
        maskEntities = Objects.requireNonNull(maskEntities, "maskEntities");
        errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
        if (Objects.isNull(cleanedValue)) {
            cleanedValue = "";
        }
    }

    /**
     * Whether any check in the stage flagged content.
     *
     * @return true when the stage detected something
     */
    public boolean detected() {
        return !passed;
    }

    /**
     * Builds a stage result from per-check validations, deriving {@code maskEntities} and
     * {@code passed} from them and using the supplied {@code cleanedValue}. No errors are recorded.
     *
     * @param stageName     the stage name
     * @param validations   the per-check results, in stage order
     * @param cleanedValue  the combined sanitized text
     * @return the assembled stage result
     */
    public static StageResult aggregate(final String stageName,
                                        final List<GuardrailResult> validations,
                                        final String cleanedValue) {
        return aggregate(stageName, validations, cleanedValue, List.of());
    }

    /**
     * Builds a stage result from per-check validations and a captured error list, deriving
     * {@code maskEntities} and {@code passed} from the validations.
     *
     * @param stageName     the stage name
     * @param validations   the per-check results, in stage order
     * @param cleanedValue  the combined sanitized text
     * @param errors        captured error messages for checks that threw
     * @return the assembled stage result
     */
    public static StageResult aggregate(final String stageName,
                                        final List<GuardrailResult> validations,
                                        final String cleanedValue,
                                        final List<String> errors) {
        final var masks = mergeMaskEntities(validations);
        final var flagged = validations.stream().anyMatch(GuardrailResult::detected);
        return new StageResult(stageName, validations, masks, cleanedValue, !flagged, errors);
    }

    /**
     * Merges the {@code maskEntities} of several checks, preserving unique-first-seen token order
     * per entity type and stage order across entity types.
     *
     * @param validations the per-check results
     * @return the merged, immutable map
     */
    public static Map<String, List<String>> mergeMaskEntities(final List<GuardrailResult> validations) {
        final var ordered = new LinkedHashMap<String, List<String>>();
        for (final var result : validations) {
            for (final var entry : result.maskEntities().entrySet()) {
                ordered.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).addAll(entry.getValue());
            }
        }
        final var merged = new LinkedHashMap<String, List<String>>();
        for (final var entry : ordered.entrySet()) {
            merged.put(entry.getKey(), List.copyOf(new LinkedHashSet<>(entry.getValue())));
        }
        return Map.copyOf(merged);
    }
}
