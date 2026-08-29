package io.github.khezyapp.dpriv.springai;

import io.github.khezyapp.dpriv.api.GuardrailsConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof that DataPrivacyAdvisor drives a real {@link ChatClient}: redaction happens
 * before the model sees the prompt (call and stream), output is scrubbed for the caller, and the
 * report is observable from the response context (design §5 G8/G9/G13, §12).
 */
final class EndToEndDataPrivacyAdvisorTest {

    private static final String EMAIL = "visal@example.com";
    private static final String PHONE = "012 345 6789";
    private static final String INPUT = "my email is " + EMAIL + " and phone " + PHONE;
    private static final String EMAIL_PLACEHOLDER = "<EMAIL_ADDRESS>";
    private static final String PHONE_PLACEHOLDER = "<PHONE_NUMBER>";

    @Test
    @DisplayName("call path redacts the user message before the model sees it")
    void chatClientRedactsUserMessageBeforeModelSeesIt() {
        final var stub = new RecordingChatModel("ok");
        final var client = ChatClient.builder(stub)
                .defaultAdvisors(DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build())
                .build();

        client.prompt().user(INPUT).call().content();

        final var seen = stub.lastPrompt().getUserMessages().get(0).getText();
        assertThat(seen)
                .contains(EMAIL_PLACEHOLDER)
                .contains(PHONE_PLACEHOLDER)
                .doesNotContain(EMAIL)
                .doesNotContain(PHONE);
    }

    @Test
    @DisplayName("stream path redacts the user message before the model sees it")
    void chatClientRedactsStreamedUserMessageBeforeModelSeesIt() {
        final var stub = new RecordingChatModel("ok");
        final var client = ChatClient.builder(stub)
                .defaultAdvisors(DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build())
                .build();

        client.prompt().user(INPUT).stream().chatClientResponse().collectList().block();

        final var seen = stub.lastPrompt().getUserMessages().get(0).getText();
        assertThat(seen)
                .contains(EMAIL_PLACEHOLDER)
                .contains(PHONE_PLACEHOLDER)
                .doesNotContain(EMAIL);
    }

    @Test
    @DisplayName("OUTPUT scope scrubs the response the caller receives")
    void outputScopeRedactsResponseForCaller() {
        final var stub = new RecordingChatModel("your email is " + EMAIL);
        final var client = ChatClient.builder(stub)
                .defaultAdvisors(DataPrivacyAdvisor.builder()
                        .config(GuardrailsConfig.DEFAULTS)
                        .scope(ProtectionScope.BOTH)
                        .build())
                .build();

        final var content = client.prompt().user("hello").call().content();

        assertThat(content).contains(EMAIL_PLACEHOLDER).doesNotContain(EMAIL);
    }

    @Test
    @DisplayName("report is readable from the response context")
    void reportIsReadableFromResponseContext() {
        final var stub = new RecordingChatModel("ok");
        final var client = ChatClient.builder(stub)
                .defaultAdvisors(DataPrivacyAdvisor.builder().config(GuardrailsConfig.DEFAULTS).build())
                .build();

        final var response = client.prompt().user(INPUT).call().chatClientResponse();

        final var report = (RedactionReport) response.context().get(DataPrivacyAdvisor.CONTEXT_KEY);
        assertThat(report.redacted()).isTrue();
        assertThat(report.entityTypes()).contains("pii_email_address", "pii_phone_number");
    }

    private static final class RecordingChatModel implements ChatModel {

        private final String responseText;
        private final AtomicReference<Prompt> lastPrompt = new AtomicReference<>();
        private final AtomicInteger callCount = new AtomicInteger();

        private RecordingChatModel(final String responseText) {
            this.responseText = responseText;
        }

        private Prompt lastPrompt() {
            return lastPrompt.get();
        }

        @Override
        public ChatResponse call(final Prompt prompt) {
            lastPrompt.set(prompt);
            callCount.incrementAndGet();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(responseText))));
        }

        @Override
        public Flux<ChatResponse> stream(final Prompt prompt) {
            lastPrompt.set(prompt);
            callCount.incrementAndGet();
            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage(responseText)))));
        }
    }
}
