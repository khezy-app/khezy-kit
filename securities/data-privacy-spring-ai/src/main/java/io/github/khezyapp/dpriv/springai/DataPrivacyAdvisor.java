package io.github.khezyapp.dpriv.springai;

import io.github.khezyapp.dpriv.api.Guardrails;
import io.github.khezyapp.dpriv.api.GuardrailsConfig;
import io.github.khezyapp.dpriv.api.GuardrailsOutcome;
import io.github.khezyapp.dpriv.api.Operation;
import io.github.khezyapp.dpriv.springai.exception.RedactionException;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MITIGATE pattern (design §2, §7): deterministic redaction of USER-message text before the
 * model call (INPUT/BOTH) and of the model response after (OUTPUT/BOTH). Never blocks; never
 * modifies anything but USER text. Fail-closed via {@link RedactionException}. INPUT-only advisors
 * stream raw deltas; OUTPUT/BOTH aggregate the full response so redaction sees the complete text.
 */
public final class DataPrivacyAdvisor implements BaseAdvisor {

    public static final String CONTEXT_KEY = "io.github.khezyapp.dpriv.springai.redactionReport";

    private final Guardrails guardrails;
    private final ProtectionScope scope;
    private final RedactMode mode;
    private final boolean failOnError;
    private final int order;

    private DataPrivacyAdvisor(final Builder builder) {
        this.guardrails = Objects.requireNonNull(builder.guardrails, "guardrails");
        this.scope = Objects.requireNonNull(builder.scope, "scope");
        this.mode = Objects.requireNonNull(builder.mode, "mode");
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
        final var prompt = request.prompt();
        final var candidates = new LinkedHashSet<>(selectCandidates(prompt));
        final var instructions = prompt.getInstructions();
        final var redactedInstructions = new ArrayList<Message>(instructions.size());
        var anyChanged = false;
        final var types = new LinkedHashSet<String>();
        for (final var msg : instructions) {
            if (!(msg instanceof final UserMessage user) || !candidates.contains(user)) {
                redactedInstructions.add(msg);
                continue;
            }
            final var text = user.getText();
            if (Objects.isNull(text)) {
                redactedInstructions.add(msg);
                continue;
            }
            final GuardrailsOutcome outcome;
            try {
                outcome = guardrails.run(text, Operation.SANITIZE);
            } catch (final IllegalStateException ex) {
                if (failOnError) {
                    throw new RedactionException("redaction failed: " + ex.getMessage(), ex);
                }
                redactedInstructions.add(msg);
                continue;
            }
            final var redacted = outcome.text();
            if (redacted.equals(text)) {
                redactedInstructions.add(msg);
                continue;
            }
            anyChanged = true;
            types.addAll(outcome.maskEntities().keySet());
            redactedInstructions.add(user.mutate().text(redacted).build());
        }
        final var report = anyChanged
                ? new RedactionReport(true, Set.copyOf(types))
                : RedactionReport.NONE;
        final var redactedPrompt = prompt.mutate().messages(redactedInstructions).build();
        final var newContext = new LinkedHashMap<>(request.context());
        newContext.put(CONTEXT_KEY, report);
        return request.mutate().prompt(redactedPrompt).context(newContext).build();
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
        if (Objects.isNull(output.getText())) {
            return response;
        }
        final var text = output.getText();
        final GuardrailsOutcome outcome;
        try {
            outcome = guardrails.run(text, Operation.SANITIZE);
        } catch (final IllegalStateException ex) {
            if (failOnError) {
                throw new RedactionException("redaction failed: " + ex.getMessage(), ex);
            }
            return response;
        }
        final var redacted = outcome.text();
        if (redacted.equals(text)) {
            final var newContext = new LinkedHashMap<>(response.context());
            newContext.put(CONTEXT_KEY, RedactionReport.NONE);
            return response.mutate().context(newContext).build();
        }
        final var redactedMessage = output.mutate().content(redacted).build();
        final var redactedResponse = ChatResponse.builder()
                .generations(List.of(new Generation(redactedMessage)))
                .metadata(chatResponse.getMetadata())
                .build();
        final var report = new RedactionReport(true, Set.copyOf(outcome.maskEntities().keySet()));
        final var newContext = new LinkedHashMap<>(response.context());
        newContext.put(CONTEXT_KEY, report);
        return response.mutate().chatResponse(redactedResponse).context(newContext).build();
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
        return "DataPrivacyAdvisor";
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

    private List<UserMessage> selectCandidates(final Prompt prompt) {
        if (mode == RedactMode.ALL) {
            return prompt.getUserMessages();
        }
        final var last = prompt.getLastUserOrToolResponseMessage();
        if (last.getMessageType() == MessageType.USER) {
            return List.of((UserMessage) last);
        }
        final var users = prompt.getUserMessages();
        if (users.isEmpty()) {
            return List.of();
        }
        return List.of(users.get(users.size() - 1));
    }

    public static final class Builder {

        private Guardrails guardrails;
        private ProtectionScope scope = ProtectionScope.INPUT;
        private RedactMode mode = RedactMode.ALL;
        private boolean failOnError = true;
        private int order = Ordered.HIGHEST_PRECEDENCE;

        private Builder() {
        }

        public Builder guardrails(final Guardrails value) {
            this.guardrails = Objects.requireNonNull(value, "guardrails");
            return this;
        }

        public Builder config(final GuardrailsConfig value) {
            this.guardrails = Guardrails.builder()
                    .config(Objects.requireNonNull(value, "config"))
                    .build();
            return this;
        }

        public Builder scope(final ProtectionScope value) {
            this.scope = Objects.requireNonNull(value, "scope");
            return this;
        }

        public Builder mode(final RedactMode value) {
            this.mode = Objects.requireNonNull(value, "mode");
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

        public DataPrivacyAdvisor build() {
            if (guardrails == null) {
                throw new IllegalStateException("guardrails(...) or config(...) is required");
            }
            return new DataPrivacyAdvisor(this);
        }
    }
}
