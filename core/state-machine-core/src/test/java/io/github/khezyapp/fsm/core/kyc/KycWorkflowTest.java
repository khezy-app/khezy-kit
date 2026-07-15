package io.github.khezyapp.fsm.core.kyc;

import io.github.khezyapp.fsm.core.api.StateMachine;
import io.github.khezyapp.fsm.core.builder.StateMachineBuilder;
import io.github.khezyapp.fsm.core.builder.StateMachineBuilderException;
import io.github.khezyapp.fsm.core.model.Event;
import io.github.khezyapp.fsm.core.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end KYC workflow tests.
 *
 * Flow:
 *   DRAFT ──submit──► INFO_COLLECTED ──validate──► VALIDATING ──pass──► APPROVED
 *                          │                            │
 *                          │                            └──fail──► REJECTED
 *                          │
 *                          └──revision──► DRAFT
 */
class KycWorkflowTest {

    private StateMachine<String, String, KycContext> machine;

    record KycContext(String name, String placeOfBirth, String dateOfBirth, String decision) { }

    @BeforeEach
    void setUp() throws StateMachineBuilderException {
        machine = StateMachineBuilder.<String, String, KycContext>builder()
            .initialState("DRAFT")

            .state("DRAFT",
                List.of(),                                      // onEntry: none
                List.of(KycWorkflowTest::validateFields)        // onExit: validate
            )
            .state("INFO_COLLECTED",
                List.<io.github.khezyapp.fsm.core.api.Action<KycContext>>of(ctx -> { }),              // onEntry
                List.of()                                       // onExit: none
            )
            .state("VALIDATING")
            .finalState("APPROVED")
            .finalState("REJECTED")

            // DRAFT → INFO_COLLECTED
            .transition("submit", "DRAFT", "INFO_COLLECTED", "submit")
                .guard(ctx -> Objects.nonNull(ctx.name()) && !ctx.name().isBlank())
                .action(ctx -> { /* fields submitted */ })
            .and()

            // INFO_COLLECTED → DRAFT (revision)
            .transition("revision", "INFO_COLLECTED", "DRAFT", "revision")
            .and()

            // INFO_COLLECTED → VALIDATING
            .transition("validate", "INFO_COLLECTED", "VALIDATING", "validate")
            .and()

            // VALIDATING → APPROVED
            .transition("pass", "VALIDATING", "APPROVED", "pass")
                .action(ctx -> { /* approved */ })
            .and()

            // VALIDATING → REJECTED
            .transition("fail", "VALIDATING", "REJECTED", "fail")
                .action(ctx -> { /* rejected */ })
            .and()

            .build();
    }

