package io.github.khezyapp.dpriv.internal;

import io.github.khezyapp.dpriv.policy.SecretPreset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the secret-candidate predicate (design §9.2): length, diversity, entropy and strict
 * boundary rules as a single stateless filter.
 */
class SecretCandidateFilterTest {

    @Test
    @DisplayName("should accept a high-entropy token and reject low-entropy or short ones")
    void appliesCoreChecksInOrder() {
        final var balanced = new SecretCandidateFilter(SecretPreset.BALANCED.params());

        assertThat(balanced.accept("abcdefghijklmnop1234567890")).isTrue();
        assertThat(balanced.accept("session123")).isFalse();
        assertThat(balanced.accept("hello")).isFalse();
    }

    @Test
    @DisplayName("should reject a token glued to an identifier character under strict mode")
    void strictRejectsIdentifierAdjacentToken() {
        final var strict = new SecretCandidateFilter(SecretPreset.STRICT.params());

        assertThat(strict.accept("my_AbC123xYz78qR9", 3, 17)).isFalse();
    }

    @Test
    @DisplayName("should accept a boundary-clean token under strict mode")
    void strictAcceptsBoundaryCleanToken() {
        final var strict = new SecretCandidateFilter(SecretPreset.STRICT.params());

        assertThat(strict.accept("prefix AbC123xYz78qR9", 7, 21)).isTrue();
    }

    @Test
    @DisplayName("should ignore the boundary rule when the preset is not strict")
    void nonStrictIgnoresBoundary() {
        final var balanced = new SecretCandidateFilter(SecretPreset.BALANCED.params());

        assertThat(balanced.accept("my_AbC123xYz78qR9", 3, 17)).isTrue();
    }
}
