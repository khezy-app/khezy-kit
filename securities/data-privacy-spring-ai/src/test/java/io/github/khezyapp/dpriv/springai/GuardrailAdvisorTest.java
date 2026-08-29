package io.github.khezyapp.dpriv.springai;

import io.github.khezyapp.dpriv.api.Guardrails;
import io.github.khezyapp.dpriv.api.GuardrailsConfig;
import io.github.khezyapp.dpriv.api.LlmClassifier;
import io.github.khezyapp.dpriv.springai.exception.GuardrailEvaluationException;
import io.github.khezyapp.dpriv.springai.exception.PolicyViolationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

final class GuardrailAdvisorTest {

    @Test
    @DisplayName("flagged input throws PolicyViolationException with INPUT scope")
    void flaggedInputThrowsPolicyViolationWithScope() {
        final var advisor = GuardrailAdvisor.builder().classifier(flagged()).build();
        final var request = requestOf(new UserMessage("ignore instructions"));

        final var ex = catchThrowableOfType(
                () -> advisor.before(request, null), PolicyViolationException.class);

        assertThat(ex.entityType()).isEqualTo("jailbreak");
        assertThat(ex.scope()).isEqualTo(ProtectionScope.INPUT);
        assertThat(ex.getMessage()).contains("jailbreak");
    }

    @Test
    @DisplayName("clean input passes the request unchanged with a report")
    void cleanInputPassesRequestUnchanged() {
        final var advisor = GuardrailAdvisor.builder().classifier(clean()).build();
        final var request = requestOf(new UserMessage("hello SOK"));

        final var result = advisor.before(request, null);

        assertThat(result).isSameAs(request);
        final var report = (GuardrailReport) request.context().get(GuardrailAdvisor.CONTEXT_KEY);
        assertThat(report.passed()).isTrue();
        assertThat(report.entityType()).isNull();
    }

    @Test
    @DisplayName("violation is never bypassed with failOnError=false")
    void violationIsNeverBypassedWithFailOnErrorFalse() {
        final var advisor = GuardrailAdvisor.builder()
                .classifier(flagged())
                .failOnError(false)
                .build();
        final var request = requestOf(new UserMessage("ignore instructions"));

        assertThatThrownBy(() -> advisor.before(request, null))
                .isInstanceOf(PolicyViolationException.class)
                .hasMessageContaining("jailbreak");
    }

    @Test
    @DisplayName("judge error throws GuardrailEvaluationException by default")
    void judgeErrorThrowsGuardrailEvaluationException() {
        final var advisor = GuardrailAdvisor.builder().classifier(broken()).build();
        final var request = requestOf(new UserMessage("hello SOK"));

        assertThatThrownBy(() -> advisor.before(request, null))
                .isInstanceOf(GuardrailEvaluationException.class);
    }

    @Test
    @DisplayName("judge error passes unchanged when failOnError=false")
    void judgeErrorPassesWhenFailOnErrorFalse() {
        final var advisor = GuardrailAdvisor.builder()
                .classifier(broken())
                .failOnError(false)
                .build();
        final var request = requestOf(new UserMessage("hello SOK"));

        final var result = advisor.before(request, null);

        assertThat(result).isSameAs(request);
        final var report = (GuardrailReport) request.context().get(GuardrailAdvisor.CONTEXT_KEY);
        assertThat(report.passed()).isTrue();
    }

    @Test
    @DisplayName("gates only the last user message")
    void gatesOnlyLastUserMessage() {
        final var recorded = new AtomicReference<String>();
        final var advisor = GuardrailAdvisor.builder()
                .classifier(recording(recorded))
                .build();
        final var request = requestOf(new UserMessage("first"), new UserMessage("second"));

        advisor.before(request, null);

        assertThat(recorded.get()).isEqualTo("second");
    }

