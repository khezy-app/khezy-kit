package io.github.khezyapp.dpriv.checks;

/**
 * Per-window context handed to a streaming detector (design §10.3).
 *
 * @param text              the window text to scan
 * @param window            the zero-based window index
 * @param first             whether this is the very first window
 * @param last              whether this is the final window (end of input reached)
 * @param prevChar          the last character of the previous window (empty for the first window);
 *                          the true full-text predecessor of the current window's first character
 * @param prevEndsWithWord  whether the previous window ends in a letter, digit, or underscore
 */
record WindowMeta(String text,
                  int window,
                  boolean first,
                  boolean last,
                  String prevChar,
                  boolean prevEndsWithWord) {
}