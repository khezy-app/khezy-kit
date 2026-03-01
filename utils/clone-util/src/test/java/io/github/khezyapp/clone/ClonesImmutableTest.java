package io.github.khezyapp.clone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertSame;

public class ClonesImmutableTest {

    /**
     * Data provider for immutable types.
     * These objects should be returned as-is by the Cloner.
     */
    static Stream<Object> immutableInstances() {
        return Stream.of(
                "Hello World",                         // String
                42,                                    // Integer (Autoboxed)
                Long.valueOf(999L),                    // Long
                true,                                  // Boolean
                3.14159,                               // Double
                LocalDate.of(2024, 1, 1),              // Java 8 Date API
                LocalDateTime.now(),                   // Java 8 DateTime API
                BigDecimal.TEN,                        // Common Immutable Math class
                TestEnum.ACTIVE                        // Enums
        );
    }

    @ParameterizedTest
    @MethodSource("immutableInstances")
    @DisplayName("Should return the same reference for immutable and primitive-wrapper types")
    void testImmutableTypesReturnSameReference(final Object immutable) {
        final var clone = Clones.deepClone(immutable);

        // For immutables, we expect Referential Identity (the exact same memory address)
        assertSame(immutable, clone,
                "The cloner should not create a new instance for immutable type: "
                        + immutable.getClass().getSimpleName());
    }

    enum TestEnum { ACTIVE, INACTIVE }
}
