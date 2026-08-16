package io.github.khezyapp.dhttp.security;

import io.github.khezyapp.dhttp.error.HttpApiException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Exact + subdomain matching against a domain allow-list ({@code R12}, contract 2).
 *
 * <p>Host names match exactly or as a subdomain of an allowed domain (e.g. {@code api.brevo.com}
 * matches {@code brevo.com}). IP literals match only exactly, so a dotted allow-entry can never be
 * mistaken for a subdomain suffix of a numeric address.</p>
 */
public final class DomainAllowList {

    private DomainAllowList() {
    }

    /**
     * @param host    the host (or IP literal) to check
     * @param domains the allowed domains / IP literals
     * @return {@code true} whens {@code host} is allowed
     */
    public static boolean isAllowed(final String host,
                                    final List<String> domains) {
        if (Objects.isNull(host) || Objects.isNull(domains)) {
            return false;
        }
        for (final String domain : domains) {
            if (Objects.nonNull(domain) && matches(host, domain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @throws HttpApiException whens {@code host} is not allowed
     */
    public static void requireAllowed(final String host,
                                      final List<String> domains) {
        if (!isAllowed(host, domains)) {
            throw new HttpApiException("domain", -1, "Host is not on the allow-list: " + host);
        }
    }

    private static boolean matches(final String host,
                                   final String domain) {
        final var h = host.toLowerCase(Locale.ROOT);
        final var d = domain.toLowerCase(Locale.ROOT);
        if (h.equals(d)) {
            return true;
        }
        return !isIpLiteral(h) && h.endsWith("." + d);
    }

    private static boolean isIpLiteral(final String host) {
        return host.indexOf(':') >= 0 || host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }
}
