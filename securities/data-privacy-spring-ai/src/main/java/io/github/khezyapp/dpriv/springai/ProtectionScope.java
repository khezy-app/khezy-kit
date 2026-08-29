package io.github.khezyapp.dpriv.springai;

/**
 * Where an advisor applies (design §7). Shared by both advisors.
 */
public enum ProtectionScope {
    INPUT,    // user messages only (default for both advisors)
    OUTPUT,   // model response only
    BOTH      // both directions
}
