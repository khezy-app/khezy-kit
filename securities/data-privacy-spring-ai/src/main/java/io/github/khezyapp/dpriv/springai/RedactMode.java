package io.github.khezyapp.dpriv.springai;

/**
 * Which user messages DataPrivacyAdvisor redacts in before() (design §7).
 */
public enum RedactMode {
    ALL,        // every USER message in the prompt, incl. history (default)
    LAST_ONLY   // only the last USER message (perf opt-in)
}
