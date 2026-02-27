package io.github.khezyapp.doa.model;

/**
 * Marker interface for a specific step in a path navigation.
 */
public sealed interface PathToken permits PropertyToken, IndexToken {
}
