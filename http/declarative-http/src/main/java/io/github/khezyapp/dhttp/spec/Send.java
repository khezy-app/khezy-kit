package io.github.khezyapp.dhttp.spec;

import lombok.Builder;

import java.util.Objects;

/**
 * Maps an operation parameter to a location in the outgoing request body or query.
 *
 * @param fromParam     the source parameter name
 * @param target        whether to place the value in the body or the query
 * @param property      the target key or dot-notation path
 * @param dotNotation   whens true, {@code property} is resolved via {@code DynamicObjects}
 * @param valueOverride an optional constant/expression that replaces the parameter value
 */
@Builder
public record Send(String fromParam,
                   Target target,
                   String property,
                   boolean dotNotation,
                   Expression valueOverride) {

    public Send {
        Objects.requireNonNull(fromParam, "fromParam");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(property, "property");
    }
}
