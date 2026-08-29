package io.github.khezyapp.dpriv.checks;

import io.github.khezyapp.dpriv.api.SecretConfig;
import io.github.khezyapp.dpriv.policy.SecretPreset;
import io.github.khezyapp.dpriv.redact.Redactor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the secret/key detection check (design §9.2): preset behavior, redaction to
 * {@code <SECRET>}, mask uniqueness/first-seen order, and custom pattern merging.
 */
class SecretKeysCheckTest {

    private static SecretKeysCheck balancedCheck() {
        return new SecretKeysCheck(SecretConfig.DEFAULTS, new Redactor());
    }

    private static SecretKeysCheck strictCheck() {
        return new SecretKeysCheck(new SecretConfig(SecretPreset.STRICT, Map.of()), new Redactor());
    }

    @Test
    @DisplayName("should flag a high-entropy token and redact it under BALANCED")
    void balancedFlagsHighEntropyToken() {
        final var check = balancedCheck();

        final var result = check.run("token sk-abcdefghijklmnop1234567890 passed");

        assertThat(result.entityType()).isEqualTo("secret");
        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("secret")).containsExactly("abcdefghijklmnop1234567890");
        assertThat(result.cleanedValue()).isEqualTo("token sk-<SECRET> passed");
    }

    @Test
    @DisplayName("should reject low-entropy and too-short tokens under BALANCED")
    void balancedRejectsLowEntropyAndShort() {
        final var check = balancedCheck();

        assertThat(check.run("session123").detected()).isFalse();
        assertThat(check.run("hello").detected()).isFalse();
    }

    @Test
    @DisplayName("should reject an identifier-adjacent token under STRICT but accept it under BALANCED")
    void strictRejectsGluedTokenBalancedAccepts() {
        final var input = "my_AbC123xYz78qR9";

        final var balanced = balancedCheck().run(input);
        final var strict = strictCheck().run(input);

        assertThat(balanced.detected()).isTrue();
        assertThat(strict.detected()).isFalse();
    }

    @Test
    @DisplayName("should redact the reported secret to <SECRET> in cleanedValue")
    void redactsSecretToPlaceholder() {
        final var check = balancedCheck();

        final var result = check.run("my-api-key=AbC123xYz78qR9 attached");

        assertThat(result.cleanedValue()).isEqualTo("my-api-key=<SECRET> attached");
        assertThat(result.cleanedValue()).doesNotContain("AbC123xYz78qR9");
    }

    @Test
    @DisplayName("should keep tokens unique and in first-seen order")
    void tokensUniqueAndFirstSeenOrder() {
        final var check = balancedCheck();
        final var input = "my_AbC123xYz78qR9 then my_gpk3Kd0QxZ9mN4 then my_AbC123xYz78qR9";

        final var result = check.run(input);

        assertThat(result.maskEntities().get("secret"))
                .containsExactly("AbC123xYz78qR9", "gpk3Kd0QxZ9mN4");
    }

    @Test
    @DisplayName("should not flag plain prose")
    void noDetectionOnPlainProse() {
        final var check = balancedCheck();

        final var result = check.run("SOK and VISAL met in Phnom Penh for lunch at Siem Reap");

        assertThat(result.detected()).isFalse();
        assertThat(result.maskEntities()).isEmpty();
    }

    @Test
    @DisplayName("should merge custom pattern matches into the secret bucket")
    void customPatternMergesIntoSecretBucket() {
        final var pattern = Pattern.compile("API_KEY_[A-Z0-9_]+");
        final var config = new SecretConfig(SecretPreset.BALANCED, Map.of("api_doc", List.of(pattern)));
        final var check = new SecretKeysCheck(config, new Redactor());

        final var result = check.run("provide API_KEY_ABC123 to the service");

        assertThat(result.detected()).isTrue();
        assertThat(result.maskEntities().get("secret")).containsExactly("API_KEY_ABC123");
        assertThat(result.cleanedValue()).isEqualTo("provide <SECRET> to the service");
    }
}
