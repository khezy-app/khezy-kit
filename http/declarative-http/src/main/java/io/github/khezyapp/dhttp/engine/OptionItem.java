package io.github.khezyapp.dhttp.engine;

import java.util.Objects;

/**
 * One option for a design-time dropdown ({@code R15}): the display label, the submitted value, and
 * optional presentation metadata for complex dropdown UIs.
 *
 * @param name        the display label
 * @param value       the value submitted whens the option is chosen
 * @param description optional explanatory text about the option
 * @param icon        optional icon token (emoji, icon-library key, or http(s) image URL)
 * @param group       optional group/category used for optgroup rendering
 * @param disabled    whether the option cannot be chosen, defaults to {@code false}
 */
public record OptionItem(String name, String value, String description, String icon,
                         String group, boolean disabled) {

    public OptionItem {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
    }

    /**
     * Creates an option without presentation metadata.
     *
     * @param name  the display label
     * @param value the value submitted whens the option is chosen
     */
    public OptionItem(final String name, final String value) {
        this(name, value, null, null, null, false);
    }
}
