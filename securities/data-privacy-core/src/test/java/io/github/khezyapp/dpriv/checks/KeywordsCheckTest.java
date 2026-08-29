package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.KeywordsConfig;
import io.github.khezyapp.dpriv.redact.Redactor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the keyword filter check (design §9.4): whole-word case-insensitive matching with
 * unicode-aware boundaries, {@code toMask} behavior, mask uniqueness/first-seen order, and the
 * empty-config no-op.
 */
class KeywordsCheckTest {

    private static KeywordsCheck check(final KeywordsConfig config) {
        return new KeywordsCheck(config, new Redactor());
    }

    @Test
    @DisplayName("should mask a matched keyword when toMask is true")
    void masksKeywordWhenToMask() {
        final var result = check(new KeywordsConfig(true, List.of("confidential")))
                .run("this is confidential data");

        assertThat(result.entityType()).isEqualTo("keyword");
        assertThat(result.detected()).isTrue();
        assertThat(result.isPassed()).isFalse();
        assertThat(result.maskEntities().get("keyword")).containsExactly("confidential");
        assertThat(result.cleanedValue()).isEqualTo("this is <KEYWORD> data");
    }

    @Test
    @DisplayName("should leave text unchanged but detected when toMask is false")
    void classifiesOnlyWhenNotToMask() {
        final var input = "handle confidential documents with care";
        final var result = check(new KeywordsConfig(false, List.of("confidential"))).run(input);

        assertThat(result.detected()).isTrue();
        assertThat(result.cleanedValue()).isEqualTo(input);
        assertThat(result.maskEntities().get("keyword")).containsExactly("confidential");
    }

    @Test
    @DisplayName("should be a no-op for an empty keyword list")
    void emptyConfigIsNoOp() {
        final var result = check(new KeywordsConfig(true, List.of())).run("plain text here");

        assertThat(result.detected()).isFalse();
        assertThat(result.isPassed()).isTrue();
        assertThat(result.cleanedValue()).isEqualTo("plain text here");
        assertThat(result.maskEntities()).isEmpty();
    }

    @Test
    @DisplayName("should keep case-preserved matches unique and case-folded first-seen")
    void matchesUniqueAndFirstSeen() {
        final var result = check(new KeywordsConfig(true, List.of("urgent")))
                .run("URGENT urgent then urgent URGENT later");

        assertThat(result.maskEntities().get("keyword")).containsExactly("URGENT");
        assertThat(result.cleanedValue())
                .isEqualTo("<KEYWORD> urgent then urgent <KEYWORD> later");
    }

    @Test
    @DisplayName("should not match a keyword embedded inside a longer word")
    void noSubstringMatchInsideWord() {
        final var result = check(new KeywordsConfig(true, List.of("confidential")))
                .run("the confidentiality report is ready");

        assertThat(result.detected()).isFalse();
        assertThat(result.cleanedValue()).isEqualTo("the confidentiality report is ready");
    }

    @Test
    @DisplayName("should match a keyword adjacent to punctuation")
    void matchesPunctuationAdjacentKeyword() {
        final var result = check(new KeywordsConfig(true, List.of("urgent")))
                .run("reply now urgent!");

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("keyword")).containsExactly("urgent");
        assertThat(result.cleanedValue()).isEqualTo("reply now <KEYWORD>!");
    }

    @Test
    @DisplayName("should match a keyword that itself starts with punctuation")
    void matchesPunctuationLeadingKeyword() {
        final var result = check(new KeywordsConfig(true, List.of("!priority")))
                .run("mark!priority handled");

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("keyword")).containsExactly("!priority");
        assertThat(result.cleanedValue()).isEqualTo("mark<KEYWORD> handled");
    }

    @Test
    @DisplayName("should strip trailing punctuation from a configured keyword")
    void stripsTrailingPunctuationFromKeyword() {
        final var result = check(new KeywordsConfig(true, List.of("urgent!")))
                .run("this is an urgent flag");

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("keyword")).containsExactly("urgent");
        assertThat(result.cleanedValue()).isEqualTo("this is an <KEYWORD> flag");
    }

    @Test
    @DisplayName("should be a no-op when every keyword sanitizes to empty")
    void noOpWhenAllKeywordsArePunctuationOnly() {
        final var result = check(new KeywordsConfig(true, List.of("!!!", "..."))).run("!!! ...");

        assertThat(result.detected()).isFalse();
        assertThat(result.maskEntities()).isEmpty();
    }

    @Test
    @DisplayName("should match a Khmer keyword only as a whole word (mask replacing every token occurrence)")
    void matchesKhmerKeywordWholeWordOnly() {
        final var keyword = "\u1797\u17d2\u1793\u17c6\u1796\u17c1\u1789";
        final var glued = "\u1781" + keyword;
        final var input = " " + keyword + " " + glued + " ";

        final var result = check(new KeywordsConfig(true, List.of(keyword))).run(input);

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("keyword")).containsExactly(keyword);
        assertThat(result.cleanedValue()).isEqualTo(" <KEYWORD> \u1781<KEYWORD> ");
    }
}
