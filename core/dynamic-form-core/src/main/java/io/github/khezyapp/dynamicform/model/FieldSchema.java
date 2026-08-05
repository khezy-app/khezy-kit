package io.github.khezyapp.dynamicform.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The orthogonal-facets contract for a single field (P3).
 * <p>
 * Each facet is independent: {@code renderType} (widget) vs {@code valueType} (data),
 * {@code defaultValue} (always applied when absent), {@code constraints} (required / bounds /
 * precision), {@code visibility} (data predicate), {@code options} (static list or provider
 * reference), and the composition facets — {@code children} for {@code GROUP} (a single nested
 * object) and {@code collection} for {@code COLLECTION} (repeatable rows). {@code FILE} fields carry
 * an upload policy, and {@code actions} declare UI side-effects.
 * <p>
 * A field's value lives at {@code FormValues[field.name]} — one name, one value slot; the resolver
 * picks the declaration whose visibility currently holds when the same name is declared more than
 * once.
 *
 * @param name           identity; the value slot name (dot-paths address nested groups)
 * @param displayNameKey i18n key for the field label
 * @param renderType     the widget type
 * @param valueType      the data type after coercion; {@code null} for value-less nodes
 * @param defaultValue   the value applied whenever the field is absent (P6)
 * @param constraints    required / requiredWhen / bounds / scale / length / pattern
 * @param visibility     the declarative show/hide predicate (P4)
 * @param options        inline options or a provider reference (P8)
 * @param children       the nested fields of a {@code GROUP}
 * @param collection     the repeatable-row spec of a {@code COLLECTION}
 * @param file           the upload policy of a {@code FILE} field
 * @param actions        declared UI side-effects (P12)
 * @param meta           arbitrary extra data (e.g. a {@code textKey} for {@code NOTICE})
 */
public record FieldSchema(
        String name,
        String displayNameKey,
        RenderType renderType,
        ValueType valueType,
        @JsonProperty("default") Object defaultValue,
        Constraints constraints,
        Visibility visibility,
        Options options,
        List<FieldSchema> children,
        CollectionSpec collection,
        FileSpec file,
        List<FieldAction> actions,
        Map<String, Object> meta
) {

    /**
     * Compact canonical constructor that normalises null collection facets.
     */
    public FieldSchema {
        name = Objects.requireNonNull(name, "name must not be null");
        children = Objects.nonNull(children) ? List.copyOf(children) : List.of();
        actions = Objects.nonNull(actions) ? List.copyOf(actions) : List.of();
        meta = Objects.nonNull(meta) ? Map.copyOf(meta) : Map.of();
    }

    /**
     * Creates the simplest possible field with no facets.
     *
     * @param name       the field name
     * @param renderType the widget type
     * @param valueType  the value type
     * @return a minimal field
     */
    public static FieldSchema of(final String name,
                                 final RenderType renderType,
                                 final ValueType valueType) {
        return new FieldSchema(name, null, renderType, valueType, null, null, null, null, null, null,
                null, null, null);
    }

    /**
     * Starts a fluent builder for a field schema.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link FieldSchema}.
     */
    public static final class Builder {
        private String name;
        private String displayNameKey;
        private RenderType renderType;
        private ValueType valueType;
        private Object defaultValue;
        private Constraints constraints;
        private Visibility visibility;
        private Options options;
        private List<FieldSchema> children;
        private CollectionSpec collection;
        private FileSpec file;
        private List<FieldAction> actions;
        private Map<String, Object> meta;

        private Builder() {
        }

        /**
         * Sets the field name.
         *
         * @param name the name
         * @return this builder
         */
        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the display i18n key.
         *
         * @param displayNameKey the key
         * @return this builder
         */
        public Builder displayNameKey(final String displayNameKey) {
            this.displayNameKey = displayNameKey;
            return this;
        }

        /**
         * Sets the render type.
         *
         * @param renderType the widget type
         * @return this builder
         */
        public Builder renderType(final RenderType renderType) {
            this.renderType = renderType;
            return this;
        }

        /**
         * Sets the value type.
         *
         * @param valueType the data type
         * @return this builder
         */
        public Builder valueType(final ValueType valueType) {
            this.valueType = valueType;
            return this;
        }

        /**
         * Sets the default value.
         *
         * @param defaultValue the default
         * @return this builder
         */
        public Builder defaultValue(final Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Sets the constraints.
         *
         * @param constraints the constraints
         * @return this builder
         */
        public Builder constraints(final Constraints constraints) {
            this.constraints = constraints;
            return this;
        }

        /**
         * Sets the visibility rule.
         *
         * @param visibility the visibility
         * @return this builder
         */
        public Builder visibility(final Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        /**
         * Sets the options.
         *
         * @param options the options
         * @return this builder
         */
        public Builder options(final Options options) {
            this.options = options;
            return this;
        }

        /**
         * Sets the nested children (GROUP composition).
         *
         * @param children the children
         * @return this builder
         */
        public Builder children(final List<FieldSchema> children) {
            this.children = children;
            return this;
        }

        /**
         * Sets the collection spec.
         *
         * @param collection the spec
         * @return this builder
         */
        public Builder collection(final CollectionSpec collection) {
            this.collection = collection;
            return this;
        }

        /**
         * Sets the file policy.
         *
         * @param file the policy
         * @return this builder
         */
        public Builder file(final FileSpec file) {
            this.file = file;
            return this;
        }

        /**
         * Sets the declared actions.
         *
         * @param actions the actions
         * @return this builder
         */
        public Builder actions(final List<FieldAction> actions) {
            this.actions = actions;
            return this;
        }

        /**
         * Sets the extra metadata.
         *
         * @param meta the metadata
         * @return this builder
         */
        public Builder meta(final Map<String, Object> meta) {
            this.meta = meta;
            return this;
        }

        /**
         * Builds the field schema.
         *
         * @return the immutable field
         */
        public FieldSchema build() {
            return new FieldSchema(this.name, this.displayNameKey, this.renderType, this.valueType,
                    this.defaultValue, this.constraints, this.visibility, this.options, this.children,
                    this.collection, this.file, this.actions, this.meta);
        }
    }
}
