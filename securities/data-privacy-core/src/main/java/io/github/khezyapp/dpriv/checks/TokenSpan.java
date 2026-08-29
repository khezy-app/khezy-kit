package io.github.khezyapp.dpriv.checks;

/**
 * A detected match span with enough context for the streaming boundary rules and the per-check
 * drain ordering. Positions are relative to the window the span was detected in; {@code window} is
 * the zero-based window index and {@code patternIndex} distinguishes detection passes within one
 * window (per-check semantics, e.g. one ordinal per secret custom pattern).
 *
 * @param start        the inclusive start offset in the window text
 * @param end          the exclusive end offset in the window text
 * @param token        the detected token text
 * @param entityType   the entity type the token belongs to
 * @param patternIndex the detection-pass ordinal within the window's detector
 * @param window       the zero-based window index
 */
record TokenSpan(int start,
                 int end,
                 String token,
                 String entityType,
                 int patternIndex,
                 int window) {
}