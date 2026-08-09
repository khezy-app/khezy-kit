package io.github.khezyapp.fsm.core.kyc;

import io.github.khezyapp.fsm.core.api.StateMachine;
import io.github.khezyapp.fsm.core.builder.StateMachineBuilder;
import io.github.khezyapp.fsm.core.builder.StateMachineBuilderException;
import io.github.khezyapp.fsm.core.model.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChoiceRoutingWorkflowTest {

    private record KycContext(String decision) { }

    private StateMachine<String, String, KycContext> buildMachine() throws StateMachineBuilderException {
        return StateMachineBuilder.<String, String, KycContext>builder()
            .initialState("manual_review")
            .state("manual_review")
            .state("revision")
            .finalState("approved")
            .finalState("rejected")

            .transition("to-approved", "manual_review", "approved", "KYC_RESPONSE_RECEIVED")
                .guard(ctx -> "approved".equals(ctx.decision()))
            .and()
            .transition("to-rejected", "manual_review", "rejected", "KYC_RESPONSE_RECEIVED")
                .guard(ctx -> "rejected".equals(ctx.decision()))
            .and()
            .transition("to-revision", "manual_review", "revision", "KYC_RESPONSE_RECEIVED")
                .guard(ctx -> "revision".equals(ctx.decision()))
            .and()
            .build();
    }

    @Test
    @DisplayName("Should route each decision to its matching target")
    void testDecisionRouting() throws StateMachineBuilderException {
        final var approvedMachine = buildMachine();
        approvedMachine.fire(Event.of("KYC_RESPONSE_RECEIVED"), new KycContext("approved"));
        assertEquals("approved", approvedMachine.getCurrentState().id());
        assertEquals("to-approved", approvedMachine.getLastTransition().orElseThrow().id());

        final var revisionMachine = buildMachine();
        revisionMachine.fire(Event.of("KYC_RESPONSE_RECEIVED"), new KycContext("revision"));
        assertEquals("revision", revisionMachine.getCurrentState().id());
        assertEquals("to-revision", revisionMachine.getLastTransition().orElseThrow().id());
    }

    @Test
    @DisplayName("Should route to fallback target when no guard passes")
    void testFallbackRouting() throws StateMachineBuilderException {
        final var machine = StateMachineBuilder.<String, String, KycContext>builder()
            .initialState("route")
            .state("route")
            .state("known")
            .state("unknown")
            .transition("known", "route", "known", "check")
                .guard(ctx -> ctx.decision() != null)
            .and()
            .transition("fallback", "route", "unknown", "check")
            .and()
            .build();

        machine.fire(Event.of("check"), new KycContext(null));

        assertEquals("unknown", machine.getCurrentState().id());
        assertEquals("fallback", machine.getLastTransition().orElseThrow().id());
    }

    @Test
    @DisplayName("Should only execute the winning branch's actions")
    void testOnlyOneBranchExecutes() throws StateMachineBuilderException {
        final var log = new ArrayList<String>();
        final var machine = StateMachineBuilder.<String, String, KycContext>builder()
            .initialState("manual_review")
            .state("manual_review")
            .state("revision")
            .finalState("approved")
            .finalState("rejected")
            .transition("to-approved", "manual_review", "approved", "KYC_RESPONSE_RECEIVED")
                .guard(ctx -> "approved".equals(ctx.decision()))
                .action(ctx -> log.add("approved-branch"))
            .and()
            .transition("to-rejected", "manual_review", "rejected", "KYC_RESPONSE_RECEIVED")
                .guard(ctx -> "rejected".equals(ctx.decision()))
                .action(ctx -> log.add("rejected-branch"))
            .and()
            .build();

        machine.fire(Event.of("KYC_RESPONSE_RECEIVED"), new KycContext("rejected"));

        assertEquals(List.of("rejected-branch"), log);
    }
}
