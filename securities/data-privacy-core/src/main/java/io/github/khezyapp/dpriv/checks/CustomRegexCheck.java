package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.CustomRegexConfig;
import io.github.khezyapp.dpriv.api.GuardrailCheck;
import io.github.khezyapp.dpriv.api.GuardrailResult;
import io.github.khezyapp.dpriv.redact.Redactor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * User-named custom regex guardrail check (design §9.5). Each {@link CustomRegexConfig#name()}
 * becomes its own {@code maskEntities} key holding the full matched texts (unique, first-seen
 * order); a name is never folded into the {@code pii} family. {@code detected} is {@code true}
 * when any group matches, and {@code cleanedValue} redacts every matched token through the shared
 * {@link Redactor}.
 *
 * <p>An empty config list yields an identity {@code pass}; a malformed (blank) or {@code null}
 * name, or a {@code null}/{@code null}-entry pattern, is skipped without ever throwing. Kept
 * public so Task 10 can run it standalone as well as folded into {@link PiiCheck}.
 */
public final class CustomRegexCheck implements GuardrailCheck {

    private static final String ENTITY_TYPE = "custom";

    private final List<CustomRegexConfig> configs;
    private final Redactor redactor;

    /**
     * Creates the check for the given configs and redactor.
     *
     * @param configs  the named custom regex rules; never null
     * @param redactor the redactor used to compute {@code cleanedValue}; never null
     */
    public CustomRegexCheck(final List<CustomRegexConfig> configs,
                            final Redactor redactor) {
        this.configs = Objects.requireNonNull(configs, "configs");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
    }

    @Override
    public GuardrailResult run(final String input) {
        Objects.requireNonNull(input, "input");
        final var masks = new LinkedHashMap<String, List<String>>();
        for (final var config : configs) {
            if (Objects.isNull(config) || isBlankName(config.name())) {
                continue;
            }
            final var tokens = matchTokens(input, config.patterns());
            if (!tokens.isEmpty()) {
                masks.put(config.name(), tokens);
            }
        }
        if (masks.isEmpty()) {
            return GuardrailResult.pass(ENTITY_TYPE, input);
        }
        final var cleanedValue = redactor.redact(input, masks);
        return GuardrailResult.fail(ENTITY_TYPE, cleanedValue, masks);
    }

    @Override
    public String name() {
        return "CustomRegexCheck";
    }

    private static List<String> matchTokens(final String input,
                                            final List<Pattern> patterns) {
        final var seen = new LinkedHashSet<String>();
        if (Objects.isNull(patterns)) {
            return List.of();
        }
        for (final var pattern : patterns) {
            if (Objects.isNull(pattern)) {
                continue;
            }
            final var matcher = pattern.matcher(input);
            while (matcher.find()) {
                seen.add(matcher.group());
            }
        }
        return List.copyOf(seen);
    }

    private static boolean isBlankName(final String name) {
        return Objects.isNull(name) || name.isBlank();
    }
}
