package io.github.khezyapp.dpriv.checks;

import java.util.List;

/**
 * Detects candidate matches inside a single window. Implementations run exactly the same detection
 * a check's in-memory {@code run} uses, over a one-window substring with overlap, so the streaming
 * and in-memory paths cannot drift (design §10.3 parity rule).
 */
@FunctionalInterface
interface Detector {

    /**
     * Detects candidate spans in the given window.
     *
     * @param meta the window text and boundary context
     * @return the detected spans, in detection order
     */
    List<TokenSpan> detect(WindowMeta meta);
}