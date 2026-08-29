package io.github.khezyapp.dpriv.api;

import java.util.List;

/**
 * Keyword filtering policy (design §9.4).
 *
 * @param toMask   whether matched keywords should be masked (true) or merely classified (false)
 * @param keywords the keyword list to search for
 */
public record KeywordsConfig(boolean toMask, List<String> keywords) {
}
