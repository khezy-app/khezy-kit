package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.GuardrailCheck;
import io.github.khezyapp.dpriv.api.GuardrailResult;
import io.github.khezyapp.dpriv.api.StreamCheck;
import io.github.khezyapp.dpriv.api.UrlsConfig;
import io.github.khezyapp.dpriv.redact.Redactor;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * URL policy check (design §9.3). Detects candidate URLs with a three-pass regex sweep
 * (scheme-ful, scheme-less domain, bare IP), parses each candidate with {@link java.net.URI}, and
 * flags a link when its scheme is not allowed, it carries {@code user:pass@} userinfo (always
 * blocked), or (when a host allow-list is configured) its host is not on the list. Only
 * policy-violating URLs are reported; a clean, allowed link passes with the input unchanged.
 *
 * <p>The detection sweep ({@link #detect}) and the classifier ({@link #isFlagged}) are exposed
 * package-visible static so the streaming {@code StreamUrlsCheck} (Task 09) re-runs exactly the
 * same passes over windowed text with overlap, keeping the in-memory == streaming parity
 * contract. Both take only immutable inputs and share no mutable state.
 */
public final class UrlsCheck implements GuardrailCheck {

    /**
     * Pass 1 — scheme-ful URL texts. One pattern per family ({@code http(s)://}, {@code ftp://},
     * and the single-colon {@code data:} / {@code javascript:} / {@code vbscript:} /
     * {@code mailto:} forms). Uppercase schemes are simply not detected, mirroring the reference
     * sweep (which is lowercase).
     */
    private static final String SCHEME_CLASS = "[^\\s<>\"{}|\\\\^`\\[\\]]+";

    private static final List<Pattern> SCHEME_PATTERNS = List.of(
            Pattern.compile("https?://" + SCHEME_CLASS),
            Pattern.compile("ftp://" + SCHEME_CLASS),
            Pattern.compile("data:" + SCHEME_CLASS),
            Pattern.compile("javascript:" + SCHEME_CLASS),
            Pattern.compile("vbscript:" + SCHEME_CLASS),
            Pattern.compile("mailto:" + SCHEME_CLASS)
    );

    /**
     * Pass 2 — scheme-less domains with an optional {@code www.} prefix and optional path.
     */
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("\\b(?:www\\.)?[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}(?:/[^\\s]*)?");

    /**
     * Pass 3 — bare IPv4 addresses with optional port and path.
     */
    private static final Pattern IP_PATTERN =
            Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::[0-9]+)?(?:/[^\\s]*)?");

    /**
     * Matches a single-colon special scheme so it can be parsed without synthesizing a host.
     */
    private static final Pattern SPECIAL_SCHEME =
            Pattern.compile("^(?:data|javascript|vbscript|mailto):");

    /**
     * Removes trailing sentence punctuation from a raw match so {@code "https://visal.example,."}
     * is reported as {@code "https://visal.example"}.
     */
    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[].,;:!?)\\\\]+$");

    /**
     * Splits a detected candidate into its first {@code /}, {@code ?}, or {@code #} cut point.
     */
    private static final Pattern PATH_SEPARATOR = Pattern.compile("[/?#]");

    private final Redactor redactor;
    private final Set<String> allowedSchemes;
    private final Set<String> allowedHosts;

    /**
     * Creates the check for the given policy and redactor. The scheme and host allow-lists are
     * normalized to lowercase at construction so {@link #run} and Task 09 share one comparison.
     *
     * @param config   the URL policy; never null
     * @param redactor the redactor used to compute {@code cleanedValue}; never null
     */
    public UrlsCheck(final UrlsConfig config,
                     final Redactor redactor) {
        Objects.requireNonNull(config, "config");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
        this.allowedSchemes = normalize(config.allowedSchemes());
        this.allowedHosts = normalize(config.allowedHosts());
    }

    @Override
    public GuardrailResult run(final String input) {
        Objects.requireNonNull(input, "input");
        final var blocked = new LinkedHashSet<String>();
        for (final var candidate : detect(input)) {
            if (isFlagged(candidate, allowedSchemes, allowedHosts)) {
                blocked.add(candidate);
            }
        }
        if (blocked.isEmpty()) {
            return GuardrailResult.pass("link", input);
        }
        final var maskEntities = Map.of("link", List.copyOf(blocked));
        return GuardrailResult.fail("link", redactor.redact(input, maskEntities), maskEntities);
    }

    @Override
    public String name() {
        return "UrlsCheck";
    }

    @Override
    public StreamCheck toStream() {
        return new StreamUrlsCheck(allowedSchemes, allowedHosts);
    }

    /**
     * Three-pass URL detection (design §9.3): scheme-ful URLs first, then scheme-less domains and
     * bare IPs that are not already covered by a scheme-ful match. Trailing punctuation is cleaned
     * off each match and the result is unique in first-seen order. Reused by Task 09's
     * {@code StreamUrlsCheck} over windowed text.
     *
     * @param text the text to scan
     * @return the detected candidate URLs, unique, first-seen order
     */
    static List<String> detect(final String text) {
        return finalizeCandidates(detectSpans(text).stream()
                .map(UrlSpan::candidate)
                .toList());
    }

    /**
     * A detected pre-finalized URL candidate with its raw span and detection-pass index. Pass
     * indices are stable per detection family ({@code 0..SCHEME_PATTERNS.size() - 1} for the
     * scheme-ful patterns, then the scheme-less-domain pass and finally the bare-IP pass), so the
     * streaming check can reorder per-window spans into the same pattern-major order the
     * in-memory sweep produces over the full text.
     */
    record UrlSpan(int start, int end, String candidate, int patternIndex) {
    }

    /**
     * The raw three-pass sweep over one text, reporting every candidate span (start, end,
     * cleaned candidate, pass index) in detection order. Unlike {@link #detect}, the result is not
     * finalized or deduplicated; the streaming check merges all windows first and finalizes once,
     * replicating the in-memory ordering.
     *
     * @param text the text to sweep
     * @return the raw candidate spans
     */
    static List<UrlSpan> detectSpans(final String text) {
        final var spans = new ArrayList<UrlSpan>();
        final var schemeDomains = new HashSet<String>();
        for (var i = 0; i < SCHEME_PATTERNS.size(); i++) {
            final var matcher = SCHEME_PATTERNS.get(i).matcher(text);
            while (matcher.find()) {
                final var candidate = cleanup(matcher.group());
                if (!candidate.isEmpty()) {
                    final var schemeEnd = candidate.indexOf("://");
                    if (schemeEnd >= 0) {
                        schemeDomains.add(hostPart(candidate.substring(schemeEnd + 3)));
                    }
                    spans.add(new UrlSpan(matcher.start(), matcher.end(), candidate, i));
                }
            }
        }
        matchDomainAndIp(text, spans, schemeDomains);
        return List.copyOf(spans);
    }

    /**
     * Applies the policy rules of §9.3 to one candidate URL: it parses cleanly, its scheme is
     * allowed, it carries no userinfo, and (when a host allow-list is configured) its host is on
     * it. Parse failures drop the candidate. Reused by Task 09's {@code StreamUrlsCheck}.
     *
     * @param candidate      a detected candidate URL
     * @param allowedSchemes the normalized (lowercase) allowed-scheme set
     * @param allowedHosts   the normalized (lowercase) host allow-list; empty disables the host rule
     * @return {@code true} when the link is a policy violation
     */
    static boolean isFlagged(final String candidate,
                             final Set<String> allowedSchemes,
                             final Set<String> allowedHosts) {
        final var uri = parse(candidate);
        return Objects.nonNull(uri) && isPolicyViolation(uri, allowedSchemes, allowedHosts);
    }

    private static void matchDomainAndIp(final String text,
                                         final List<UrlSpan> spans,
                                         final Set<String> schemeDomains) {
        final var domainMatcher = DOMAIN_PATTERN.matcher(text);
        while (domainMatcher.find()) {
            final var candidate = cleanup(domainMatcher.group());
            if (!candidate.isEmpty() && !schemeDomains.contains(hostPart(candidate))) {
                spans.add(new UrlSpan(domainMatcher.start(), domainMatcher.end(), candidate,
                        SCHEME_PATTERNS.size()));
            }
        }
        final var ipMatcher = IP_PATTERN.matcher(text);
        while (ipMatcher.find()) {
            final var candidate = cleanup(ipMatcher.group());
            if (!candidate.isEmpty() && !schemeDomains.contains(hostPart(candidate))) {
                spans.add(new UrlSpan(ipMatcher.start(), ipMatcher.end(), candidate,
                        SCHEME_PATTERNS.size() + 1));
            }
        }
    }

    private static boolean isPolicyViolation(final URI uri,
                                             final Set<String> allowedSchemes,
                                             final Set<String> allowedHosts) {
        final var scheme = uri.getScheme();
        if (Objects.isNull(scheme) || !allowedSchemes.contains(lower(scheme))) {
            return true;
        }
        if (Objects.nonNull(uri.getRawUserInfo())) {
            return true;
        }
        if (!allowedHosts.isEmpty()) {
            final var host = uri.getHost();
            if (Objects.isNull(host) || !allowedHosts.contains(lower(host))) {
                return true;
            }
        }
        return false;
    }

    private static URI parse(final String candidate) {
        try {
            if (candidate.contains("://") || SPECIAL_SCHEME.matcher(candidate).find()) {
                return URI.create(candidate);
            }
            return URI.create("http://" + candidate);
        } catch (final IllegalArgumentException e) {
            // ignore malformed detection artifact — drop the candidate, never crash
            return null;
        }
    }

    /**
     * Cross-pass dedupe (design §9.3 step 3): scheme-ful candidates are kept first (in input
     * order) and cover their hosts; a scheme-less domain or bare-IP candidate is kept only when its
     * host was not already covered by a scheme-ful candidate. Shared with the streaming check so
     * both paths finalize identically.
     *
     * @param detected the raw candidate spans' candidates, in detection order
     * @return the unique, finalized candidate list
     */
    static List<String> finalizeCandidates(final List<String> detected) {
        final var covered = new HashSet<String>();
        final var result = new LinkedHashSet<String>();
        for (final var candidate : detected) {
            if (candidate.contains("://")) {
                final var parsed = parse(candidate);
                if (Objects.nonNull(parsed) && Objects.nonNull(parsed.getHost())) {
                    covered.add(parsed.getHost().toLowerCase(Locale.ROOT));
                    covered.add(stripWww(parsed.getHost().toLowerCase(Locale.ROOT)));
                }
                result.add(candidate);
            }
        }
        for (final var candidate : detected) {
            if (!candidate.contains("://")
                    && !covered.contains(stripWww(candidate.toLowerCase(Locale.ROOT)))) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    private static String cleanup(final String match) {
        return TRAILING_PUNCTUATION.matcher(match).replaceAll("");
    }

    /**
     * The host part of a candidate: everything before the first {@code /}, {@code ?}, or {@code #}.
     */
    private static String hostPart(final String candidate) {
        final var matcher = PATH_SEPARATOR.matcher(candidate);
        final var end = matcher.find() ? matcher.start() : candidate.length();
        return lower(candidate.substring(0, end));
    }

    private static String stripWww(final String host) {
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private static String lower(final String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static Set<String> normalize(final List<String> values) {
        final var normalized = new HashSet<String>();
        for (final var value : values) {
            if (Objects.isNull(value)) {
                continue;
            }
            final var trimmed = value.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return Set.copyOf(normalized);
    }
}
