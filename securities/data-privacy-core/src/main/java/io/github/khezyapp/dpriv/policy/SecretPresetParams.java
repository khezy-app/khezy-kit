package io.github.khezyapp.dpriv.policy;

/**
 * The resolved parameter tuple for a {@link SecretPreset} (design §9.2).
 *
 * @param minLength    minimum token length
 * @param minEntropy   minimum Shannon entropy
 * @param minDiversity minimum distinct character classes
 * @param strictMode   whether the strict decision procedure applies
 */
public record SecretPresetParams(
        int minLength,
        double minEntropy,
        int minDiversity,
        boolean strictMode) {
}
