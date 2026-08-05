package io.github.khezyapp.ast.core.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test suite for {@link Arguments} record.
 * <p>
 * Covers construction with positional and named values, factory method,
 * and equality checks.
 * </p>
 */
@DisplayName("Arguments")
class ArgumentsTest {

    @Nested
    @DisplayName("creation")
    class CreationTests {

        @Test
        @DisplayName("creates with positional and named")
        void createsWithPositionalAndNamed() {
            final var args = new Arguments(List.of(1, 2), Map.of("key", "val"));

            assertAll(
                () -> assertEquals(2, args.positional().size()),
                () -> assertEquals(1, args.positional().get(0)),
                () -> assertEquals("val", args.named().get("key"))
            );
        }

        @Test
        @DisplayName("of factory creates with positional only")
        void ofFactoryCreatesWithPositionalOnly() {
            final var args = Arguments.of(List.of("a", "b"));

            assertAll(
                () -> assertEquals(2, args.positional().size()),
                () -> assertTrue(args.named().isEmpty())
            );
        }

        @Test
        @DisplayName("creates with empty positional")
        void createsWithEmptyPositional() {
            final var args = new Arguments(List.of(), Map.of("k", "v"));
            assertTrue(args.positional().isEmpty());
        }

        @Test
        @DisplayName("creates with empty named")
        void createsWithEmptyNamed() {
            final var args = new Arguments(List.of(1), Map.of());
            assertTrue(args.named().isEmpty());
        }
    }

    @Nested
    @DisplayName("equality")
    class EqualityTests {

        @Test
        @DisplayName("equal arguments are equal")
        void equalArgumentsAreEqual() {
            final var a = new Arguments(List.of(1, 2), Map.of("x", "y"));
            final var b = new Arguments(List.of(1, 2), Map.of("x", "y"));

            assertEquals(a, b);
        }
    }
}
