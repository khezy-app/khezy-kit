package io.github.khezyapp.dpriv.api;

import java.util.List;
import java.util.Map;

/**
 * Composition root for the full data-privacy configuration (design §5.4, §9.1). Every section is
 * optional and defaults to a sensible value.
 *
 * @param pii            PII coverage configuration
 * @param secrets        secret-scanning configuration
 * @param urls           URL-validation configuration
 * @param keywords       keyword-filtering configuration
 * @param customRegexes  user-named custom regex rules
 * @param llm            generic LLM check configuration
 * @param jailbreak      LLM jailbreak check configuration
 * @param nsfw           LLM NSFW check configuration
 * @param topical        LLM topical check configuration
 * @param booleanOptions reserved option bag (no keys defined; keep empty)
 */
public record GuardrailsConfig(
        PiiConfig pii,
        SecretConfig secrets,
        UrlsConfig urls,
        KeywordsConfig keywords,
        List<CustomRegexConfig> customRegexes,
        LlmCheckConfig llm,
        LlmCheckConfig jailbreak,
        LlmCheckConfig nsfw,
        LlmCheckConfig topical,
        Map<String, Boolean> booleanOptions) {

    /**
     * Creates a fluent builder pre-filled with each section's defaults.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Defaults: every section uses its default, {@code booleanOptions} is empty.
     */
    public static final GuardrailsConfig DEFAULTS = builder().build();

    /**
     * Fluent builder for {@link GuardrailsConfig}.
     */
    public static final class Builder {

        private PiiConfig pii = PiiConfig.DEFAULTS;
        private SecretConfig secrets = SecretConfig.DEFAULTS;
        private UrlsConfig urls = UrlsConfig.DEFAULTS;
        private KeywordsConfig keywords = new KeywordsConfig(false, List.of());
        private List<CustomRegexConfig> customRegexes = List.of();
        private LlmCheckConfig llm = LlmCheckConfig.DEFAULTS;
        private LlmCheckConfig jailbreak = LlmCheckConfig.DEFAULTS;
        private LlmCheckConfig nsfw = LlmCheckConfig.DEFAULTS;
        private LlmCheckConfig topical = LlmCheckConfig.DEFAULTS;
        private Map<String, Boolean> booleanOptions = Map.of();

        private Builder() {
        }

        public Builder pii(final PiiConfig value) {
            this.pii = value;
            return this;
        }

        public Builder secrets(final SecretConfig value) {
            this.secrets = value;
            return this;
        }

        public Builder urls(final UrlsConfig value) {
            this.urls = value;
            return this;
        }

        public Builder keywords(final KeywordsConfig value) {
            this.keywords = value;
            return this;
        }

        public Builder customRegexes(final List<CustomRegexConfig> value) {
            this.customRegexes = value;
            return this;
        }

        public Builder llm(final LlmCheckConfig value) {
            this.llm = value;
            return this;
        }

        public Builder jailbreak(final LlmCheckConfig value) {
            this.jailbreak = value;
            return this;
        }

        public Builder nsfw(final LlmCheckConfig value) {
            this.nsfw = value;
            return this;
        }

        public Builder topical(final LlmCheckConfig value) {
            this.topical = value;
            return this;
        }

        public Builder booleanOptions(final Map<String, Boolean> value) {
            this.booleanOptions = value;
            return this;
        }

        public GuardrailsConfig build() {
            return new GuardrailsConfig(pii, secrets, urls, keywords, customRegexes,
                    llm, jailbreak, nsfw, topical, booleanOptions);
        }
    }
}
