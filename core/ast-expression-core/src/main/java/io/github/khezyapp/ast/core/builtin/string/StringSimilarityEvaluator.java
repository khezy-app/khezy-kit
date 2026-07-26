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
 * Evaluator for computing string similarity scores.
 * <p>
 * Supports multiple algorithms: {@code levenshtein} (default) and
 * {@code jaroWinkler}. Returns a score between 0.0 and 1.0.
 * </p>
 */
public class StringSimilarityEvaluator implements Evaluator {

    @Override
    public EvaluationOutcome evaluate(final EvaluationContext ctx,
                                      final Arguments args) {
        final var input = (String) args.positional().get(0);
        final var other = (String) args.named().get("other");
        final var algorithm = (String) args.named()
                .getOrDefault("algorithm", "levenshtein");
        final boolean caseSensitive = (boolean) args.named()
                .getOrDefault("caseSensitive", true);

        if (Objects.isNull(other)) {
            return EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.MISSING_NAMED_ARG,
                            "other is required", "named:other"));
        }
        if (Objects.isNull(input)) {
            return EvaluationOutcome.success(0.0,
                    Map.of("other", other, "algorithm", algorithm, "caseSensitive", caseSensitive));
        }

        final var a = caseSensitive ? input : input.toLowerCase();
        final var b = caseSensitive ? other : other.toLowerCase();

        final double score = switch (algorithm) {
            case "levenshtein" -> levenshteinSimilarity(a, b);
            case "jaroWinkler" -> jaroWinklerSimilarity(a, b);
            default -> levenshteinSimilarity(a, b);
        };

        return EvaluationOutcome.success(score,
                Map.of("input", input, "other", other, "algorithm", algorithm,
                        "caseSensitive", caseSensitive, "score", score));
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
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                final int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    private static double jaroWinklerSimilarity(final String a,
                                                final String b) {
        if (a.equals(b)) {
            return 1.0;
        }
        var matchRange = Math.max(a.length(), b.length()) / 2 - 1;
        if (matchRange < 0) {
            matchRange = 0;
        }

        final boolean[] aMatched = new boolean[a.length()];
        final boolean[] bMatched = new boolean[b.length()];
        var matches = 0;
        var transpositions = 0;

        for (int i = 0; i < a.length(); i++) {
            final int start = Math.max(0, i - matchRange);
            final int end = Math.min(b.length(), i + matchRange + 1);
            for (int j = start; j < end; j++) {
                if (bMatched[j]) {
                    continue;
                }
                if (a.charAt(i) != b.charAt(j)) {
                    continue;
                }
                aMatched[i] = true;
                bMatched[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        int k = 0;
        for (var i = 0; i < a.length(); i++) {
            if (!aMatched[i]) {
                continue;
            }
            while (!bMatched[k]) {
                k++;
            }
            if (a.charAt(i) != b.charAt(k)) {
                transpositions++;
            }
            k++;
        }

        final double jaro = (1.0 / 3.0) * (
                (double) matches / a.length()
                        + (double) matches / b.length()
                        + (double) (matches - transpositions / 2) / matches
        );

        int prefix = 0;
        for (int i = 0; i < Math.min(4,
                Math.min(a.length(), b.length())); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                prefix++;
            } else {
                break;
            }
        }

        return jaro + prefix * 0.1 * (1.0 - jaro);
    }
}
