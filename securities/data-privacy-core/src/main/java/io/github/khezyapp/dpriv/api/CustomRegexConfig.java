package io.github.khezyapp.dpriv.api;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A user-named custom regex rule (design §9.5).
 *
 * @param name     the rule name (uppercased and used as the entity type)
 * @param patterns the patterns to match
 */
public record CustomRegexConfig(String name, List<Pattern> patterns) {
}
