package io.github.khezyapp.dpriv.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the bundled LLM prompt templates (design §11.4) as a two-message model: static, input-free system
 * content (guardrail prompt + JSON_SCHEMA + SYSTEM_RULES) that stays provider-cacheable, and the raw input as
 * the user message. Also pins the two-field output contract and NUL tolerance of the policy interpolation.
 */
class LlmPolicyPromptsTest {

    @Test
    @DisplayName("should expose a static jailbreak policy block free of any input placeholder")
    void jailbreakPromptIsStaticPolicy() {
        final var prompt = LlmPolicyPrompts.jailbreakPrompt();

        assertMultiline(prompt);
        assertThat(prompt).contains("jailbreak").doesNotContain("[[");
    }

    @Test
    @DisplayName("should expose a static nsfw policy block free of any input placeholder")
    void nsfwPromptIsStaticPolicy() {
        final var prompt = LlmPolicyPrompts.nsfwPrompt();

        assertMultiline(prompt);
        assertThat(prompt).contains("NSFW").doesNotContain("[[");
    }

    @Test
    @DisplayName("should expose a static topical-alignment policy block with the business-scope marker")
    void topicalPromptIsStaticPolicy() {
        final var prompt = LlmPolicyPrompts.topicalAlignmentPrompt();

        assertMultiline(prompt);
        assertThat(prompt).contains("BUSINESS SCOPE").doesNotContain("[[");
    }

    @Test
    @DisplayName("should build a custom policy block from the caller rules, free of any input placeholder")
    void customPromptContainsRules() {
        final var prompt = LlmPolicyPrompts.customPrompt("do not disclose employee data");

        assertMultiline(prompt);
        assertThat(prompt).contains("POLICY").contains("do not disclose employee data").doesNotContain("[[");
    }

    @Test
    @DisplayName("should tolerate NUL characters in the custom policy rules")
    void customPromptSurvivesNulInRules() {
        assertThatCode(() -> {
            final var prompt = LlmPolicyPrompts.customPrompt("no\0disclosure");
            assertThat(prompt).contains("\0");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should assemble the system message as policy + schema + rules in order")
    void systemMessageAssemblesInOrder() {
        final var system = LlmPolicyPrompts.systemMessage(LlmPolicyPrompts.jailbreakPrompt());

        assertMultiline(system);
        assertThat(system.indexOf("jailbreak"))
                .isNotNegative()
                .isLessThan(system.indexOf("RESPONSE SCHEMA"));
        assertThat(system.indexOf("RESPONSE SCHEMA")).isLessThan(system.indexOf("IMPORTANT"));
        assertThat(system).contains("confidence").contains("flagged");
    }

    @Test
    @DisplayName("should keep the system message byte-identical across calls (provider cache key)")
    void systemMessageIsStable() {
        final var system = LlmPolicyPrompts.systemMessage(LlmPolicyPrompts.jailbreakPrompt());
        final var again = LlmPolicyPrompts.systemMessage(LlmPolicyPrompts.jailbreakPrompt());

        assertThat(system).isEqualTo(again);
    }

    @Test
    @DisplayName("should never embed the classified input in the system message")
    void userInputOnlyLivesInTheUserMessage() {
        final var system = LlmPolicyPrompts.systemMessage(LlmPolicyPrompts.jailbreakPrompt());

        assertThat(system).doesNotContain("varying request text");
    }

    @Test
    @DisplayName("should describe each schema field and forbid markup wrapping in the response")
    void schemaDescribesFieldsAndForbidsMarkup() {
        final var schema = LlmPolicyPrompts.JSON_SCHEMA;

        assertMultiline(schema);
        assertThat(schema)
                .contains("description")
                .contains("How likely the user message violates the guardrail rules")
                .contains("true when the user message violates the guardrail")
                .contains("Do not return the response wrapped in markdown code fences")
                .contains("Output the raw json object and nothing else.")
                .doesNotContain("[[");
    }

    @Test
    @DisplayName("should rank every confidence value from 0 (safe) to 1 (certain violation)")
    void systemRulesRankTheConfidenceScale() {
        final var rules = LlmPolicyPrompts.SYSTEM_RULES;

        assertThat(rules)
                .contains("0.0 — definitely no violation: clearly safe")
                .contains("0.5 — uncertain / ambiguous")
                .contains("1.0 — certain violation: clear and unambiguous")
                .contains("0.6 — slightly more likely than not a violation")
                .contains("0.1 — very unlikely a violation: almost certainly safe");
    }

    @Test
    @DisplayName("should let a caller refine the system rules and place them after the schema")
    void customSystemRulesAreUsedAfterSchema() {
        final var custom = "My custom output rules for better calibration.";
        final var system = LlmPolicyPrompts.systemMessage(LlmPolicyPrompts.jailbreakPrompt(), custom);

        assertThat(system).contains(custom);
        assertThat(system.indexOf(custom)).isGreaterThan(system.indexOf("RESPONSE SCHEMA"));
        assertThat(system).doesNotContain("Use every rank of the scale below");
    }

    @Test
    @DisplayName("should fall back to the built-in system rules when null or blank")
    void blankSystemRulesFallBackToDefaults() {
        final var prompt = LlmPolicyPrompts.jailbreakPrompt();
        final var defaultSystem = LlmPolicyPrompts.systemMessage(prompt);

        assertThat(LlmPolicyPrompts.systemMessage(prompt, null)).isEqualTo(defaultSystem);
        assertThat(LlmPolicyPrompts.systemMessage(prompt, "")).isEqualTo(defaultSystem);
        assertThat(LlmPolicyPrompts.systemMessage(prompt, "   ")).isEqualTo(defaultSystem);
    }

    @Test
    @DisplayName("should reject a null guardrail prompt")
    void nullGuardrailPromptRejected() {
        assertThatThrownBy(() -> LlmPolicyPrompts.systemMessage(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("guardrailPrompt");
    }

    @Test
    @DisplayName("should pin the two-field output contract in both the schema and the rules")
    void constantsPinOutputContract() {
        assertMultiline(LlmPolicyPrompts.JSON_SCHEMA);
        assertMultiline(LlmPolicyPrompts.SYSTEM_RULES);
        assertThat(LlmPolicyPrompts.JSON_SCHEMA).contains("confidence").contains("flagged");
        assertThat(LlmPolicyPrompts.SYSTEM_RULES).contains("confidence").contains("flagged");
    }

    private static void assertMultiline(final String text) {
        assertThat(text).isNotBlank().contains("\n");
    }
}
