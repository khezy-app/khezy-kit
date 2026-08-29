package io.github.khezyapp.dpriv.springai;

import io.github.khezyapp.dpriv.api.Guardrails;
import io.github.khezyapp.dpriv.api.GuardrailsConfig;
import io.github.khezyapp.dpriv.api.GuardrailsOutcome;
import io.github.khezyapp.dpriv.api.LlmClassifier;
import io.github.khezyapp.dpriv.api.Operation;
import io.github.khezyapp.dpriv.springai.exception.GuardrailEvaluationException;
import io.github.khezyapp.dpriv.springai.exception.PolicyViolationException;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * PREVENT pattern (design §2, §7): LLM-as-judge gating of the last user message (INPUT) and/or
 * the model response (OUTPUT) via Operation.CLASSIFY. Never modifies the request: it allows it
 * or blocks it (PolicyViolationException). Judge failures fail-closed via
 * GuardrailEvaluationException unless failOnError=false (G14, G15). INPUT-only advisors
 * stream raw deltas; OUTPUT/BOTH aggregate the full response so the judge sees the complete text.
 */
public final class GuardrailAdvisor implements BaseAdvisor {

    public static final String CONTEXT_KEY = "io.github.khezyapp.dpriv.springai.guardrailReport";

    private final Guardrails guardrails;
    private final ProtectionScope scope;
    private final boolean failOnError;
    private final int order;

    private GuardrailAdvisor(final Builder builder) {
        this.guardrails = Objects.requireNonNull(builder.guardrails, "guardrails");
        this.scope = Objects.requireNonNull(builder.scope, "scope");
        this.failOnError = builder.failOnError;
        this.order = builder.order;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ChatClientRequest before(final ChatClientRequest request,
                                    final AdvisorChain chain) {
        if (!appliesToInput()) {
            return request;
        }
        final var target = selectTarget(request.prompt());
        if (Objects.isNull(target)) {
            return request;
        }
        final var outcome = guardrails.run(target, Operation.CLASSIFY);
        interpretInput(outcome);
        request.context().put(CONTEXT_KEY, new GuardrailReport(true, outcome.entityType()));
        return request;
    }

    @Override
    public ChatClientResponse after(final ChatClientResponse response,
                                    final AdvisorChain chain) {
        if (!appliesToOutput()) {
            return response;
        }
        final var chatResponse = response.chatResponse();
        if (Objects.isNull(chatResponse)) {
            return response;
        }
        final var result = chatResponse.getResult();
        if (Objects.isNull(result)) {
            return response;
        }
        final var output = result.getOutput();
        final var text = output.getText();
        if (Objects.isNull(text)) {
            return response;
        }
        final var outcome = guardrails.run(text, Operation.CLASSIFY);
        interpretOutput(outcome);
        response.context().put(CONTEXT_KEY, new GuardrailReport(true, outcome.entityType()));
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(final ChatClientRequest request,
                                                 final StreamAdvisorChain chain) {
        final var processed = Mono.just(request)
                .publishOn(getScheduler())
                .map(r -> this.before(r, chain));
        if (!appliesToOutput()) {
            return processed.flatMapMany(chain::nextStream);
        }
        final var ref = new AtomicReference<ChatClientResponse>();
        return processed.flatMapMany(chain::nextStream)
                .transform(flux -> {
                    final var aggregated = new ChatClientMessageAggregator()
                            .aggregateChatClientResponse(flux, a -> ref.set(this.after(a, chain)));
                    return aggregated.then(Mono.defer(() -> Mono.justOrEmpty(ref.get())));
                });
    }

    @Override
    public String getName() {
        return "GuardrailAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }

    private boolean appliesToInput() {
        return scope == ProtectionScope.INPUT || scope == ProtectionScope.BOTH;
    }

    private boolean appliesToOutput() {
        return scope == ProtectionScope.OUTPUT || scope == ProtectionScope.BOTH;
    }

    private String selectTarget(final Prompt prompt) {
        final var last = prompt.getLastUserOrToolResponseMessage();
        if (last.getMessageType() == MessageType.USER && Objects.nonNull(last.getText())) {
            return last.getText();
        }
        final var users = prompt.getUserMessages();
        for (var i = users.size() - 1; i >= 0; i--) {
            final var text = users.get(i).getText();
            if (Objects.nonNull(text)) {
                return text;
            }
        }
        return null;
    }

    private void interpretInput(final GuardrailsOutcome outcome) {
        if (!outcome.detected()) {
            return;
        }
        if (outcome.messages().isEmpty()) {
            throw new PolicyViolationException(outcome.entityType(), ProtectionScope.INPUT);
        }
        if (failOnError) {
            throw new GuardrailEvaluationException(
                    "guardrail evaluation failed: " + outcome.entityType()
                            + " (messages=" + outcome.messages() + ")",
                    null);
        }
    }

    private void interpretOutput(final GuardrailsOutcome outcome) {
        if (!outcome.detected()) {
            return;
        }
        if (outcome.messages().isEmpty()) {
            throw new PolicyViolationException(outcome.entityType(), ProtectionScope.OUTPUT);
        }
        if (failOnError) {
            throw new GuardrailEvaluationException(
                    "guardrail evaluation failed: " + outcome.entityType()
                            + " (messages=" + outcome.messages() + ")",
                    null);
        }
    }

    public static final class Builder {

        private Guardrails guardrails;
        private GuardrailsConfig config;
        private final List<LlmClassifier> classifiers = new ArrayList<>();
        private ProtectionScope scope = ProtectionScope.INPUT;
        private boolean failOnError = true;
        private int order = Ordered.HIGHEST_PRECEDENCE + 1;

        private Builder() {
        }

        public Builder guardrails(final Guardrails value) {
            this.guardrails = Objects.requireNonNull(value, "guardrails");
            return this;
        }

        public Builder config(final GuardrailsConfig value) {
            this.config = Objects.requireNonNull(value, "config");
            return this;
        }

        public Builder classifier(final LlmClassifier... value) {
            Objects.requireNonNull(value, "classifier");
            for (final LlmClassifier classifier : value) {
                this.classifiers.add(Objects.requireNonNull(classifier, "classifier"));
            }
            return this;
        }

        public Builder scope(final ProtectionScope value) {
            this.scope = Objects.requireNonNull(value, "scope");
            return this;
        }

        public Builder failOnError(final boolean value) {
            this.failOnError = value;
            return this;
        }

        public Builder order(final int value) {
            this.order = value;
            return this;
        }

        public GuardrailAdvisor build() {
            if (guardrails == null) {
                if (classifiers.isEmpty()) {
                    throw new IllegalStateException(
                            "guardrails(...) or classifier(...) is required");
                }
                final var builder = Guardrails.builder()
                        .config(config != null ? config : GuardrailsConfig.DEFAULTS)
                        .failOnlyOnErrors(failOnError);
                for (final LlmClassifier classifier : classifiers) {
                    builder.withClassifier(classifier);
                }
                this.guardrails = builder.build();
            }
            return new GuardrailAdvisor(this);
        }
    }
}
