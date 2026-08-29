package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.GuardrailCheck;
import io.github.khezyapp.dpriv.api.GuardrailResult;
import io.github.khezyapp.dpriv.api.PiiConfig;
import io.github.khezyapp.dpriv.api.PiiCoverage;
import io.github.khezyapp.dpriv.api.StreamCheck;
import io.github.khezyapp.dpriv.policy.PiiEntity;
import io.github.khezyapp.dpriv.policy.PiiPatterns;
import io.github.khezyapp.dpriv.redact.Redactor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The big "detect" PII guardrail check (design §9.1). Resolves coverage
 * ({@code ALL} = the full catalog, {@code SELECTED} = the configured entity set), scans each
 * entity's pattern over the input, validates every candidate against {@link PiiConfig#strict()}
 * (checksum-backed entities rejected when strict), and accumulates unique-first-seen tokens under
 * each {@link PiiEntity#type()} key.
 *
 * <p>The result is a single aggregated {@link GuardrailResult} with {@code entityType == "pii"},
 * {@code maskEntities = { "pii_email": [...], "pii_credit_card": [...], ... }}. The configured
 * {@link CustomRegexCheck} groups are folded into the same aggregate (their own keys, never inside
 * {@code pii_*}), and one redaction pass produces the shared {@code cleanedValue}. This is the
 * single entry point Task 09 (streaming) and Task 10 (pipeline) use for PII.
 */
public final class PiiCheck implements GuardrailCheck {

    private static final String ENTITY_TYPE = "pii";

    private final PiiConfig config;
    private final Redactor redactor;
    private final CustomRegexCheck customRegexCheck;

    /**
     * Creates the check for the given policy and redactor.
     *
     * @param config   the PII policy (coverage, entities, custom regexes, strict); never null
     * @param redactor the redactor used to compute the aggregated {@code cleanedValue}; never null
     */
    public PiiCheck(final PiiConfig config,
                    final Redactor redactor) {
        this.config = Objects.requireNonNull(config, "config");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
        this.customRegexCheck = new CustomRegexCheck(config.customRegexes(), redactor);
    }

    @Override
    public GuardrailResult run(final String input) {
        Objects.requireNonNull(input, "input");
        final var entities = resolveEntities();
        if (entities.isEmpty()) {
            return GuardrailResult.pass(ENTITY_TYPE, input);
        }
        final var masks = new LinkedHashMap<String, List<String>>();
        for (final var entity : entities) {
            final var tokens = scanEntity(entity, input);
            if (!tokens.isEmpty()) {
                masks.put(entity.type(), tokens);
            }
        }
        final var custom = customRegexCheck.run(input);
        masks.putAll(custom.maskEntities());
        if (masks.isEmpty()) {
            return GuardrailResult.pass(ENTITY_TYPE, input);
        }
        final var cleanedValue = redactor.redact(input, masks);
        return GuardrailResult.fail(ENTITY_TYPE, cleanedValue, masks);
    }

    @Override
    public String name() {
        return "PiiCheck";
    }

    @Override
    public StreamCheck toStream() {
        return new StreamPiiCheck(config);
    }

    private Set<PiiEntity> resolveEntities() {
        return resolveFor(config);
    }

    /**
     * Resolves the entities a {@link PiiConfig} scans, in catalog order: the full catalog for
     * {@link PiiCoverage#ALL}, otherwise the selected entities filtered by catalog order. Shared
     * with the streaming {@code StreamPiiCheck} so both paths scan the same set in the same order.
     *
     * @param config the PII policy; never null
     * @return the entities to scan, in catalog order
     */
    static Set<PiiEntity> resolveFor(final PiiConfig config) {
        if (config.coverage() == PiiCoverage.ALL) {
            return PiiPatterns.all().keySet();
        }
        final var chosen = config.entities();
        final var ordered = new LinkedHashSet<PiiEntity>();
        for (final var entity : PiiPatterns.all().keySet()) {
            if (chosen.contains(entity)) {
                ordered.add(entity);
            }
        }
        return ordered;
    }

    private List<String> scanEntity(final PiiEntity entity,
                                    final String input) {
        final var seen = new LinkedHashSet<String>();
        final var matcher = PiiPatterns.forEntity(entity).matcher(input);
        while (matcher.find()) {
            final var token = matcher.group();
            final var accepted = config.strict()
                    ? PiiPatterns.isStrictMatch(entity, token)
                    : PiiPatterns.isNonStrictMatch(entity, token);
            if (accepted) {
                seen.add(token);
            }
        }
        return List.copyOf(seen);
    }
}
