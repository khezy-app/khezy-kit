package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.UrlsConfig;
import io.github.khezyapp.dpriv.redact.Redactor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the URL policy check (design §9.3): scheme allow-list, userinfo-always-block, host
 * allow-list when non-empty, malformed-input tolerance, and the {@code "link"} / {@code <LINK>}
 * mask contract.
 */
class UrlsCheckTest {

    private static UrlsCheck check(final UrlsConfig config) {
        return new UrlsCheck(config, new Redactor());
    }

    private static UrlsCheck defaultCheck() {
        return check(UrlsConfig.DEFAULTS);
    }

    @Test
    @DisplayName("should flag a URL whose scheme is not allowed")
    void flagsDisallowedScheme() {
        final var config = new UrlsConfig(List.of("https"), List.of());

        final var result = check(config).run("Check out http://visal.example today");

        assertThat(result.entityType()).isEqualTo("link");
        assertThat(result.detected()).isTrue();
        assertThat(result.isPassed()).isFalse();
        assertThat(result.maskEntities().get("link")).containsExactly("http://visal.example");
        assertThat(result.cleanedValue()).isEqualTo("Check out <LINK> today");
    }

    @Test
    @DisplayName("should not flag a clean allowed https URL")
    void cleanHttpsUrlPasses() {
        final var result = defaultCheck().run("Visit https://example.com for details");

        assertThat(result.detected()).isFalse();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.cleanedValue()).isEqualTo("Visit https://example.com for details");
        assertThat(result.maskEntities()).isEmpty();
    }

    @Test
    @DisplayName("should flag an ftp URL under the default scheme allow-list")
    void flagsFtpScheme() {
        final var result = defaultCheck().run("fetch ftp://example.com/files");

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("link")).containsExactly("ftp://example.com/files");
        assertThat(result.cleanedValue()).isEqualTo("fetch <LINK>");
    }

    @Test
    @DisplayName("should block userinfo regardless of allowed scheme and host")
    void blocksUserinfo() {
        final var config = new UrlsConfig(List.of("https"), List.of("example.com"));

        final var result = check(config).run("open https://user:pass@example.com now");

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("link")).containsExactly("https://user:pass@example.com");
        assertThat(result.cleanedValue()).isEqualTo("open <LINK> now");
    }

    @Test
    @DisplayName("should flag a host outside a non-empty allow-list")
    void flagsHostOutsideAllowList() {
        final var config = new UrlsConfig(List.of("https"), List.of("example.com"));

        final var result = check(config).run("see https://phnompenh.example.org report");

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("link")).containsExactly("https://phnompenh.example.org");
    }

    @Test
    @DisplayName("should pass an allow-listed host when the allow-list is non-empty")
    void allowsListedHost() {
        final var config = new UrlsConfig(List.of("https"), List.of("example.com"));

        final var result = check(config).run("see https://example.com report");

        assertThat(result.detected()).isFalse();
        assertThat(result.maskEntities()).isEmpty();
    }

    @Test
    @DisplayName("should ignore malformed URL text without throwing")
    void ignoresMalformedUrlText() {
        final var result = defaultCheck().run("the endpoint is http:// and that is all");

        assertThat(result.detected()).isFalse();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.maskEntities()).isEmpty();
    }

    @Test
    @DisplayName("should mask only policy-violating URLs among several")
    void masksOnlyViolatingLinks() {
        final var config = new UrlsConfig(List.of("https"), List.of());

        final var result = check(config)
                .run("ok https://example.com bad ftp://visal.example/share now");

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("link")).containsExactly("ftp://visal.example/share");
        assertThat(result.cleanedValue())
                .isEqualTo("ok https://example.com bad <LINK> now");
    }

    @Test
    @DisplayName("should keep flagged URLs unique and in first-seen order")
    void flaggedUrlsAreUniqueAndFirstSeen() {
        final var config = new UrlsConfig(List.of("http"), List.of());

        final var result = check(config)
                .run("a ftp://visal.example b ftp://champa.example c ftp://visal.example d");

        assertThat(result.maskEntities().get("link"))
                .containsExactly("ftp://visal.example", "ftp://champa.example");
    }

    @Test
    @DisplayName("should not double-report a bare domain covered by a scheme-ful URL")
    void schemeFulConsumesBareDomain() {
        final var config = new UrlsConfig(List.of("http", "https"), List.of("other.com"));

        final var result = check(config).run("see https://example.com or example.com now");

        assertThat(result.maskEntities().get("link")).containsExactly("https://example.com");
    }

    @Test
    @DisplayName("should strip trailing sentence punctuation from a flagged URL")
    void stripsTrailingPunctuation() {
        final var result = defaultCheck().run("read ftp://visal.example,");

        assertThat(result.maskEntities().get("link")).containsExactly("ftp://visal.example");
        assertThat(result.cleanedValue()).isEqualTo("read <LINK>,");
    }

    @Test
    @DisplayName("should not flag an ordinary scheme-less domain by default")
    void schemeLessDomainNotFlaggedByDefault() {
        final var result = defaultCheck().run("find us at www.example.com or example.com");

        assertThat(result.detected()).isFalse();
        assertThat(result.maskEntities()).isEmpty();
    }

    @Test
    @DisplayName("should flag a scheme-less domain against a host allow-list")
    void flagsSchemeLessDomainAgainstAllowList() {
        final var config = new UrlsConfig(List.of("http", "https"), List.of("example.com"));

        final var result = check(config).run("see phnompenh.example.org now");

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("link")).containsExactly("phnompenh.example.org");
        assertThat(result.cleanedValue()).isEqualTo("see <LINK> now");
    }
}
