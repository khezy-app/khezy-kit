package io.github.khezyapp.dpriv.api;

/**
 * PII coverage mode (design §5.4). {@link #ALL} scans every catalog entity; {@link #SELECTED}
 * scans only the entities configured in {@link PiiConfig}.
 */
public enum PiiCoverage {
    ALL,
    SELECTED
}
