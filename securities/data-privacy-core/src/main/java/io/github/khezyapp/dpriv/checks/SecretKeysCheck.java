package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.GuardrailCheck;
import io.github.khezyapp.dpriv.api.GuardrailResult;
import io.github.khezyapp.dpriv.api.SecretConfig;
import io.github.khezyapp.dpriv.api.StreamCheck;
import io.github.khezyapp.dpriv.internal.SecretCandidateFilter;
import io.github.khezyapp.dpriv.redact.Redactor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Secret/key/token detection check (design §9.2 + §5.4). Scans the input for high-entropy runs and
 * merges any configured custom pattern matches into the same {@code "secret"} {@code maskEntities}
 * bucket. The reported {@code cleanedValue} is the input with every flagged token replaced by its
 * {@code <SECRET>} placeholder.
 *
 * <p>Fully deterministic and reusable from {@code toStream()} composition (Task 09) because the
 * candidate predicate {@link SecretCandidateFilter} lives in {@code internal} and the custom
 * patterns come straight from the immutable {@link SecretConfig}.
 */
public final class SecretKeysCheck implements GuardrailCheck {

    /**
     * Base64/hex-safe candidate token: an alphanumeric run optionally followed by trailing
     * {@code +}{@code /}{@code =} (base64 padding). The base64 characters are allowed only as a
     * trailing run so a {@code key=value} separator does not glom into the value token.
     */
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9]+(?:[+/=]+)?");

    private final SecretConfig config;
    private final Redactor redactor;
    private final SecretCandidateFilter filter;

    /**
     * Creates the check for the given policy and redactor.
     *
     * @param config   the secret scanning policy; never null
     * @param redactor the redactor used to compute {@code cleanedValue}; never null
     */
    public SecretKeysCheck(final SecretConfig config,
                           final Redactor redactor) {
        this.config = Objects.requireNonNull(config, "config");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
        this.filter = new SecretCandidateFilter(config.preset().params());
    }

    @Override
    public GuardrailResult run(final String input) {
        Objects.requireNonNull(input, "input");
        final var tokens = scanTokens(input);
        if (tokens.isEmpty()) {
            return GuardrailResult.pass("secret", input);
        }
        final var maskEntities = Map.of("secret", List.copyOf(tokens));
        return GuardrailResult.fail("secret", redactor.redact(input, maskEntities), maskEntities);
    }

    @Override
    public String name() {
        return "SecretKeysCheck";
    }

    @Override
    public StreamCheck toStream() {
        return new StreamSecretKeysCheck(config);
    }

    private Set<String> scanTokens(final String input) {
        final var tokens = new LinkedHashSet<String>();
        final var matcher = TOKEN.matcher(input);
        while (matcher.find()) {
            final var start = matcher.start();
            final var end = matcher.end();
            if (filter.accept(input, start, end)) {
                tokens.add(input.substring(start, end));
            }
        }
        for (final var patterns : config.customPatterns().values()) {
            for (final var pattern : patterns) {
                collectCustomMatches(pattern, input, tokens);
            }
        }
        return tokens;
    }

    /**
     * The base64/hex candidate-run pattern shared with the streaming {@code StreamSecretKeysCheck}
     * so both paths tokenize with the same boundary definition (Task 05 logged rule).
     *
     * @return the candidate token pattern
     */
    static Pattern tokenPattern() {
        return TOKEN;
    }

    private static void collectCustomMatches(final Pattern pattern,
                                             final String input,
                                             final Set<String> tokens) {
        final var matcher = pattern.matcher(input);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
    }
}
