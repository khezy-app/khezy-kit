package io.github.khezyapp.dhttp.security;

import io.github.khezyapp.dhttp.error.HttpApiException;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SSRF guard ({@code R12}, contract 1).
 *
 * <p>Validates a URL host against the allowed domains both by name and, for DNS names, by every
 * address it resolves to. Direct IP literals are checked against the allow-list without any DNS
 * lookup. Strictest-first: anything that cannot be proven allowed is rejected.</p>
 */
public final class SsrfGuard {

    private SsrfGuard() {
    }

    /**
     * @param url             the URL to validate
     * @param allowedDomains  the allowed domains and/or IP literals
     * @param allowIpLiteral  whens {@code true}, raw IP literals bypass the allow-list
     * @throws HttpApiException whens the host (or any resolved address) is not allowed
     */
    public static void validate(final String url,
                                final List<String> allowedDomains,
                                final boolean allowIpLiteral) {
        final var host = hostOf(url);
        if (isIpLiteral(host)) {
            validateLiteral(host, allowedDomains, allowIpLiteral);
            return;
        }
        validateDnsName(host, allowedDomains);
    }

    /**
     * Name-level allow-list check (exact + subdomain, no DNS).
     *
     * @param host            the host (or IP literal) to check
     * @param allowedDomains  the allowed domains and/or IP literals
     */
    public static boolean allows(final String host,
                                 final List<String> allowedDomains) {
        return DomainAllowList.isAllowed(host, allowedDomains);
    }

    private static void validateLiteral(final String host,
                                        final List<String> allowedDomains,
                                        final boolean allowIpLiteral) {
        if (allowIpLiteral) {
            return;
        }
        if (!allowedDomains.contains(host)) {
            throw new HttpApiException("ssrf", -1, "IP literal is not on the allow-list: " + host);
        }
    }

    private static void validateDnsName(final String host,
                                        final List<String> allowedDomains) {
        final var allowed = allowedAddresses(allowedDomains);
        for (final var address : resolve(host)) {
            if (!allowed.contains(address)) {
                throw new HttpApiException("ssrf", -1,
                        "Host " + host + " resolves to an address that is not on the allow-list: " + address);
            }
        }
    }

    private static Set<String> allowedAddresses(final List<String> allowedDomains) {
        final var result = new HashSet<String>();
        for (final var domain : allowedDomains) {
            result.addAll(resolve(domain));
        }
        return result;
    }

    private static Set<String> resolve(final String host) {
        try {
            final var result = new HashSet<String>();
            for (final var address : InetAddress.getAllByName(host)) {
                result.add(address.getHostAddress());
            }
            return result;
        } catch (final UnknownHostException e) {
            throw new HttpApiException(HttpApiException.NO_STATUS, "ssrf", -1,
                    "Cannot resolve host: " + host, e);
        }
    }

    private static String hostOf(final String url) {
        final URI uri;
        try {
            uri = new URI(url);
        } catch (final URISyntaxException e) {
            throw new HttpApiException(HttpApiException.NO_STATUS, "ssrf", -1, "Invalid URL: " + url, e);
        }
        final var host = uri.getHost();
        if (host == null) {
            throw new HttpApiException("ssrf", -1, "URL has no host: " + url);
        }
        return host;
    }

    private static boolean isIpLiteral(final String host) {
        return host.indexOf(':') >= 0 || host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }
}
