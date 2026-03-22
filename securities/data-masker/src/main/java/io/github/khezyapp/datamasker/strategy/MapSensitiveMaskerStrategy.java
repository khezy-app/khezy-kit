package io.github.khezyapp.datamasker.strategy;

import io.github.khezyapp.datamasker.api.SensitiveMaskerContext;
import io.github.khezyapp.datamasker.api.SensitiveMaskerStrategy;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A sensitive data masking strategy specifically designed for {@link Map} structures.
 * <p>
 * This strategy evaluates map entries against a pre-defined set of sensitive keys (e.g., passwords,
 * credit card numbers, PII). It can either replace the value with a specific mask string or
 * remove the entry entirely if configured to be ignored.
 * </p>
 * <p>Key behaviors include:</p>
 * <ul>
 * <li><b>Support:</b> Handles any object that satisfies {@code isMap(payload)}.</li>
 * <li><b>Default Security Rules:</b> Includes an extensive built-in map of common sensitive
 * keys covering authentication (tokens, secrets), identity (SSN, passport),
 * financial (PCI data), and contact information.</li>
 * <li><b>Customization:</b> Supports a custom list of {@link KeyValueMask} objects to override
 * or extend the default masking behavior.</li>
 * <li><b>Recursive Processing:</b> For keys not present in the masking rules, it delegates
 * complex objects to the {@link SensitiveMaskerContext} for nested masking while
 * preserving primitives and nulls.</li>
 * <li><b>Circular Dependency Protection:</b> Registers the map instance with the
 * {@link SensitiveMaskerContext} to ensure safe traversal of self-referencing maps.</li>
 * </ul>
 */
public class MapSensitiveMaskerStrategy implements SensitiveMaskerStrategy {
    private static final Map<String, KeyValueMask> DEFAULT_KEYS_MAP = Map.ofEntries(
            // --- Security & Authentication (Mostly Ignore) ---
            Map.entry("password", KeyValueMask.builder().key("password").mask("******").build()),
            Map.entry("passphrase", KeyValueMask.builder().key("passphrase").mask("******").build()),
            Map.entry("secret", KeyValueMask.builder().key("secret").mask("******").build()),
            Map.entry("client_secret", KeyValueMask.builder().key("client_secret").ignore(true).build()),
            Map.entry("authorization", KeyValueMask.builder().key("authorization").ignore(true).build()),
            Map.entry("access_token", KeyValueMask.builder().key("access_token").ignore(true).build()),
            Map.entry("accessToken", KeyValueMask.builder().key("accessToken").ignore(true).build()),
            Map.entry("refresh_token", KeyValueMask.builder().key("refresh_token").ignore(true).build()),
            Map.entry("refreshToken", KeyValueMask.builder().key("refreshToken").ignore(true).build()),
            Map.entry("api_key", KeyValueMask.builder().key("api_key").ignore(true).build()),
            Map.entry("apiKey", KeyValueMask.builder().key("apiKey").ignore(true).build()),

            // --- Government & Identity (PII) ---
            Map.entry("ssn", KeyValueMask.builder().key("ssn").mask("***-**-****").build()),
            Map.entry("social_security", KeyValueMask.builder().key("social_security").mask("***-**-****").build()),
            Map.entry("socialSecurity", KeyValueMask.builder().key("socialSecurity").mask("***-**-****").build()),
            Map.entry("tax_id", KeyValueMask.builder().key("tax_id").mask("******").build()),
            Map.entry("taxId", KeyValueMask.builder().key("taxId").mask("******").build()),
            Map.entry("passport", KeyValueMask.builder().key("passport").mask("******").build()),
            Map.entry("driver_license", KeyValueMask.builder().key("driver_license").mask("******").build()),
            Map.entry("driverLicense", KeyValueMask.builder().key("driverLicense").mask("******").build()),

            // --- Financial & Payment (PCI) ---
            Map.entry("credit_card", KeyValueMask.builder().key("credit_card").mask("****-****-****-****").build()),
            Map.entry("creditCard", KeyValueMask.builder().key("creditCard").mask("****-****-****-****").build()),
            Map.entry("cardNumber", KeyValueMask.builder().key("cardNumber").mask("****-****-****-****").build()),
            Map.entry("cvv", KeyValueMask.builder().key("cvv").ignore(true).build()),
            Map.entry("cvc", KeyValueMask.builder().key("cvc").ignore(true).build()),
            Map.entry("pin", KeyValueMask.builder().key("pin").ignore(true).build()),
            Map.entry("bank_account", KeyValueMask.builder().key("bank_account").mask("******").build()),

            // --- Personal Contact Info (Optional Masking) ---
            Map.entry("phone", KeyValueMask.builder().key("phone").mask("*******").build()),
            Map.entry("phoneNumber", KeyValueMask.builder().key("phoneNumber").mask("*******").build()),
            Map.entry("mobile", KeyValueMask.builder().key("mobile").mask("*******").build()),
            Map.entry("email", KeyValueMask.builder().key("email").mask("******@****.com").build())
    );

    private final Map<String, KeyValueMask> keyValueMasks;

    public MapSensitiveMaskerStrategy() {
        this.keyValueMasks = DEFAULT_KEYS_MAP;
    }

    public MapSensitiveMaskerStrategy(final List<KeyValueMask> keyValueMasks) {
        Objects.requireNonNull(keyValueMasks, "keyValueMasks must not be null");
        this.keyValueMasks = keyValueMasks.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(KeyValueMask::getKey, Function.identity()));
    }

    @Override
    public boolean supports(final Object payload) {
        return isMap(payload);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object mask(final Object payload,
                       final SensitiveMaskerContext context) {
        if (payload instanceof Map<?, ?> map) {
            final var proceedMap = new HashMap<String, Object>((Map<? extends String, ?>) map);

            context.registerVisited(payload, proceedMap);

            final var keys = proceedMap.keySet();
            for (final String key : keys) {
                final var valueToMask = proceedMap.get(key);
                if (keyValueMasks.containsKey(key)) {
                    final var mask = keyValueMasks.get(key);
                    if (!mask.ignore) {
                        proceedMap.put(key, mask.mask);
                    }
                } else if (Objects.isNull(valueToMask) ||
                        isPrimitive(valueToMask.getClass())) {
                    proceedMap.put(key, valueToMask);
                } else {
                    final var maks = context.processMask(proceedMap.get(key));
                    proceedMap.put(key, maks);
                }
            }
            return proceedMap;
        }
        return payload;
    }

    @Getter
    @Setter
    @Builder
    public static class KeyValueMask {
        private final String key;
        private final String mask;
        private final boolean ignore;
    }
}
