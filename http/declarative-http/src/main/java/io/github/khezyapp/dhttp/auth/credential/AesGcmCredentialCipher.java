package io.github.khezyapp.dhttp.auth.credential;

import io.github.khezyapp.dhttp.json.JsonMapper;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import tools.jackson.core.type.TypeReference;

/**
 * Default {@link CredentialCipher}: JDK AES-256-GCM (§6.2).
 *
 * <p>The IV is derived deterministically (HMAC-SHA256 of the plaintext, first 12 bytes) so the same
 * credential properties always encrypt to the same ciphertext — this keeps stored payloads
 * reproducible and makes the Map-form and type-safe {@code create} converge on byte-identical maps.
 * A wrong key fails authentication on decrypt and surfaces as a {@link RuntimeException}.</p>
 */
public final class AesGcmCredentialCipher implements CredentialCipher {

    private static final String ALGORITHM = "AES-GCM-256";
    private static final String CIPHER_TRANSFORM = "AES/GCM/NoPadding";
    private static final String MAC_ALGORITHM = "HmacSHA256";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<Map<String, Object>>() {
            };

    private final SecretKey key;
    private final JsonMapper jsonMapper;

    public AesGcmCredentialCipher(final KeyProvider keyProvider) {
        this(keyProvider, JacksonJsonMapper.INSTANCE);
    }

    public AesGcmCredentialCipher(final KeyProvider keyProvider,
                                  final JsonMapper jsonMapper) {
        this(Objects.requireNonNull(keyProvider, "keyProvider").key(),
                Objects.requireNonNull(jsonMapper, "jsonMapper"));
    }

    public AesGcmCredentialCipher(final SecretKey key) {
        this(key, JacksonJsonMapper.INSTANCE);
    }

    public AesGcmCredentialCipher(final SecretKey key,
                                  final JsonMapper jsonMapper) {
        this.key = Objects.requireNonNull(key, "key");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    @Override
    public EncryptedPayload encrypt(final Map<String, Object> plaintext) {
        Objects.requireNonNull(plaintext, "plaintext");
        try {
            final var payload = jsonMapper.write(plaintext).getBytes(StandardCharsets.UTF_8);
            final var iv = deriveIv(payload);
            final var cipher = Cipher.getInstance(CIPHER_TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            final var ciphertext = cipher.doFinal(payload);
            return new EncryptedPayload(ALGORITHM, base64(iv), base64(ciphertext));
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    public Map<String, Object> decrypt(final EncryptedPayload payload) {
        Objects.requireNonNull(payload, "payload");
        try {
            final var cipher = Cipher.getInstance(CIPHER_TRANSFORM);
            cipher.init(
                    Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(TAG_BITS, unbase64(payload.iv()))
            );
            final var plaintext = cipher.doFinal(unbase64(payload.ciphertext()));
            return jsonMapper.read(new String(plaintext, StandardCharsets.UTF_8), MAP_TYPE);
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    /**
     * Derives the IV deterministically from the keyed plaintext so identical properties encrypt to
     * identical ciphertext.
     */
    private byte[] deriveIv(final byte[] plaintext) throws GeneralSecurityException {
        final var mac = Mac.getInstance(MAC_ALGORITHM);
        mac.init(key);
        return Arrays.copyOf(mac.doFinal(plaintext), IV_LENGTH);
    }

    private static String base64(final byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] unbase64(final String value) {
        return Base64.getDecoder().decode(value);
    }
}
