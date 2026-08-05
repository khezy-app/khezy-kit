package io.github.khezyapp.dynamicform.model;

import io.github.khezyapp.dynamicform.spi.Option;

import java.util.List;
import java.util.Objects;

/**
 * Dynamic options for a select-like field.
 * <p>
 * Options are provided either {@code inline} (a static list embedded in the schema) or
 * {@code by reference} — {@code provider} names an {@link
 * io.github.khezyapp.dynamicform.spi.OptionsProvider} loaded at render time. {@code dependsOn} is an
 * optional UI refetch/cache hint (e.g. a {@code state} select depends on {@code country}); it does
 * not drive resolution ordering.
 *
 * @param inline    the static option list, may be empty
 * @param provider  the name of an {@code OptionsProvider}, may be {@code null} for inline-only
 * @param dependsOn optional dependency names hinting the UI when to refetch
 */
public record Options(
        List<Option> inline,
        String provider,
        List<String> dependsOn
) {

    /**
     * Compact canonical constructor that normalises null lists.
     */
    public Options {
        inline = Objects.nonNull(inline) ? List.copyOf(inline) : List.of();
        dependsOn = Objects.nonNull(dependsOn) ? List.copyOf(dependsOn) : List.of();
    }

    /**
     * Creates static inline options.
     *
     * @param inline the option list
     * @return a new options carrier
     */
    public static Options inline(final List<Option> inline) {
        return new Options(inline, null, null);
    }

    /**
     * Creates a provider reference.
     *
     * @param provider the {@code OptionsProvider} name
     * @return a new options carrier
     */
    public static Options provider(final String provider) {
        return new Options(null, provider, null);
    }
}
