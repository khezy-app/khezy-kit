package io.github.khezyapp.dynamicform.spi;

import java.util.List;

/**
 * Loads the dynamic options for a field that references a provider by name.
 * <p>
 * Consumers implement this interface and register instances in an
 * {@link OptionsProviderRegistry}. Options are resolved <em>by reference</em> — the schema stays
 * static while the data is loaded at render time. The provider receives the current form values,
 * enabling country → state style cascades without any schema coupling.
 */
@FunctionalInterface
public interface OptionsProvider {

    /**
     * Loads the options for the given request.
     *
     * @param request the request carrying current values, context, and optional filter/pagination
     * @return the list of options to render, never {@code null}
     */
    List<Option> load(OptionRequest request);
}
