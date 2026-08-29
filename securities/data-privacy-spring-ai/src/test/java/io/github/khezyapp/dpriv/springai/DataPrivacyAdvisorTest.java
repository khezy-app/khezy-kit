package io.github.khezyapp.dpriv.springai;

import io.github.khezyapp.dpriv.api.Guardrails;
import io.github.khezyapp.dpriv.api.GuardrailsConfig;
import io.github.khezyapp.dpriv.api.PiiConfig;
import io.github.khezyapp.dpriv.api.PiiCoverage;
import io.github.khezyapp.dpriv.springai.exception.RedactionException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.Ordered;
import org.springframework.util.MimeType;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class DataPrivacyAdvisorTest {

    private static final String EMAIL = "visal@example.com";
    private static final String PII_TEXT = "my email is " + EMAIL;
    private static final String PLACEHOLDER = "<EMAIL_ADDRESS>";

    private static ChatClientRequest requestOf(final Message... messages) {
        return ChatClientRequest.builder()
                .prompt(new Prompt(List.of(messages)))
                .context(new LinkedHashMap<String, Object>())
                .build();
    }

    private static ChatClientResponse responseOf(final String text) {
        final var chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(text))))
                .build();
        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(new LinkedHashMap<String, Object>())
                .build();
    }

    private static Guardrails r4Guardrails() {
        final var config = GuardrailsConfig.builder()
                .pii(new PiiConfig(PiiCoverage.SELECTED, null, List.of(), true))
                .build();
        return Guardrails.builder().config(config).build();
    }

    private static String lastUserText(final ChatClientRequest request) {
        return request.prompt().getUserMessages().get(0).getText();
    }

    @Test
    @DisplayName("before redacts the user message text in place")
    void beforeRedactsUserMessageInPlace() {
        final var advisor = DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build();
        final var result = advisor.before(requestOf(new UserMessage(PII_TEXT)), null);

        assertThat(lastUserText(result)).contains(PLACEHOLDER).doesNotContain(EMAIL);
    }

    @Test
    @DisplayName("before leaves the system message instance untouched")
    void beforeLeavesSystemMessageUntouched() {
        final var system = new SystemMessage("system persona");
        final var advisor = DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build();
        final var result = advisor.before(requestOf(system, new UserMessage(PII_TEXT)), null);

        assertThat(result.prompt().getInstructions().get(0)).isSameAs(system);
    }

    @Test
    @DisplayName("before preserves media and metadata while redacting text")
    void beforePreservesMediaAndMetadata() {
        final var media = List.of(new Media(MimeType.valueOf("text/plain"),
                URI.create("file:///x.txt")));
        final Map<String, Object> metadata = Map.of("author", "visal");
        final var user = UserMessage.builder()
                .text(PII_TEXT)
                .media(media)
                .metadata(metadata)
                .build();
        final var advisor = DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build();

        final var result = advisor.before(requestOf(user), null);
        final var redacted = result.prompt().getUserMessages().get(0);

        assertThat(redacted.getText()).contains(PLACEHOLDER);
        assertThat(redacted.getMedia()).isEqualTo(media);
        assertThat(redacted.getMetadata()).containsEntry("author", "visal");
    }

    @Test
    @DisplayName("ALL mode redacts every user message")
    void allModeRedactsEveryUserMessage() {
        final var advisor = DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build();
        final var result = advisor.before(
                requestOf(new UserMessage(PII_TEXT), new UserMessage(PII_TEXT)), null);

        assertThat(result.prompt().getUserMessages())
                .allSatisfy(m -> assertThat(m.getText()).contains(PLACEHOLDER).doesNotContain(EMAIL));
    }

    @Test
    @DisplayName("LAST_ONLY mode redacts only the last user message")
    void lastOnlyModeRedactsOnlyLastUserMessage() {
        final var advisor = DataPrivacyAdvisor.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .mode(RedactMode.LAST_ONLY)
                .build();
        final var result = advisor.before(
                requestOf(new UserMessage("hello"), new UserMessage(PII_TEXT)), null);

        final var users = result.prompt().getUserMessages();
        assertThat(users.get(0).getText()).isEqualTo("hello");
        assertThat(users.get(1).getText()).contains(PLACEHOLDER);
    }

    @Test
    @DisplayName("LAST_ONLY falls back to the last user message when the last is a tool response")
    void lastOnlyFallsBackToLastUserWhenLastIsToolResponse() {
        final var toolResponse = ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse("id", "name", "data")))
                .build();
        final var advisor = DataPrivacyAdvisor.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .mode(RedactMode.LAST_ONLY)
                .build();
        final var result = advisor.before(
                requestOf(new UserMessage(PII_TEXT), toolResponse), null);

        assertThat(lastUserText(result)).contains(PLACEHOLDER);
    }

    @Test
    @DisplayName("before skips a pure-media user message with no text content")
    void beforeSkipsPureMediaUserMessage() {
        final var media = List.of(new Media(MimeType.valueOf("text/plain"),
                URI.create("file:///x.txt")));
        final var user = UserMessage.builder().text("").media(media).build();
        final var advisor = DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build();

        final var result = advisor.before(requestOf(user), null);

        assertThat(result.prompt().getUserMessages().get(0).getText()).isEmpty();
        assertThat(result.prompt().getUserMessages().get(0).getMedia()).isEqualTo(media);
    }

    @Test
    @DisplayName("before writes a RedactionReport carrying entity types")
    void beforeWritesReportWithEntityTypes() {
        final var advisor = DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build();
        final var result = advisor.before(requestOf(new UserMessage(PII_TEXT)), null);

        final var report = (RedactionReport) result.context().get(DataPrivacyAdvisor.CONTEXT_KEY);
        assertThat(report.redacted()).isTrue();
        assertThat(report.entityTypes()).contains("pii_email_address");
    }

    @Test
    @DisplayName("before writes a NONE report on clean text")
    void beforeWritesNoneReportOnCleanText() {
        final var advisor = DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build();
        final var result = advisor.before(requestOf(new UserMessage("hello there")), null);

        final var report = (RedactionReport) result.context().get(DataPrivacyAdvisor.CONTEXT_KEY);
        assertThat(report).isEqualTo(RedactionReport.NONE);
    }

    @Test
    @DisplayName("before fail-closed throws RedactionException and writes nothing")
    void beforeFailClosedThrowsRedactionException() {
        final var advisor = DataPrivacyAdvisor.builder().guardrails(r4Guardrails()).build();

        assertThatThrownBy(() -> advisor.before(requestOf(new UserMessage(PII_TEXT)), null))
                .isInstanceOf(RedactionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("before fail-open passes the original text through")
    void beforeFailOpenPassesOriginalText() {
        final var advisor = DataPrivacyAdvisor.builder()
                .guardrails(r4Guardrails())
                .failOnError(false)
                .build();
        final var result = advisor.before(requestOf(new UserMessage(PII_TEXT)), null);

        assertThat(lastUserText(result)).contains(EMAIL);
    }

    @Test
    @DisplayName("after redacts the response text")
    void afterRedactsResponseText() {
        final var advisor = DataPrivacyAdvisor.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .scope(ProtectionScope.OUTPUT)
                .build();
        final var result = advisor.after(responseOf(PII_TEXT), null);

        assertThat(result.chatResponse().getResult().getOutput().getText())
                .contains(PLACEHOLDER)
                .doesNotContain(EMAIL);
    }

    @Test
    @DisplayName("after ignores a tool-call-only response (null output text)")
    void afterIgnoresToolCallOnlyResponse() {
        final var advisor = DataPrivacyAdvisor.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .scope(ProtectionScope.OUTPUT)
                .build();
        final var response = responseOf((String) null);

        assertThat(advisor.after(response, null)).isSameAs(response);
    }

    @Test
    @DisplayName("after fail-closed throws RedactionException")
    void afterFailClosedThrowsRedactionException() {
        final var advisor = DataPrivacyAdvisor.builder()
                .guardrails(r4Guardrails())
                .scope(ProtectionScope.OUTPUT)
                .build();

        assertThatThrownBy(() -> advisor.after(responseOf(PII_TEXT), null))
                .isInstanceOf(RedactionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("OUTPUT scope leaves before untouched")
    void outputScopeLeavesBeforeUntouched() {
        final var advisor = DataPrivacyAdvisor.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .scope(ProtectionScope.OUTPUT)
                .build();
        final var request = requestOf(new UserMessage(PII_TEXT));

        assertThat(advisor.before(request, null)).isSameAs(request);
    }

    @Test
    @DisplayName("BOTH scope redacts input and output")
    void bothScopeRedactsInputAndOutput() {
        final var advisor = DataPrivacyAdvisor.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .scope(ProtectionScope.BOTH)
                .build();
        final var afterBefore = advisor.before(requestOf(new UserMessage(PII_TEXT)), null);
        assertThat(lastUserText(afterBefore)).contains(PLACEHOLDER);

        final var afterAfter = advisor.after(responseOf(PII_TEXT), null);
        assertThat(afterAfter.chatResponse().getResult().getOutput().getText()).contains(PLACEHOLDER);
    }

    @Test
    @DisplayName("config(DEFAULTS) behaves identically to the equivalent guardrails(...) instance")
    void configConvenienceEqualsGuardrailsInstance() {
        final var viaConfig = DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build();
        final var viaGuardrails = DataPrivacyAdvisor.builder()
                .guardrails(Guardrails.builder().config(GuardrailsConfig.DEFAULTS).build())
                .build();

        final var r1 = viaConfig.before(requestOf(new UserMessage(PII_TEXT)), null);
        final var r2 = viaGuardrails.before(requestOf(new UserMessage(PII_TEXT)), null);

        assertThat(lastUserText(r1)).isEqualTo(lastUserText(r2));
    }

    @Test
    @DisplayName("defaults are INPUT scope, ALL mode, fail-closed, HIGHEST_PRECEDENCE")
    void defaultsAreInputAllFailClosedHighestPrecedence() {
        final var advisor = DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build();

        assertThat(advisor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(advisor.getName()).isEqualTo("DataPrivacyAdvisor");
        final var redacted = advisor.before(requestOf(new UserMessage(PII_TEXT)), null);
        assertThat(lastUserText(redacted)).contains(PLACEHOLDER);
        final var untouched = advisor.after(responseOf(PII_TEXT), null);
        assertThat(untouched.chatResponse().getResult().getOutput().getText()).contains(EMAIL);
    }

    @Test
    @DisplayName("build() fails fast without guardrails or config")
    void buildFailsFastWithoutGuardrailsOrConfig() {
        assertThatThrownBy(() -> DataPrivacyAdvisor.builder().build())
                .isInstanceOf(IllegalStateException.class);
    }
}
