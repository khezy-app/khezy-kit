package io.github.khezyapp.doa.model;

/** Represents a named property access (e.g., ".name"). */
public record PropertyToken(String name, boolean safeAccess) implements PathToken {
}
