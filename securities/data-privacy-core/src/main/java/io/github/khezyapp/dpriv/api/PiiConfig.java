package io.github.khezyapp.dpriv.api;

import io.github.khezyapp.dpriv.policy.PiiEntity;

import java.util.List;
import java.util.Set;

/**
 * PII policy configuration (design §5.4 plus the resolved {@code strict} field). {@code ALL}
 * coverage ignores {@code entities}; {@code SELECTED} scans only the listed entities.
 *
 * @param coverage      whether to scan all entities or only the selected ones
 * @param entities      the selected entities (used when coverage is {@link PiiCoverage#SELECTED})
 * @param customRegexes user-named custom regex rules
 * @param strict        whether checksum-based validation is enabled (design §8.2)
 */
public record PiiConfig(
        PiiCoverage coverage,
        Set<PiiEntity> entities,
        List<CustomRegexConfig> customRegexes,
        boolean strict) {

    /**
     * Defaults: all coverage, no selected entities, no custom regexes, strict checksum validation
     * enabled.
     */
    public static final PiiConfig DEFAULTS = new PiiConfig(PiiCoverage.ALL, Set.of(), List.of(), true);
}
