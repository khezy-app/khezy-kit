package io.github.khezyapp.ast.core.builtin.string;

import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.model.Arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for built-in string evaluators.
 * <p>
 * Covers StringContainsEvaluator, StringStartsWithEvaluator,
 * StringEndsWithEvaluator, StringMatchEvaluator, StringFuzzyMatchEvaluator,
 * and StringSimilarityEvaluator.
 * </p>
 */
@DisplayName("String evaluators")
class StringEvaluatorsTest {

    @Nested
    @DisplayName("StringContainsEvaluator")
    class StringContainsTests {

        private final StringContainsEvaluator evaluator = new StringContainsEvaluator();

        @Test
        @DisplayName("returns true when substring found")
        void containsFound() {
            final var args = new Arguments(
                    List.of("hello world"), Map.of("substring", "world"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(true, result.value());
        }

        @Test
        @DisplayName("returns false when substring not found")
        void containsNotFound() {
            final var args = new Arguments(
                    List.of("hello world"), Map.of("substring", "xyz"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(false, result.value());
        }

        @Test
        @DisplayName("returns MISSING_NAMED_ARG when substring is missing")
        void missingSubstring() {
            final var args = new Arguments(List.of("hello"), Map.of());
            final var result = evaluator.evaluate(null, args);

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.MISSING_NAMED_ARG.code(),
                    result.errors().get(0).errorCode().code());
        }
    }

    @Nested
    @DisplayName("StringStartsWithEvaluator")
    class StringStartsWithTests {

        private final StringStartsWithEvaluator evaluator = new StringStartsWithEvaluator();

        @Test
        @DisplayName("returns true when prefix matches")
        void startsWithPrefix() {
            final var args = new Arguments(
                    List.of("hello"), Map.of("prefix", "hel"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(true, result.value());
        }

        @Test
        @DisplayName("returns false when prefix does not match")
        void startsWithNoMatch() {
            final var args = new Arguments(
                    List.of("hello"), Map.of("prefix", "xyz"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(false, result.value());
        }

        @Test
        @DisplayName("returns true with case-insensitive prefix")
        void caseInsensitivePrefix() {
            final var args = new Arguments(
                    List.of("Hello"),
                    Map.of("prefix", "hel", "caseSensitive", false));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(true, result.value());
        }

        @Test
        @DisplayName("returns MISSING_NAMED_ARG when prefix is missing")
        void missingPrefix() {
            final var args = new Arguments(List.of("hello"), Map.of());
            final var result = evaluator.evaluate(null, args);

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.MISSING_NAMED_ARG.code(),
                    result.errors().get(0).errorCode().code());
        }

        @Test
        @DisplayName("returns false when input is null")
        void nullInput() {
            final var positional = new ArrayList<>();
            positional.add(null);
            final var args = new Arguments(positional, Map.of("prefix", "hel"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(false, result.value());
        }
    }

    @Nested
    @DisplayName("StringEndsWithEvaluator")
    class StringEndsWithTests {

        private final StringEndsWithEvaluator evaluator = new StringEndsWithEvaluator();

        @Test
        @DisplayName("returns true when suffix matches")
        void endsWithSuffix() {
            final var args = new Arguments(
                    List.of("hello"), Map.of("suffix", "llo"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(true, result.value());
        }

        @Test
        @DisplayName("returns false when suffix does not match")
        void endsWithNoMatch() {
            final var args = new Arguments(
                    List.of("hello"), Map.of("suffix", "xyz"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(false, result.value());
        }

        @Test
        @DisplayName("returns true with case-insensitive suffix")
        void caseInsensitiveSuffix() {
            final var args = new Arguments(
                    List.of("Hello"),
                    Map.of("suffix", "LLO", "caseSensitive", false));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(true, result.value());
        }

        @Test
        @DisplayName("returns MISSING_NAMED_ARG when suffix is missing")
        void missingSuffix() {
            final var args = new Arguments(List.of("hello"), Map.of());
            final var result = evaluator.evaluate(null, args);

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.MISSING_NAMED_ARG.code(),
                    result.errors().get(0).errorCode().code());
        }
    }

    @Nested
    @DisplayName("StringMatchEvaluator")
    class StringMatchTests {

        private final StringMatchEvaluator evaluator = new StringMatchEvaluator();

        @Test
        @DisplayName("returns true when regex matches")
        void regexMatches() {
            final var args = new Arguments(
                    List.of("hello123"), Map.of("regex", "\\w+\\d+"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(true, result.value());
        }

        @Test
        @DisplayName("returns false when regex does not match")
        void regexNoMatch() {
            final var args = new Arguments(
                    List.of("hello"), Map.of("regex", "\\d+"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(false, result.value());
        }

        @Test
        @DisplayName("returns MISSING_NAMED_ARG when regex is missing")
        void missingRegex() {
            final var args = new Arguments(List.of("hello"), Map.of());
            final var result = evaluator.evaluate(null, args);

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.MISSING_NAMED_ARG.code(),
                    result.errors().get(0).errorCode().code());
        }

        @Test
        @DisplayName("returns error for invalid regex pattern")
        void invalidRegex() {
            final var args = new Arguments(
                    List.of("hello"), Map.of("regex", "[invalid"));
            final var result = evaluator.evaluate(null, args);

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.INVALID_REGEX.code(),
                    result.errors().get(0).errorCode().code());
        }
    }

    @Nested
    @DisplayName("StringFuzzyMatchEvaluator")
    class StringFuzzyMatchTests {

        private final StringFuzzyMatchEvaluator evaluator = new StringFuzzyMatchEvaluator();

        @Test
        @DisplayName("returns true when similarity above threshold")
        void fuzzyMatchAboveThreshold() {
            final var args = new Arguments(
                    List.of("hello"), Map.of("pattern", "hallo"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(true, result.value());
            final var attrs = result.attributes();
            assertEquals(0.8, (double) attrs.get("score"), 0.001);
            assertEquals("hello", attrs.get("input"));
            assertEquals("hallo", attrs.get("pattern"));
        }

        @Test
        @DisplayName("returns false when similarity below threshold")
        void fuzzyMatchBelowThreshold() {
            final var args = new Arguments(
                    List.of("hello"), Map.of("pattern", "xyzabc"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(false, result.value());
            final var attrs = result.attributes();
            assertTrue((double) attrs.get("score") < 0.3);
            assertEquals("hello", attrs.get("input"));
            assertEquals("xyzabc", attrs.get("pattern"));
        }

        @Test
        @DisplayName("returns MISSING_NAMED_ARG when pattern is missing")
        void missingPattern() {
            final var args = new Arguments(List.of("hello"), Map.of());
            final var result = evaluator.evaluate(null, args);

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.MISSING_NAMED_ARG.code(),
                    result.errors().get(0).errorCode().code());
        }
    }

    @Nested
    @DisplayName("StringSimilarityEvaluator")
    class StringSimilarityTests {

        private final StringSimilarityEvaluator evaluator = new StringSimilarityEvaluator();

        @Test
        @DisplayName("returns 1.0 for identical strings")
        void identicalStrings() {
            final var args = new Arguments(
                    List.of("hello"), Map.of("other", "hello"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertEquals(1.0, (double) result.value(), 0.001);
        }

        @Test
        @DisplayName("returns similarity score for different strings")
        void differentStrings() {
            final var args = new Arguments(
                    List.of("hello"), Map.of("other", "hallo"));
            final var result = evaluator.evaluate(null, args);

            assertTrue(result.errors().isEmpty());
            assertTrue((double) result.value() > 0);
        }

        @Test
        @DisplayName("returns MISSING_NAMED_ARG when other is missing")
        void missingOther() {
            final var args = new Arguments(List.of("hello"), Map.of());
            final var result = evaluator.evaluate(null, args);

            assertFalse(result.errors().isEmpty());
            assertEquals(StandardErrors.MISSING_NAMED_ARG.code(),
                    result.errors().get(0).errorCode().code());
        }
    }
}