    private static void validateFields(final KycContext ctx) {
        if (Objects.isNull(ctx.name()) || ctx.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
    }

    @Test
    @DisplayName("Should complete KYC happy path: DRAFT → INFO_COLLECTED → VALIDATING → APPROVED")
    void testKycHappyPath() {
        final var ctx = new KycContext("Sok", "Battambang", "1995-05-20", null);

        machine.fire(Event.of("submit"), ctx);
        assertEquals("INFO_COLLECTED", machine.getCurrentState().id());

        machine.fire(Event.of("validate"), ctx);
        assertEquals("VALIDATING", machine.getCurrentState().id());

        machine.fire(Event.of("pass"), ctx);
        assertEquals("APPROVED", machine.getCurrentState().id());

        assertTrue(machine.isFinal());
    }

    @Test
    @DisplayName("Should complete KYC fail path: DRAFT → INFO_COLLECTED → VALIDATING → REJECTED")
    void testKycFailPath() {
        final var ctx = new KycContext("Sao", "Phnom Penh", "1990-01-15", null);

        machine.fire(Event.of("submit"), ctx);
        assertEquals("INFO_COLLECTED", machine.getCurrentState().id());

        machine.fire(Event.of("validate"), ctx);
        assertEquals("VALIDATING", machine.getCurrentState().id());

        machine.fire(Event.of("fail"), ctx);
        assertEquals("REJECTED", machine.getCurrentState().id());

        assertTrue(machine.isFinal());
    }

    @Test
    @DisplayName("Should support revision: DRAFT → INFO_COLLECTED → DRAFT")
    void testKycRevision() {
        final var ctx = new KycContext("Sal", "Kampongcham", "2000-12-25", null);

        machine.fire(Event.of("submit"), ctx);
        assertEquals("INFO_COLLECTED", machine.getCurrentState().id());

        machine.fire(Event.of("revision"), ctx);
        assertEquals("DRAFT", machine.getCurrentState().id());
        assertFalse(machine.isFinal());
    }

    @Test
    @DisplayName("Should block submit transition when name is blank")
    void testGuardBlocksBlankName() {
        final var ctx = new KycContext("", "Battambang", "1995-05-20", null);

        machine.fire(Event.of("submit"), ctx);

        assertEquals("DRAFT", machine.getCurrentState().id());
    }

    @Test
    @DisplayName("Should block submit transition when name is null")
    void testGuardBlocksNullName() {
        final var ctx = new KycContext(null, "Battambang", "1995-05-20", null);

        machine.fire(Event.of("submit"), ctx);

        assertEquals("DRAFT", machine.getCurrentState().id());
    }

    @Test
    @DisplayName("Should execute entry actions in correct order")
    void testEntryActionExecutionOrder() {
        final var actionLog = new ArrayList<String>();
        final var m = buildWithLoggingMachine(actionLog);

        m.fire(Event.of("submit"), new KycContext("Alice", "Battambang", "1995-05-20", null));

        assertTrue(actionLog.contains("entry:INFO_COLLECTED"));
    }

    @Test
    @DisplayName("Should execute exit actions before transition")
    void testExitActionExecution() {
        final var actionLog = new ArrayList<String>();
        final var m = buildWithLoggingMachine(actionLog);

        m.fire(Event.of("submit"), new KycContext("Alice", "Battambang", "1995-05-20", null));

        assertTrue(actionLog.contains("exit:DRAFT"));
    }

    @Test
    @DisplayName("Should execute actions in order: exit → transition → entry")
    void testActionOrder() {
        final var actionLog = new ArrayList<String>();
        final var m = buildWithLoggingMachine(actionLog);

        m.fire(Event.of("submit"), new KycContext("Alice", "Battambang", "1995-05-20", null));

        final var expectedOrder = List.of("exit:DRAFT", "transition:submit", "entry:INFO_COLLECTED");
        assertEquals(expectedOrder, actionLog);
    }

    @Test
    @DisplayName("Should allow message header mutation in interceptor during KYC flow")
    void testMessageHeadersInInterceptor() {
        final var message = Message.of("kyc-submission");
        final var interceptor =
            new io.github.khezyapp.fsm.core.api.StateMachineInterceptor<String, String, KycContext>() {
            @Override
            public boolean preTransition(
                final String source, final String target,
                final Event<String, ?> event, final KycContext ctx
            ) {
                event.message().withHeader("sourceState", source)
                    .withHeader("targetState", target);
                return true;
            }
        };
        machine.addInterceptor(interceptor);

        machine.fire(Event.of("submit", message), new KycContext("Alice", "Battambang", "1995-05-20", null));

        assertEquals("DRAFT", message.headers().get("sourceState"));
        assertEquals("INFO_COLLECTED", message.headers().get("targetState"));
    }

    @Test
    @DisplayName("Should handle full lifecycle with listener notifications")
    void testFullLifecycleWithListeners() {
        final var transitions = new ArrayList<String>();
        machine.addListener(new io.github.khezyapp.fsm.core.api.StateMachineListener<String, String>() {
            @Override
            public void onTransitionComplete(final String source, final String target, final Event<String, ?> event) {
                transitions.add(source + "->" + target);
            }
        });

        final var ctx = new KycContext("Alice", "Battambang", "1995-05-20", null);

        machine.fire(Event.of("submit"), ctx);
        machine.fire(Event.of("validate"), ctx);
        machine.fire(Event.of("pass"), ctx);

        assertEquals(
            List.of("DRAFT->INFO_COLLECTED", "INFO_COLLECTED->VALIDATING", "VALIDATING->APPROVED"),
            transitions
        );
        assertTrue(machine.isFinal());
    }

    private StateMachine<String, String, KycContext> buildWithLoggingMachine(
        final List<String> actionLog
    ) throws StateMachineBuilderException {
        return StateMachineBuilder.<String, String, KycContext>builder()
            .initialState("DRAFT")
            .state("DRAFT",
                List.of(),
                List.of(ctx -> actionLog.add("exit:DRAFT"))
            )
            .state("INFO_COLLECTED",
                List.of(ctx -> actionLog.add("entry:INFO_COLLECTED")),
                List.of()
            )
            .transition("submit", "DRAFT", "INFO_COLLECTED", "submit")
                .guard(ctx -> ctx.name() != null && !ctx.name().isBlank())
                .action(ctx -> actionLog.add("transition:submit"))
            .and()
            .build();
    }
}
