package io.github.khezyapp.dpriv.redact;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the in-memory redactor (design §7.2): literal, longest-first, placeholder-protected
 * replacement that is pure and deterministic.
 */
class RedactorTest {

    @Test
    @DisplayName("should replace an email token with its <ENTITY> placeholder")
    void replacesEmailWithPlaceholder() {
        final var mask = new LinkedHashMap<String, List<String>>();
        mask.put("pii_email_address", List.of("visal@example.com"));
        final var redactor = new Redactor();

        final var result = redactor.redact("contact visal@example.com today", mask);

        assertThat(result).isEqualTo("contact <EMAIL_ADDRESS> today");
    }

    @Test
    @DisplayName("should let the longer token win over a contained shorter token")
    void longerTokenWinsOverContainedShorter() {
        final var mask = new LinkedHashMap<String, List<String>>();
        mask.put("pii_email_address", List.of("visal@example.com"));
        mask.put("secret", List.of("example"));
        final var redactor = new Redactor();

        final var result = redactor.redact("mail visal@example.com here", mask);

        assertThat(result).isEqualTo("mail <EMAIL_ADDRESS> here");
    }

    @Test
    @DisplayName("should not corrupt a placeholder with a token sharing its text")
    void doesNotCorruptPlaceholderWithSubstringToken() {
        final var mask = new LinkedHashMap<String, List<String>>();
        mask.put("pii_email_address", List.of("visal@example.com"));
        mask.put("secret", List.of("EMAIL"));
        final var redactor = new Redactor();

        final var result = redactor.redact("EMAIL + visal@example.com", mask);

        assertThat(result).isEqualTo("<SECRET> + <EMAIL_ADDRESS>");
    }

    @Test
    @DisplayName("should be deterministic for identical inputs")
    void isDeterministic() {
        final var mask = new LinkedHashMap<String, List<String>>();
        mask.put("pii_email_address", List.of("visal@example.com"));
        mask.put("pii_location", List.of("Phnom Penh", "Siem Reap"));
        final var redactor = new Redactor();
        final var input = "visal@example.com visits Phnom Penh and Siem Reap";

        final var first = redactor.redact(input, mask);
        final var second = redactor.redact(input, mask);

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("should redact every occurrence of a token")
    void redactsEveryOccurrence() {
        final var mask = new LinkedHashMap<String, List<String>>();
        mask.put("secret", List.of("SOK"));
        final var redactor = new Redactor();

        final var result = redactor.redact("SOK met SOK in Phnom Penh", mask);

        assertThat(result).isEqualTo("<SECRET> met <SECRET> in Phnom Penh");
    }

    @Test
    @DisplayName("should return the input unchanged when maskEntities is empty")
    void returnsInputWhenNoMasks() {
        final var redactor = new Redactor();

        final var result = redactor.redact("nothing to hide here", Map.of());

        assertThat(result).isEqualTo("nothing to hide here");
    }
}
