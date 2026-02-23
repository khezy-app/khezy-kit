package io.github.khezyapp.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StringUtil Test")
class StringUtilTest {

    // -------------------------------------------------
    // isBlank / isNotBlank
    // -------------------------------------------------

    @ParameterizedTest(name = "[{index}] text=\"{0}\"")
    @NullSource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("isBlank - should return true for blank inputs")
    void isBlankCase(final String input) {
        if (Objects.isNull(input)) {
            assertTrue(StringUtil.isBlank(null));
        } else {
            assertTrue(StringUtil.isBlank(input));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "abc", "  a  "})
    @DisplayName("isBlank - should return false for non blank inputs")
    void isBlankNonBlankCase(final String input) {
        assertFalse(StringUtil.isBlank(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "abc"})
    @DisplayName("isNotBlank - should return true when not blank")
    void isNotBlankCase(final String input) {
        assertTrue(StringUtil.isNotBlank(input));
    }

    // -------------------------------------------------
    // isEmpty / isNotEmpty
    // -------------------------------------------------

    @ParameterizedTest
    @NullSource
    @DisplayName("isEmpty - null should return false")
    void isEmptyNullCase(final String input) {
        assertFalse(StringUtil.isEmpty(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {""})
    @DisplayName("isEmpty - empty string should return true")
    void isEmptyCase(final String input) {
        assertTrue(StringUtil.isEmpty(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "a"})
    @DisplayName("isEmpty - non empty should return false")
    void isEmptyNonEmptyCase(final String input) {
        assertFalse(StringUtil.isEmpty(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "abc"})
    @DisplayName("isNotEmpty - should return true when not empty")
    void isNotEmptyCase(final String input) {
        assertTrue(StringUtil.isNotEmpty(input));
    }

    // -------------------------------------------------
    // equals / equalsIgnoreCase
    // -------------------------------------------------

    static Stream<Arguments> equalsProvider() {
        return Stream.of(
                Arguments.of(null, null, true),
                Arguments.of(null, "a", false),
                Arguments.of("a", "a", true),
                Arguments.of("a", "b", false)
        );
    }

    @ParameterizedTest
    @MethodSource("equalsProvider")
    @DisplayName("equals - should compare safely")
    void equalsCase(final String a,
                    final String b,
                    final boolean expected) {
        assertEquals(expected, StringUtil.equals(a, b));
    }

    static Stream<Arguments> equalsIgnoreCaseProvider() {
        return Stream.of(
                Arguments.of("ABC", "abc", true),
                Arguments.of("abc", "ABC", true),
                Arguments.of("abc", "def", false),
                Arguments.of(null, null, true),
                Arguments.of(null, "abc", false)
        );
    }

    @ParameterizedTest
    @MethodSource("equalsIgnoreCaseProvider")
    @DisplayName("equalsIgnoreCase - should compare ignoring case")
    void equalsIgnoreCaseCase(final String a,
                              final String b,
                              final boolean expected) {
        assertEquals(expected, StringUtil.equalsIgnoreCase(a, b));
    }

    // -------------------------------------------------
    // reverse
    // -------------------------------------------------

    static Stream<Arguments> reverseProvider() {
        return Stream.of(
                Arguments.of(null, ""),
                Arguments.of("", ""),
                Arguments.of("a", "a"),
                Arguments.of("abc", "cba")
        );
    }

    @ParameterizedTest
    @MethodSource("reverseProvider")
    @DisplayName("reverse - should reverse correctly")
    void reverseCase(final String input,
                     final String expected) {
        assertEquals(expected, StringUtil.reverse(input));
    }

    // -------------------------------------------------
    // strip
    // -------------------------------------------------

    static Stream<Arguments> stripProvider() {
        return Stream.of(
                Arguments.of("  abc  ", true, "abc"),
                Arguments.of(null, true, ""),
                Arguments.of(null, false, null)
        );
    }

    @ParameterizedTest
    @MethodSource("stripProvider")
    @DisplayName("strip - should trim and handle null properly")
    void stripCase(final String input,
                   final boolean returnEmptyIfNull,
                   final String expected) {
        assertEquals(expected, StringUtil.strip(input, returnEmptyIfNull));
    }

    // -------------------------------------------------
    // stripLeft
    // -------------------------------------------------

    static Stream<Arguments> stripLeftProvider() {
        return Stream.of(
                Arguments.of("foobar", "foo", false, "bar"),
                Arguments.of("foofoobar", "foo", true, "bar"),
                Arguments.of("foobar", "bar", true, "foobar"),
                Arguments.of(null, "a", true, null),
                Arguments.of("abc", null, true, "abc")
        );
    }

    @ParameterizedTest
    @MethodSource("stripLeftProvider")
    @DisplayName("stripLeft - should remove prefix properly")
    void stripLeftCase(final String text,
                       final String stripText,
                       final boolean repeat,
                       final String expected) {
        assertEquals(expected, StringUtil.stripLeft(text, stripText, repeat));
    }

    // -------------------------------------------------
    // stripRight
    // -------------------------------------------------

    static Stream<Arguments> stripRightProvider() {
        return Stream.of(
                Arguments.of("foobar", "bar", false, "foo"),
                Arguments.of("foobarbar", "bar", true, "foo"),
                Arguments.of("foobar", "foo", true, "foobar"),
                Arguments.of(null, "a", true, null),
                Arguments.of("abc", null, true, "abc")
        );
    }

    @ParameterizedTest
    @MethodSource("stripRightProvider")
    @DisplayName("stripRight - should remove suffix properly")
    void stripRightCase(final String text,
                        final String stripText,
                        final boolean repeat,
                        final String expected) {
        assertEquals(expected, StringUtil.stripRight(text, stripText, repeat));
    }

    // ------------------------------------------
    // split(text, pattern)
    // ------------------------------------------

    static Stream<Arguments> splitProvider() {
        return Stream.of(
                Arguments.of("a,b,c", ",", List.of("a", "b", "c")),
                Arguments.of("a,,b", ",", List.of("a", "", "b")),
                Arguments.of("a,b,", ",", List.of("a", "b")),
                Arguments.of("", ",", List.of("")),
                Arguments.of("abc", ",", List.of("abc"))
        );
    }

    @ParameterizedTest
    @MethodSource("splitProvider")
    @DisplayName("split - default behavior")
    void splitCase(final String text,
                   final String pattern,
                   final List<String> expected) {
        assertEquals(expected, StringUtil.split(text, pattern));
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("split - null text returns empty list")
    void splitNullCase(final String text) {
        assertTrue(StringUtil.split(text, ",").isEmpty());
    }

    // ------------------------------------------
    // splitByPreservedEmpty(text, pattern)
    // ------------------------------------------

    static Stream<Arguments> splitByPreservedEmptyProvider() {
        return Stream.of(
                Arguments.of("a,b,c", ",", List.of("a", "b", "c")),
                Arguments.of("a,,b", ",", List.of("a", "", "b")),
                Arguments.of("a,b,", ",", List.of("a", "b", "")),
                Arguments.of(",", ",", List.of("", "")),
                Arguments.of("", ",", List.of(""))
        );
    }

    @ParameterizedTest
    @MethodSource("splitByPreservedEmptyProvider")
    @DisplayName("splitByPreservedEmpty - preserve empty tokens")
    void splitByPreservedEmptyCase(final String text,
                                   final String pattern,
                                   final List<String> expected) {
        assertEquals(expected, StringUtil.splitByPreservedEmpty(text, pattern));
    }

    // ------------------------------------------
    // split(text, pattern, limit, preservedEmpty)
    // ------------------------------------------

    static Stream<Arguments> splitWithLimitProvider() {
        return Stream.of(
                Arguments.of("a,b,c,d", ",", 2, true, List.of("a", "b,c,d")),
                Arguments.of("a,b,c,d", ",", 3, true, List.of("a", "b", "c,d")),
                Arguments.of("a,b,c", ",", 2, false, List.of("a", "b", "c")),
                Arguments.of("a,,b,", ",", 3, true, List.of("a", "", "b,")),
                Arguments.of("a,,b,", ",", 0, true, List.of("a", "", "b", "")),
                Arguments.of("a,b,", ",", 0, false, List.of("a", "b"))
        );
    }

    @ParameterizedTest
    @MethodSource("splitWithLimitProvider")
    @DisplayName("split - with limit and preservedEmpty variations")
    void splitWithLimitCase(final String text,
                            final String pattern,
                            final int limit,
                            final boolean preservedEmpty,
                            final List<String> expected) {
        assertEquals(expected, StringUtil.split(text, pattern, limit, preservedEmpty));
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("split - with limit null text returns empty list")
    void splitWithLimitNullCase(final String text) {
        assertTrue(StringUtil.split(text, ",", 2, true).isEmpty());
    }

    // -------------------------------------------------
    // rsplit(text, pattern, limit, preservedEmpty)
    // -------------------------------------------------

    static Stream<Arguments> rsplitProvider() {
        return Stream.of(
                Arguments.of("a,b,c,d", ",", 2, true, List.of("a,b,c", "d")),
                Arguments.of("a,b,c,d", ",", 3, true, List.of("a,b", "c", "d")),
                Arguments.of("a,b,c,d", ",", 10, true, List.of("a", "b", "c", "d")),
                Arguments.of("a,,b,", ",", 3, true, List.of("a,", "b", "")),
                Arguments.of("a,,b,", ",", 0, true, List.of("a", "", "b", "")),
                Arguments.of("a,b,", ",", 0, false, List.of("a", "b")),
                Arguments.of("abc", ",", 2, true, List.of("abc")),
                Arguments.of("", ",", 2, true, List.of(""))
        );
    }

    @ParameterizedTest
    @MethodSource("rsplitProvider")
    @DisplayName("rsplit - with limit and preservedEmpty variations")
    void rsplitCase(final String text,
                    final String pattern,
                    final int limit,
                    final boolean preservedEmpty,
                    final List<String> expected) {
        assertEquals(expected, StringUtil.rsplit(text, pattern, limit, preservedEmpty));
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("rsplit - null text returns empty list")
    void rsplitNullCase(final String text) {
        assertTrue(StringUtil.rsplit(text, ",", 2, true).isEmpty());
    }

    @Test
    @DisplayName("rsplit - should call main rsplit with default limit=0 and preservedEmpty=false")
    void rsplitDefaultCase() {
        List<String> result = StringUtil.rsplit("a,b,c", ",", 2);
        List<String> expected = List.of("a,b", "c");
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("rsplitByPreservedEmpty - should call main rsplit with default limit=0 and preservedEmpty=true")
    void rsplitByPreservedEmptyDefaultCase() {
        List<String> result = StringUtil.rsplitByPreservedEmpty("a,b,c,", ",", 2);
        List<String> expected = List.of("a,b,c", "");
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("rsplit - null input returns empty list")
    void rsplitNullInputCase() {
        List<String> result = StringUtil.rsplit(null, ",", 0);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("rsplitByPreservedEmpty - null input returns empty list")
    void rsplitByPreservedEmptyNullInputCase() {
        List<String> result = StringUtil.rsplitByPreservedEmpty(null, ",", 0);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("testContains: Should return true only when string contains non-null content")
    void testContains() {
        assertAll(
                () -> assertTrue(StringUtil.contains("Hello World", "Hello")),
                () -> assertTrue(StringUtil.contains("jOOQ Specification", "Spec")),
                () -> assertFalse(StringUtil.contains("Hello", "World")),
                () -> assertFalse(StringUtil.contains("", "Hello")),
                () -> assertFalse(StringUtil.contains("   ", "Hello")),
                () -> assertFalse(StringUtil.contains(null, "Hello")),
                () -> assertFalse(StringUtil.contains("Hello", null))
        );
    }

    @Test
    @DisplayName("testToLowerCase: Should convert to lower case or return original if blank")
    void testToLowerCase() {
        assertAll(
                () -> assertEquals("hello", StringUtil.toLowerCase("HELLO")),
                () -> assertEquals("jooq", StringUtil.toLowerCase("jOOQ")),
                () -> assertEquals("", StringUtil.toLowerCase("")),
                () -> assertEquals("   ", StringUtil.toLowerCase("   ")),
                () -> assertNull(StringUtil.toLowerCase(null))
        );
    }

    @Test
    @DisplayName("testToUpperCase: Should convert to upper case or return original if blank")
    void testToUpperCase() {
        assertAll(
                () -> assertEquals("HELLO", StringUtil.toUpperCase("hello")),
                () -> assertEquals("JOOQ", StringUtil.toUpperCase("jOOQ")),
                () -> assertEquals("", StringUtil.toUpperCase("")),
                () -> assertEquals("   ", StringUtil.toUpperCase("   ")),
                () -> assertNull(StringUtil.toUpperCase(null))
        );
    }

    @Test
    @DisplayName("testCapitalize: Should capitalize first char and lowercase others")
    void testCapitalize() {
        assertAll(
                () -> assertEquals("Hello", StringUtil.capitalize("hELLO")),
                () -> assertEquals("Jooq", StringUtil.capitalize("jOOQ")),
                () -> assertEquals("A", StringUtil.capitalize("a")),
                () -> assertEquals("A", StringUtil.capitalize("A")),
                () -> assertEquals("", StringUtil.capitalize("")),
                () -> assertEquals("   ", StringUtil.capitalize("   ")),
                () -> assertNull(StringUtil.capitalize(null))
        );
    }

    @Test
    @DisplayName("testWordCapitalize: Should capitalize each word in a standard string")
    void testWordCapitalizeStandard() {
        final var input = "hello world from jooq";
        final var expected = "Hello World From Jooq";
        assertEquals(expected, StringUtil.wordCapitalize(input));
    }

    @Test
    @DisplayName("testWordCapitalize: Should handle mixed case input correctly")
    void testWordCapitalizeMixedCase() {
        final var input = "jOOQ sPeCiFiCaTiOn";
        final var expected = "Jooq Specification";
        assertEquals(expected, StringUtil.wordCapitalize(input));
    }

    @Test
    @DisplayName("testWordCapitalize: Should handle Unicode spaces (NFKC normalization)")
    void testWordCapitalizeUnicodeSpaces() {
        // \u00A0 is a non-breaking space, which NFKC normalizes to \u0020 (common space)
        final var input = "Unicode\u00A0Space";
        final var expected = "Unicode Space";
        assertEquals(expected, StringUtil.wordCapitalize(input));
    }

    @Test
    @DisplayName("testWordCapitalize: Should return original value for blank or null strings")
    void testWordCapitalizeBlank() {
        assertAll(
                () -> assertNull(StringUtil.wordCapitalize(null)),
                () -> assertEquals("", StringUtil.wordCapitalize("")),
                () -> assertEquals("   ", StringUtil.wordCapitalize("   "))
        );
    }

    @Test
    @DisplayName("testWordCapitalize: Should handle strings with multiple spaces")
    void testWordCapitalizeMultipleSpaces() {
        final var input = "multiple   spaces";
        // split(" ") on "   " creates empty strings in the array
        // capitalize("") returns "", so join puts them back
        final var expected = "Multiple   Spaces";
        assertEquals(expected, StringUtil.wordCapitalize(input));
    }

    @Test
    @DisplayName("testWordCapitalize: Should handle single character words")
    void testWordCapitalizeSingleChars() {
        final var input = "a b c";
        final var expected = "A B C";
        assertEquals(expected, StringUtil.wordCapitalize(input));
    }
}
