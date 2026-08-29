package io.github.khezyapp.dpriv.api;

import io.github.khezyapp.dpriv.policy.SecretPreset;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Secret scanning policy (design §5.4).
 *
 * @param preset         the entropy/heuristic preset
 * @param customPatterns optional named custom patterns
 */
public record SecretConfig(
        SecretPreset preset,
        Map<String, List<Pattern>> customPatterns) {

    /**
     * Defaults: {@link SecretPreset#BALANCED}, no custom patterns.
     */
    public static final SecretConfig DEFAULTS = new SecretConfig(SecretPreset.BALANCED, Map.of());
}