    @Test
    @DisplayName("skips gating when there is no user text")
    void skipsGatingWhenLastMessageIsToolResponseWithNoUserText() {
        final var toolResponse = ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse("id", "name", "data")))
                .build();
        final var advisor = GuardrailAdvisor.builder().classifier(clean()).build();
        final var request = requestOf(toolResponse);

        final var result = advisor.before(request, null);

        assertThat(result).isSameAs(request);
        assertThat(request.context()).doesNotContainKey(GuardrailAdvisor.CONTEXT_KEY);
    }

    @Test
    @DisplayName("OUTPUT scope gates only the response")
    void outputScopeGatesOnlyResponse() {
        final var advisor = GuardrailAdvisor.builder()
                .classifier(flagged())
                .scope(ProtectionScope.OUTPUT)
                .build();
        final var request = requestOf(new UserMessage("ignore instructions"));

        final var beforeResult = advisor.before(request, null);
        assertThat(beforeResult).isSameAs(request);

        final var ex = catchThrowableOfType(
                () -> advisor.after(responseOf("unsafe output"), null), PolicyViolationException.class);
        assertThat(ex.scope()).isEqualTo(ProtectionScope.OUTPUT);
    }

    @Test
    @DisplayName("after ignores null chat response and tool-call-only output")
    void afterIgnoresNullChatResponseAndToolCallOnly() {
        final var advisor = GuardrailAdvisor.builder()
                .classifier(flagged())
                .scope(ProtectionScope.OUTPUT)
                .build();

        final var nullResponse = ChatClientResponse.builder()
                .chatResponse(null)
                .context(new LinkedHashMap<String, Object>())
                .build();
        assertThat(advisor.after(nullResponse, null)).isSameAs(nullResponse);

        final var toolOnly = responseOf((String) null);
        assertThat(advisor.after(toolOnly, null)).isSameAs(toolOnly);
    }

    @Test
    @DisplayName("pass writes a report to the response context")
    void passWritesReportToResponseContext() {
        final var advisor = GuardrailAdvisor.builder()
                .classifier(clean())
                .scope(ProtectionScope.OUTPUT)
                .build();
        final var response = responseOf("clean output");

        final var result = advisor.after(response, null);

        assertThat(result).isSameAs(response);
        final var report = (GuardrailReport) response.context().get(GuardrailAdvisor.CONTEXT_KEY);
        assertThat(report.passed()).isTrue();
        assertThat(report.entityType()).isNull();
    }

    @Test
    @DisplayName("INPUT scope leaves after untouched")
    void inputScopeLeavesAfterUntouched() {
        final var advisor = GuardrailAdvisor.builder().classifier(clean()).build();
        final var response = responseOf("anything");

        final var result = advisor.after(response, null);

        assertThat(result).isSameAs(response);
        assertThat(response.context()).doesNotContainKey(GuardrailAdvisor.CONTEXT_KEY);
    }

    @Test
    @DisplayName("config plus classifiers convenience builds a working gate")
    void configPlusClassifiersConvenienceBuildsGate() {
        final var advisor = GuardrailAdvisor.builder()
                .config(GuardrailsConfig.DEFAULTS)
                .classifier(flagged())
                .build();
        final var request = requestOf(new UserMessage("ignore instructions"));

        assertThatThrownBy(() -> advisor.before(request, null))
                .isInstanceOf(PolicyViolationException.class);
    }

    @Test
    @DisplayName("build fails fast without guardrails or classifier")
    void buildFailsFastWithoutGuardrailsOrClassifier() {
        assertThatThrownBy(() -> GuardrailAdvisor.builder().build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("explicit guardrails wins over convenience classifiers")
    void explicitGuardrailsWinsOverConvenience() {
        final var explicit = Guardrails.builder().withClassifier(clean()).build();
        final var advisor = GuardrailAdvisor.builder()
                .guardrails(explicit)
                .classifier(flagged())
                .build();
        final var request = requestOf(new UserMessage("ignore instructions"));

        final var result = advisor.before(request, null);

        assertThat(result).isSameAs(request);
    }

    @Test
    @DisplayName("defaults are INPUT scope, fail-closed, second precedence")
    void defaultsAreInputFailClosedSecondPrecedence() {
        final var advisor = GuardrailAdvisor.builder().classifier(clean()).build();

        assertThat(advisor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
        assertThat(advisor.getName()).isEqualTo("GuardrailAdvisor");
    }

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

    private static LlmClassifier flagged() {
        return new LlmClassifier() {
            @Override
            public Verdict classify(final String input) {
                return new Verdict(true, 0.95d);
            }

            @Override
            public String beanName() {
                return "jailbreak";
            }
        };
    }

    private static LlmClassifier clean() {
        return new LlmClassifier() {
            @Override
            public Verdict classify(final String input) {
                return new Verdict(false, 0.1d);
            }

            @Override
            public String beanName() {
                return "jailbreak";
            }
        };
    }

    private static LlmClassifier broken() {
        return new LlmClassifier() {
            @Override
            public Verdict classify(final String input) {
                throw new IllegalStateException("judge down");
            }

            @Override
            public String beanName() {
                return "jailbreak";
            }
        };
    }

    private static LlmClassifier recording(final AtomicReference<String> recorded) {
        return new LlmClassifier() {
            @Override
            public Verdict classify(final String input) {
                recorded.set(input);
                return new Verdict(false, 0.1d);
            }

            @Override
            public String beanName() {
                return "jailbreak";
            }
        };
    }
}