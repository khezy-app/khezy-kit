package io.github.khezyapp.dhttp.security;

import java.util.*;

/**
 * Redacts secrets before they reach logs or exception messages ({@code R12}, contract 4).
 *
 * <p>Authorization headers, credential fields, OAuth2 access/refresh tokens, and any configured
 * secret values are matched literally and replaced with {@code ***}.</p>
 */
public final class SecretRedactor {

    private static final String MASK = "***";
    private static final List<String> SECRET_KEYS = List.of(
            "password", "secret", "token", "authorization", "apikey");

    private static final SecretRedactor INSTANCE = new SecretRedactor();

    private SecretRedactor() {
    }

    /**
     * @return the shared {@link SecretRedactor} instance
     */
    public static SecretRedactor get() {
        return INSTANCE;
    }

    /**
     * @param text    the message to sanitize
     * @param secrets the secret values to hide
     * @return {@code text} with every occurrence of a secret replaced by {@code ***}
     */
    public String redact(final String text,
                         final List<String> secrets) {
        if (Objects.isNull(text) || Objects.isNull(secrets) || secrets.isEmpty()) {
            return text;
        }
        var result = text;
        for (final var secret : secrets) {
            if (Objects.nonNull(secret) && !secret.isEmpty()) {
                result = result.replace(secret, MASK);
            }
        }
        return result;
    }

    /**
     * Redacts {@code text} using the secret-like values found in {@code values} (keys such as
     * {@code password}, {@code token}, {@code Authorization}).
     *
     * @param text   the message to sanitize
     * @param values a map that may hold secrets
     * @return the sanitized message
     */
    public String redact(final String text,
                         final Map<String, Object> values) {
        return redact(text, extractSecrets(values));
    }

    /**
     * Collects the values of secret-like keys from a map.
     *
     * @param values the map to inspect
     * @return the secret values, or an empty list whens none are present
     */
    public List<String> extractSecrets(final Map<String, Object> values) {
        if (Objects.isNull(values) || values.isEmpty()) {
            return List.of();
        }
        final var secrets = new ArrayList<String>();
        for (final var entry : values.entrySet()) {
            final var key = Objects.isNull(entry.getKey()) ? "" : entry.getKey().toLowerCase(Locale.ROOT);
            final var value = entry.getValue();
            if (isSecretKey(key) && Objects.nonNull(value)) {
                secrets.add(value.toString());
            }
        }
        return List.copyOf(secrets);
    }

    private static boolean isSecretKey(final String key) {
        for (final var secretKey : SECRET_KEYS) {
            if (key.contains(secretKey)) {
                return true;
            }
        }
        return false;
    }
}
