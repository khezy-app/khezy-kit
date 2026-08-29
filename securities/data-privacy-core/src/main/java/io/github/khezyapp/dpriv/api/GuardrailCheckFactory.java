package io.github.khezyapp.dpriv.api;

/**
 * Extensibility seam for registering checks even when instantiation needs configuration
 * (design §5.2 note). Used with reflection-free, preconfigured instances: {@code config -> check}.
 *
 * @param <C> the configuration type required to build a check
 */
@FunctionalInterface
public interface GuardrailCheckFactory<C> {

    /**
     * Builds a {@link GuardrailCheck} from the given configuration.
     *
     * @param config the configuration to build from
     * @return the configured check
     */
    GuardrailCheck create(C config);
}
