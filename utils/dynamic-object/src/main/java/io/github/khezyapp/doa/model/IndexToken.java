package io.github.khezyapp.doa.model;

/** Represents a positional access (e.g., "[0]"). */
public record IndexToken(int index) implements PathToken {
}
