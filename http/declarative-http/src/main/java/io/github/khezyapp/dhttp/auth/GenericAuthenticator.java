package io.github.khezyapp.dhttp.auth;

import io.github.khezyapp.cert.ClientTlsConfig;
import io.github.khezyapp.dhttp.auth.credential.DecryptedCredential;
import io.github.khezyapp.dhttp.transport.Auth;
import io.github.khezyapp.dhttp.transport.HttpRequest;
import io.github.khezyapp.dhttp.transport.HttpRequestBuilder;

import java.util.Map;
import java.util.Objects;

/**
 * Generic authenticator ({@code R10}) that injects auth purely from the decrypted credential's
 * fields — no hardcoded type-to-behavior mapping (§6.3):
 *
 * <ul>
 * <li>{@code headers} map → merged into the request (covers {@code http-header});</li>
 * <li>{@code headerName} + {@code value} → set as a request header (covers {@code api-key});</li>
 * <li>{@code username} + {@code password} → set as HTTP Basic auth (covers {@code basic-auth});</li>
 * <li>{@code certChainPem} + {@code privateKeyPem} → present as an mTLS client identity
 * (covers {@code client-certificate}).</li>
 * </ul>
 *
 * <p>Secrets always come from the credential, never the spec ({@code R12}/§7.5); none are logged.</p>
 */
public final class GenericAuthenticator implements Authenticator {

    @Override
    public HttpRequest apply(final DecryptedCredential<?> credential,
                             final HttpRequest request,
                             final AuthResult out) {
        final var fields = credential.fields();
        final var builder = request.toBuilder();
        var applied = false;
        applied |= mergeHeaders(fields, builder);
        applied |= injectHeaderKey(fields, builder);
        applied |= injectBasic(fields, builder);
        applied |= injectClientCertificate(fields, builder);
        out.markApplied(applied);
        out.setCredentialId(credential.id());
        return builder.build();
    }

    private static boolean mergeHeaders(final Map<String, Object> fields,
                                        final HttpRequestBuilder builder) {
        if (!(fields.get("headers") instanceof Map<?, ?> headers)) {
            return false;
        }
        for (final var entry : headers.entrySet()) {
            builder.header(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return !headers.isEmpty();
    }

    private static boolean injectHeaderKey(final Map<String, Object> fields,
                                           final HttpRequestBuilder builder) {
        final var headerName = fields.get("headerName");
        final var value = fields.get("value");
        if (Objects.isNull(headerName) || Objects.isNull(value)) {
            return false;
        }
        builder.header(String.valueOf(headerName), String.valueOf(value));
        return true;
    }

    private static boolean injectBasic(final Map<String, Object> fields,
                                       final HttpRequestBuilder builder) {
        final var username = fields.get("username");
        final var password = fields.get("password");
        if (Objects.isNull(username) || Objects.isNull(password)) {
            return false;
        }
        builder.auth(new Auth.BasicAuth(String.valueOf(username), String.valueOf(password)));
        return true;
    }

    private static boolean injectClientCertificate(final Map<String, Object> fields,
                                                   final HttpRequestBuilder builder) {
        final var chain = fields.get("certChainPem");
        final var key = fields.get("privateKeyPem");
        if (Objects.isNull(chain) || Objects.isNull(key)) {
            return false;
        }
        final var password = fields.get("privateKeyPassword");
        builder.tlsConfig(new ClientTlsConfig(String.valueOf(chain), String.valueOf(key),
                Objects.isNull(password) ? null : String.valueOf(password)));
        return true;
    }
}
