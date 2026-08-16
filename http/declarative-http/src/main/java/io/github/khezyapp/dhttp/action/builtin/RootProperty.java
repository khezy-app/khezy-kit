package io.github.khezyapp.dhttp.action.builtin;

import io.github.khezyapp.dhttp.action.PostReceiveAction;
import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.transport.HttpResult;

import java.util.List;
import java.util.Objects;

/**
 * Reads a single dotted property from the response body and uses it as the record list ({@code R4}).
 * Covers {@link PostReceive.RootProperty}.
 *
 * @param property the dotted path to read
 */
public record RootProperty(String property) implements PostReceiveAction {

    public RootProperty {
        Objects.requireNonNull(property, "property");
    }

    public static RootProperty from(final PostReceive descriptor) {
        if (descriptor instanceof PostReceive.RootProperty rootProperty) {
            return new RootProperty(rootProperty.property());
        }
        return new RootProperty(String.valueOf(BuiltinSupport.prop(descriptor, "property")));
    }

    @Override
    public List<OutputRecord> apply(final List<OutputRecord> records,
                                    final HttpResult response) {
        final var body = BuiltinSupport.parseBody(response, JacksonJsonMapper.INSTANCE);
        return BuiltinSupport.recordsFromBody(body, property);
    }
}
