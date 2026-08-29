package io.github.khezyapp.dpriv.springai;

import io.github.khezyapp.dpriv.springai.exception.DataPrivacyException;
import io.github.khezyapp.dpriv.springai.exception.GuardrailEvaluationException;
import io.github.khezyapp.dpriv.springai.exception.PolicyViolationException;
import io.github.khezyapp.dpriv.springai.exception.RedactionException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class AdvisorTypesTest {

    @Test
    void protectionScopeHasExactlyInputOutputBoth() {
        assertThat(ProtectionScope.values())
                .containsExactly(ProtectionScope.INPUT, ProtectionScope.OUTPUT, ProtectionScope.BOTH);
    }

    @Test
    void redactModeHasExactlyAllLastOnly() {
        assertThat(RedactMode.values()).containsExactly(RedactMode.ALL, RedactMode.LAST_ONLY);
    }

    @Test
    void redactionReportNoneIsCleanEmptyReport() {
        final var none = RedactionReport.NONE;
        assertThat(none.redacted()).isFalse();
        assertThat(none.entityTypes()).isEmpty();
        assertThatThrownBy(() -> none.entityTypes().add("X")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void policyViolationExceptionCarriesEntityTypeAndScope() {
        final var ex = new PolicyViolationException("jailbreak", ProtectionScope.INPUT);
        assertThat(ex.entityType()).isEqualTo("jailbreak");
        assertThat(ex.scope()).isEqualTo(ProtectionScope.INPUT);
        assertThat(ex.getMessage()).contains("jailbreak").contains("INPUT");
        assertThat(ex).isInstanceOf(DataPrivacyException.class);
    }

    @Test
    void redactionExceptionIsDataPrivacyException() {
        final var cause = new IllegalStateException("boom");
        final var ex = new RedactionException("redaction failed: boom", cause);
        assertThat(ex).isInstanceOf(DataPrivacyException.class);
        assertThat(ex.getMessage()).isEqualTo("redaction failed: boom");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void guardrailEvaluationExceptionIsDataPrivacyException() {
        final var cause = new RuntimeException("judge down");
        final var ex = new GuardrailEvaluationException("judge failed", cause);
        assertThat(ex).isInstanceOf(DataPrivacyException.class);
        assertThat(ex.getMessage()).isEqualTo("judge failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
