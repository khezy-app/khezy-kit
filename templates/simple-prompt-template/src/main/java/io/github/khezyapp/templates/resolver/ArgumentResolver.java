package io.github.khezyapp.templates.resolver;

import io.github.khezyapp.templates.TemplateContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves positional argument placeholders in template strings.
 * <p>
 * Supported placeholders:
 * <ul>
 *   <li>{@code $1}, {@code $2}, ... — individual positional arguments</li>
 *   <li>{@code $ARGUMENTS} — all arguments joined by spaces</li>
 * </ul>
 * <p>
 * The highest-numbered positional placeholder slurps all remaining arguments
 * from that position onward. For example, with args {@code [a, b, c, d]},
 * {@code $2} resolves to {@code "b c d"}.
 */
public final class ArgumentResolver implements PlaceholderResolver {

    private static final Pattern ARG_PATTERN = Pattern.compile("\\$ARGUMENTS|\\$(\\d+)");

    @Override
    public String resolve(final String template, final TemplateContext ctx) {
        final var args = ctx.positionalArgs();

        final var highestPos = findHighestPosition(template);
        if (highestPos < 0 && !template.contains("$ARGUMENTS")) {
            return template;
        }

        final var matcher = ARG_PATTERN.matcher(template);
        final var sb = new StringBuffer();

        while (matcher.find()) {
            final var match = matcher.group();
            if ("$ARGUMENTS".equals(match)) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(ctx.rawInput()));
            } else {
                final var pos = Integer.parseInt(matcher.group(1));
                if (pos == highestPos) {
                    final var endIndex = Math.min(pos - 1, args.size());
                    final var rest = args.subList(endIndex, args.size());
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(String.join(" ", rest)));
                } else {
                    final var value = pos <= args.size() ? args.get(pos - 1) : "";
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
                }
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private int findHighestPosition(final String template) {
        final var matcher = ARG_PATTERN.matcher(template);
        var highest = -1;
        while (matcher.find()) {
            final var num = matcher.group(1);
            if (num != null) {
                highest = Math.max(highest, Integer.parseInt(num));
            }
        }
        return highest;
    }
}
