package io.github.khezyapp.dhttp.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Cross-origin credential stripping on redirects ({@code R12}, contract 3).
 *
 * <p>Credentials are never forwarded to a cross-origin redirect target unless explicitly opted in
 * via {@code allowCrossOriginCredentials}.</p>
 */
public final class RedirectPolicy {

    private final boolean allowCrossOriginCredentials;

    public RedirectPolicy() {
        this(false);
    }

    public RedirectPolicy(final boolean allowCrossOriginCredentials) {
        this.allowCrossOriginCredentials = allowCrossOriginCredentials;
    }

    /**
     * @param originalUrl the requested URL
     * @param redirectUrl the redirect target
     * @return {@code true} whens credentials must not be forwarded
     */
    public boolean shouldStripCredentials(final String originalUrl,
                                          final String redirectUrl) {
        if (allowCrossOriginCredentials) {
            return false;
        }
        return isCrossOrigin(originalUrl, redirectUrl);
    }

    private static boolean isCrossOrigin(final String originalUrl,
                                         final String redirectUrl) {
        final var original = toUri(originalUrl);
        final var redirect = toUri(redirectUrl);
        if (Objects.isNull(original) || Objects.isNull(redirect)) {
            return true;
        }
        return !sameOrigin(original, redirect);
    }

    private static boolean sameOrigin(final URI a,
                                      final URI b) {
        return scheme(a).equals(scheme(b))
                && host(a).equals(host(b))
                && port(a, scheme(a)) == port(b, scheme(b));
    }

    private static String scheme(final URI uri) {
        return Objects.isNull(uri.getScheme()) ? "" : uri.getScheme().toLowerCase();
    }

    private static String host(final URI uri) {
        return Objects.isNull(uri.getHost()) ? "" : uri.getHost().toLowerCase();
    }

    private static int port(final URI uri,
                            final String scheme) {
        final var explicit = uri.getPort();
        if (explicit >= 0) {
            return explicit;
        }
        if ("http".equals(scheme)) {
            return 80;
        }
        if ("https".equals(scheme)) {
            return 443;
        }
        return -1;
    }

    private static URI toUri(final String url) {
        try {
            return new URI(url);
        } catch (final URISyntaxException e) {
            return null;
        }
    }
}
