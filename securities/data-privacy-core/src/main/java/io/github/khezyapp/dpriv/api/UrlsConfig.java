package io.github.khezyapp.dpriv.api;

import java.util.List;

/**
 * URL validation policy (design §5.4).
 *
 * @param allowedSchemes allowed URL schemes (e.g. {@code http}, {@code https})
 * @param allowedHosts   allow-listed hosts; empty means deny-by-default (block all)
 */
public record UrlsConfig(List<String> allowedSchemes, List<String> allowedHosts) {

    /**
     * Defaults: {@code http} and {@code https} schemes allowed, empty host allow-list.
     */
    public static final UrlsConfig DEFAULTS = new UrlsConfig(List.of("http", "https"), List.of());
}
