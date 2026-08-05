package io.github.khezyapp.ast.core.builtin.string;

import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.eval.EvaluationContext;
import io.github.khezyapp.ast.core.eval.Evaluator;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;

import java.util.Map;
import java.util.Objects;

/**
 * Evaluator for fuzzy string matching using Levenshtein distance.
 * <p>
 * Returns {@code true} if the similarity score is at or above the configured
 * threshold (default: {@code 0.8}). Supports case-insensitive matching via
 * the named argument {@code caseSensitive}.
 * </p>
 */
public class StringFuzzyMatchEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var input = (String) args.positional().get(0);
        final var pattern = (String) args.named().get("pattern");
        final boolean caseSensitive = (boolean) args.named()
                .getOrDefault("caseSensitive", true);
        final double threshold = ((Number) args.named()
                .getOrDefault("threshold", 0.8)).doubleValue();

        if (Objects.isNull(pattern)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "pattern is required", "named:pattern"));
        }
        if (Objects.isNull(input)) {
            return EvaluationOutcome.success(false);
        }

        final var a = caseSensitive ? input : input.toLowerCase();
        final var b = caseSensitive ? pattern : pattern.toLowerCase();

        final double similarity = levenshteinSimilarity(a, b);
        return EvaluationOutcome.success(similarity >= threshold,
                Map.of("score", similarity, "input", input, "pattern", pattern));
    }

    private static double levenshteinSimilarity(final String a,
                                                 final String b) {
        final int distance = levenshteinDistance(a, b);
        final int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 1.0;
        }
        return 1.0 - ((double) distance / maxLen);
    }

    private static int levenshteinDistance(final String a,
                                            final String b) {
        final var dp = new int[a.length() + 1][b.length() + 1];
        for (var i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (var j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (var i = 1; i <= a.length(); i++) {
            for (var j = 1; j <= b.length(); j++) {
                final var cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
