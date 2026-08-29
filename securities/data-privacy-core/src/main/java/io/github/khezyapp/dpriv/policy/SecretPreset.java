package io.github.khezyapp.dpriv.policy;

/**
 * Secret-scanning presets mapping 1:1 to the reference table (design §9.2). Tuple order and
 * {@code strictMode} naming are pinned in the handoff log; Task 05 consumes it.
 */
public enum SecretPreset {

    STRICT(10, 3.0, 2, true),
    BALANCED(10, 3.8, 3, false),
    PERMISSIVE(30, 4.0, 2, false);

    private final SecretPresetParams params;

    SecretPreset(final int minLength,
                 final double minEntropy,
                 final int minDiversity,
                 final boolean strictMode) {
        this.params = new SecretPresetParams(minLength, minEntropy, minDiversity, strictMode);
    }

    /**
     * The parameter tuple carried by this preset.
     *
     * @return the params
     */
    public SecretPresetParams params() {
        return params;
    }
}
