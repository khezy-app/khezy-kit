package io.github.khezyapp.dhttp.spec;

import lombok.Builder;
import lombok.Singular;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The root declarative description of how to talk to a REST API ({@code R1}).
 *
 * <p>Immutable; all collection fields are defensively copied in the compact constructor.
 *
 * @param baseUrl              the base URL (may be templated/expression)
 * @param defaultHeaders       headers merged under operation headers ({@code R2})
 * @param defaultTimeoutMillis default request timeout
 * @param defaultSkipSsl       default SSL verification bypass ({@code R11})
 * @param operations           the operations available on this spec
 * @param defaultCredential    default credential reference ({@code R10})
 * @param defaultPagination    default pagination ({@code R9})
 * @param security             security policy ({@code R12})
 * @param batching             batching throttle settings, or {@code null} for no pacing (§10.2)
 */
@Builder
public record HttpRequestSpec(String baseUrl,
                              Map<String, String> defaultHeaders,
                              long defaultTimeoutMillis,
                              boolean defaultSkipSsl,
                              @Singular List<Operation> operations,
                              CredentialRef defaultCredential,
                              PaginationSpec defaultPagination,
                              SecurityPolicy security,
                              BatchingSpec batching) {

    public HttpRequestSpec {
        Objects.requireNonNull(baseUrl, "baseUrl");
        defaultHeaders = Map.copyOf(Objects.requireNonNullElseGet(defaultHeaders, Map::of));
        operations = List.copyOf(Objects.requireNonNullElseGet(operations, List::of));
        defaultTimeoutMillis = defaultTimeoutMillis == 0 ? 30000L : defaultTimeoutMillis;
        security = Objects.requireNonNullElseGet(security, SecurityPolicy::defaults);
    }

    /**
     * Convenience constructor without batching pacing.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public HttpRequestSpec(final String baseUrl,
                           final Map<String, String> defaultHeaders,
                           final long defaultTimeoutMillis,
                           final boolean defaultSkipSsl,
                           final List<Operation> operations,
                           final CredentialRef defaultCredential,
                           final PaginationSpec defaultPagination,
                           final SecurityPolicy security) {
        this(baseUrl, defaultHeaders, defaultTimeoutMillis, defaultSkipSsl, operations,
                defaultCredential, defaultPagination, security, null);
    }

    public HttpRequestSpec(final String baseUrl,
                           final List<Operation> operations,
                           final SecurityPolicy security) {
        this(baseUrl, Map.of(), 30000L, false, operations, null, null, security, null);
    }
}
