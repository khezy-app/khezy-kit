package io.github.khezyapp.doa.api;

import io.github.khezyapp.doa.model.PathToken;

import java.util.List;

/**
 * Responsible for parsing raw string paths into a sequence of structured tokens.
 * Implementations should handle caching of frequently parsed paths.
 */
public interface PathParser {
    /**
     * Converts a string path into an ordered list of AccessTokens.
     * @param path The raw path string.
     * @return A list of tokens representing properties or indices.
     */
    List<PathToken> parse(String path);
}

