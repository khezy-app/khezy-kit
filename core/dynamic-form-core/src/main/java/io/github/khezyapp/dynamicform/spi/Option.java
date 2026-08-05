package io.github.khezyapp.dynamicform.spi;

/**
 * A single selectable option served by an {@link OptionsProvider}.
 *
 * @param name  the human-readable label rendered in the UI
 * @param value the value that is submitted when this option is chosen
 * @param meta  optional extra data surfaced to the UI (e.g. a flag, an icon key)
 */
public record Option(String name, String value, Object meta) {

    /**
     * Creates an option without extra metadata.
     *
     * @param name  the display label
     * @param value the submitted value
     * @return a new option
     */
    public static Option of(final String name, final String value) {
        return new Option(name, value, null);
    }
}
