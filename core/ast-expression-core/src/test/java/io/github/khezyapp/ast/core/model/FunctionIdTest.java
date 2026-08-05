package io.github.khezyapp.ast.core.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test suite for {@link FunctionId} sealed interface.
 * <p>
 * Covers creation from string values, core vs named distinction,
 * equality, and null rejection.
 * </p>
 */
@DisplayName("FunctionId")
class FunctionIdTest {

    @Nested
    @DisplayName("creation")
    class CreationTests {

        @Test
        @DisplayName("creates named function id from value string")
        void createsNamedFunctionIdFromValueString() {
            final var id = FunctionId.of("ns:myFunc");

            assertAll(
                () -> assertEquals("ns:myFunc", id.value()),
                () -> assertFalse(id.isCore())
            );
        }

        @Test
        @DisplayName("creates core function id from core value")
        void createsCoreFunctionIdFromCoreValue() {
            final var id = FunctionId.of("add");

            assertAll(
                () -> assertEquals("add", id.value()),
                () -> assertTrue(id.isCore())
            );
        }

        @Test
        @DisplayName("rejects null value")
        void rejectsNullValue() {
            assertThrows(NullPointerException.class,
                () -> FunctionId.of(null));
        }
    }

    @Nested
    @DisplayName("equality")
    class EqualityTests {

        @Test
        @DisplayName("same value are equal")
        void sameValueAreEqual() {
            final var a = FunctionId.of("ns:fn");
            final var b = FunctionId.of("ns:fn");

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("different values are not equal")
        void differentValuesAreNotEqual() {
            final var a = FunctionId.of("ns:fn1");
            final var b = FunctionId.of("ns:fn2");

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("equal to itself")
        void equalToItself() {
            final var id = FunctionId.of("ns:fn");
            assertEquals(id, id);
        }

        @Test
        @DisplayName("not equal to null")
        void notEqualToNull() {
            final var id = FunctionId.of("ns:fn");
            assertNotEquals(null, id);
        }

        @Test
        @DisplayName("not equal to different type")
        void notEqualToDifferentType() {
            final var id = FunctionId.of("ns:fn");
            assertFalse(id.equals("someString"));
        }
    }

    @Nested
    @DisplayName("string representation")
    class ToStringTests {

        @Test
        @DisplayName("named toString shows record format")
        void namedToStringShowsRecordFormat() {
            final var id = FunctionId.of("ns:fn");
            assertEquals("Named[value=ns:fn]", id.toString());
        }

        @Test
        @DisplayName("core toString shows record format")
        void coreToStringShowsRecordFormat() {
            final var id = FunctionId.of("add");
            assertEquals("Core[value=add]", id.toString());
        }
    }
}
