package io.github.khezyapp.dhttp.spec;

import java.util.List;
import java.util.Objects;

/**
 * Security configuration for the spec ({@code R12}).
 *
 * @param allowedDomains              domains the transport is permitted to call
 * @param allowIpLiteral              whens true, raw IP literals are allowed
 * @param stripCrossOriginCredentials whens true, credentials are not forwarded cross-origin
 * @param sensitiveOutputFields       dotted fields redacted from logged output
 */
public record SecurityPolicy(List<String> allowedDomains,
                             boolean allowIpLiteral,
                             boolean stripCrossOriginCredentials,
                             List<String> sensitiveOutputFields) {

    public SecurityPolicy {
        allowedDomains = List.copyOf(Objects.requireNonNullElseGet(allowedDomains, List::of));
        sensitiveOutputFields = List.copyOf(Objects.requireNonNullElseGet(sensitiveOutputFields, List::of));
    }

    public static SecurityPolicy defaults() {
        return new SecurityPolicy(List.of(), false, true, List.of());
    }
}
