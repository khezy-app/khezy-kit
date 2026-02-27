package io.github.khezyapp.doa.engine;

import io.github.khezyapp.doa.api.PathParser;
import io.github.khezyapp.doa.cache.Cache;
import io.github.khezyapp.doa.model.IndexToken;
import io.github.khezyapp.doa.model.PathToken;
import io.github.khezyapp.doa.model.PropertyToken;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link PathParser} that converts string paths into a sequence of {@link PathToken}s.
 * <p>
 * This implementation utilizes a {@link Cache} to store previously parsed paths, significantly
 * improving performance for repetitive access patterns.
 * </p>
 */
public class DefaultPathParser implements PathParser {
    private final Cache<List<PathToken>> cache;

    /**
     * Constructs a DefaultPathParser with the specified cache strategy.
     *
     * @param cache the cache used to store and retrieve parsed token lists
     */
    public DefaultPathParser(final Cache<List<PathToken>> cache) {
        this.cache = cache;
    }

    /**
     * Parses the given string path into a list of executable tokens.
     * <p>
     * If the path has been parsed before, it is retrieved from the cache. Otherwise,
     * the path is broken down into property and index components.
     * </p>
     *
     * @param path the string representation of the object path (e.g., "order.items[0].price")
     * @return an unmodifiable list of {@link PathToken} instances representing the path
     * @throws IllegalArgumentException if the path is null, empty, or consists only of whitespace
     */
    @Override
    public List<PathToken> parse(final String path) {
        if (Objects.isNull(path) || path.isBlank()) {
            throw new IllegalArgumentException("Path cannot be null or blank");
        }
        return cache.get(path, this::doParse);
    }

    private List<PathToken> doParse(final String path) {
        final List<PathToken> tokens = new LinkedList<>();
        final int len = path.length();
        int i = 0;

        while (i < len) {
            final var c = path.charAt(i);

            if (c == '.') {
                i++;
                continue;
            }

            if (c == '[') {
                // --- Index/Quoted Key Mode ---
                final var closingBracket = findClosingBracket(path, i);
                final var inside = path.substring(i + 1, closingBracket).trim();

                if (isQuoted(inside)) {
                    // Handle ['key.with.dots']
                    tokens.add(new PropertyToken(stripQuotes(inside), false));
                } else if (isNumeric(inside)) {
                    // Handle [0]
                    tokens.add(new IndexToken(Integer.parseInt(inside)));
                } else {
                    // Handle [property] as a PropertyToken
                    tokens.add(new PropertyToken(inside, false));
                }
                i = closingBracket + 1;
            } else {
                // --- Property Mode ---
                final var start = i;
                while (i < len && path.charAt(i) != '.' && path.charAt(i) != '[') {
                    i++;
                }
                final var segment = path.substring(start, i);
                final var safeAccess = segment.endsWith("?");
                final var name = safeAccess ? segment.substring(0, segment.length() - 1) : segment;

                if (!name.isEmpty()) {
                    tokens.add(new PropertyToken(name, safeAccess));
                }
            }
        }
        return tokens;
    }

    private int findClosingBracket(final String path,
                                   final int start) {
        final var end = path.indexOf(']', start);
        if (end == -1) {
            throw new IllegalArgumentException("Unmatched bracket in path: " + path);
        }
        return end;
    }

    private boolean isQuoted(final String s) {
        return s.startsWith("'") && s.endsWith("'") ||
            s.startsWith("\"") && s.endsWith("\"");
    }

    private String stripQuotes(final String s) {
        return s.substring(1, s.length() - 1);
    }

    private boolean isNumeric(final String s) {
        return s.matches("\\d+");
    }
}
