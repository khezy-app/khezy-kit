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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

final class DataPrivacyAdvisorStreamTest {

    private static final String EMAIL = "visal@example.com";
    private static final String PLACEHOLDER = "<EMAIL_ADDRESS>";

    @Test
    @DisplayName("stream aggregates and redacts the full output")
    void streamAggregatesAndRedactsFullOutput() {
        final var advisor = DataPrivacyAdvisor.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .scope(ProtectionScope.BOTH)
                .build();
        final var client = ChatClient.builder(new ChunkedChatModel())
                .defaultAdvisors(advisor)
                .build();

        final var responses = client.prompt()
                .user("tell me about " + EMAIL)
                .stream()
                .chatClientResponse()
                .collectList()
                .block();

        final var last = responses.get(responses.size() - 1);
        final var text = last.chatResponse().getResult().getOutput().getText();
        assertThat(text).contains(PLACEHOLDER).doesNotContain(EMAIL);
    }

    @Test
    @DisplayName("INPUT scope streams the raw chunks through without aggregation")
    void streamInputScopeLeavesOutputUnchanged() {
        final var advisor = DataPrivacyAdvisor.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .scope(ProtectionScope.INPUT)
                .build();
        final var client = ChatClient.builder(new ChunkedChatModel())
                .defaultAdvisors(advisor)
                .build();

        final var responses = client.prompt()
                .user("tell me about " + EMAIL)
                .stream()
                .chatClientResponse()
                .collectList()
                .block();

        assertThat(responses).hasSize(3);
        final var text = responses.stream()
                .map(r -> r.chatResponse().getResult().getOutput().getText())
                .collect(Collectors.joining());
        assertThat(text).isEqualTo("my email is " + EMAIL);
    }

    private static final class ChunkedChatModel implements ChatModel {

        @Override
        public ChatResponse call(final Prompt prompt) {
            throw new UnsupportedOperationException("call is not used by the stream test");
        }

        @Override
        public Flux<ChatResponse> stream(final Prompt prompt) {
            final var chunks = List.of("my email is ", "visal@", "example.com");
            return Flux.fromIterable(chunks)
                    .map(text -> new ChatResponse(
                            List.of(new Generation(new AssistantMessage(text)))));
        }
    }
}
