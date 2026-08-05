package io.github.khezyapp.dynamicform.engine;

import io.github.khezyapp.dynamicform.model.FieldIssue;
import io.github.khezyapp.dynamicform.model.FieldSchema;
import io.github.khezyapp.dynamicform.model.FileSpec;
import io.github.khezyapp.dynamicform.model.FormSchema;
import io.github.khezyapp.dynamicform.model.RenderType;
import io.github.khezyapp.dynamicform.model.RequiredWhen;
import io.github.khezyapp.dynamicform.model.ResolvedForm;
import io.github.khezyapp.dynamicform.spi.FileUploadProvider;
import io.github.khezyapp.dynamicform.spi.UploadedRef;
import io.github.khezyapp.dynamicform.value.FormValues;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The runtime, evaluation-driven resolver (P5).
 * <p>
 * Ordering is <strong>not</strong> a static topological sort: a field is resolved only once every
 * declaration of its name is decidable — all name-based dependencies are resolved — against live
 * values. This is what makes the n8n duplicate-name case work, where the same logical field
 * ({@code operation}) is declared more than once under different visibility conditions on another
 * field ({@code resource}).
 * <p>
 * One name = one value slot. When all declarations of a name are decidable, the engine picks the
 * single declaration whose visibility currently holds; more than one simultaneously visible is an
 * <em>ambiguous</em> schema error, and a name that never becomes decidable is an
 * <em>unresolvable dependency</em> (cycle or dangling reference) schema error.
 */
public final class ResolveEngine {

    private ResolveEngine() {
    }

    /**
     * Resolves a full schema.
     *
     * @param schema  the form schema
     * @param raw     the raw submitted values
     * @param ctx     the evaluation context
     * @param runtime the extension-point wiring
     * @return the resolved values and issues
     */
    public static ResolvedForm resolve(final FormSchema schema,
                                       final FormValues raw,
                                       final EvalContext ctx,
                                       final FormRuntime runtime) {
        return resolve(schema.fields(), raw, ctx, runtime);
    }

    /**
     * Resolves a list of field declarations (also used recursively for GROUP/COLLECTION items).
     *
     * @param fields  the field declarations
     * @param raw     the raw values
     * @param ctx     the evaluation context
     * @param runtime the extension-point wiring
     * @return the resolved values and issues
     */
    public static ResolvedForm resolve(final List<FieldSchema> fields,
                                       final FormValues raw,
                                       final EvalContext ctx,
                                       final FormRuntime runtime) {
        final var issues = new ArrayList<FieldIssue>();
        final var values = resolveScoped(fields, raw, ctx, runtime, "", issues);
        return new ResolvedForm(values, List.copyOf(issues));
    }

    /**
     * Returns the next field declaration that can be evaluated against the current values.
     * <p>
     * A name is decidable only when <strong>every</strong> declaration sharing it is decidable. It
     * then resolves to the single currently-visible declaration, or to the first declaration as a
     * representative when the name is hidden (the caller drops its value).
     *
     * @param fields        the field declarations
     * @param values        the current values
     * @param ctx           the evaluation context
     * @param scopePath     the nested scope prefix, empty at the top level
     * @param resolvedNames the names whose value slots have already been decided
     * @return the next declaration to process, or empty when nothing is decidable
     * @throws SchemaException on an ambiguous duplicate declaration
     */
    public static Optional<FieldSchema> nextDecidable(final List<FieldSchema> fields,
                                                      final FormValues values,
                                                      final EvalContext ctx,
                                                      final String scopePath,
                                                      final Set<String> resolvedNames) {
        final var order = new LinkedHashSet<String>();
        for (final var field : fields) {
            order.add(field.name());
        }
        for (final var name : order) {
            if (resolvedNames.contains(name)) {
                continue;
            }
            final var declarations = new ArrayList<FieldSchema>();
            for (final var field : fields) {
                if (field.name().equals(name)) {
                    declarations.add(field);
                }
            }
            final var allDecidable = declarations.stream()
                    .allMatch(declaration -> isDecidable(declaration, resolvedNames));
            if (!allDecidable) {
                continue;
            }
            final var visible = declarations.stream()
                    .filter(declaration -> VisibilityEvaluator.isVisible(declaration, values, ctx, scopePath))
                    .toList();
            if (visible.size() > 1) {
                throw new SchemaException("ambiguous declaration: field '" + name + "' has "
                        + visible.size() + " simultaneously visible declarations");
            }
            if (visible.size() == 1) {
                return Optional.of(visible.get(0));
            }
            return Optional.of(declarations.get(0));
        }
        return Optional.empty();
    }

    /**
     * The name-based dependencies of a field: visibility references plus conditional-required
     * dependencies. Special {@code @} context references and self-references are excluded.
     *
     * @param field the field
     * @return the dependency names
     */
    static Set<String> dependencyNames(final FieldSchema field) {
        final var deps = new HashSet<String>();
        final var visibility = field.visibility();
        if (Objects.nonNull(visibility)) {
            deps.addAll(visibility.show().keySet());
            deps.addAll(visibility.hide().keySet());
        }
        final var constraints = field.constraints();
        if (Objects.nonNull(constraints)) {
            for (final RequiredWhen requiredWhen : constraints.requiredWhen()) {
                deps.add(requiredWhen.when());
            }
        }
        deps.removeIf(dep -> dep.startsWith("@") || dep.equals(field.name()));
        return deps;
    }

    private static boolean isDecidable(final FieldSchema field,
                                       final Set<String> resolvedNames) {
        final var deps = dependencyNames(field);
        return deps.isEmpty() || resolvedNames.containsAll(deps);
    }

    private static FormValues resolveScoped(final List<FieldSchema> fields,
                                            final FormValues work,
                                            final EvalContext ctx,
                                            final FormRuntime runtime,
                                            final String scopePath,
                                            final List<FieldIssue> issues) {
        var current = work;
        final var resolvedNames = new HashSet<String>();
        while (true) {
            final var next = nextDecidable(fields, current, ctx, scopePath, resolvedNames).orElse(null);
            if (Objects.isNull(next)) {
                final var remaining = new ArrayList<String>();
                for (final var field : fields) {
                    if (!resolvedNames.contains(field.name())) {
                        remaining.add(field.name());
                    }
                }
                if (!remaining.isEmpty()) {
                    throw new SchemaException("unresolvable dependency for field(s) " + remaining
                            + " (cycle or dangling reference)");
                }
                return current;
            }
            final var path = scopePath.isEmpty() ? next.name() : scopePath + "." + next.name();
            if (VisibilityEvaluator.isVisible(next, current, ctx, scopePath)) {
                current = processField(next, current, ctx, runtime, scopePath, path, issues);
            } else {
                current = current.without(path);
            }
            resolvedNames.add(next.name());
        }
    }

    private static FormValues processField(final FieldSchema field,
                                           final FormValues work,
                                           final EvalContext ctx,
                                           final FormRuntime runtime,
                                           final String scopePath,
                                           final String path,
                                           final List<FieldIssue> issues) {
        if (Objects.isNull(field.valueType())) {
            return work.without(path);
        }
        if (field.renderType() == RenderType.GROUP) {
            return processGroup(field, work, ctx, runtime, path, issues);
        }
        if (field.renderType() == RenderType.COLLECTION) {
            return processCollection(field, work, ctx, runtime, path, issues);
        }
        final var current = DefaultFiller.fillIfAbsent(field, work.get(path));
        if (field.renderType() == RenderType.FILE) {
            return processFile(field, work, ctx, runtime, scopePath, path, issues);
        }
        final var result = Coercer.coerce(field.valueType(), current, field.constraints());
        if (result.success()) {
            issues.addAll(Validator.validate(field, result.value(), work, ctx, path, scopePath));
            return work.with(path, result.value());
        }
        issues.add(FieldIssue.error(path, result.message()));
        return work;
    }

    private static FormValues processGroup(final FieldSchema field,
                                           final FormValues work,
                                           final EvalContext ctx,
                                           final FormRuntime runtime,
                                           final String path,
                                           final List<FieldIssue> issues) {
        var updated = work;
        if (!updated.has(path) && field.defaultValue() instanceof Map<?, ?> defaults) {
            updated = updated.with(path, defaults);
        }
        updated = resolveScoped(field.children(), updated, ctx, runtime, path, issues);
        if (!updated.has(path)) {
            updated = updated.with(path, Map.of());
        }
        final var groupValue = updated.get(path);
        issues.addAll(Validator.validate(field, groupValue, updated, ctx, path, path));
        return updated;
    }

    private static FormValues processCollection(final FieldSchema field,
                                                final FormValues work,
                                                final EvalContext ctx,
                                                final FormRuntime runtime,
                                                final String path,
                                                final List<FieldIssue> issues) {
        final var spec = field.collection();
        var current = work.get(path);
        if (Objects.isNull(current) && field.defaultValue() instanceof List<?> defaults) {
            current = defaults;
        }
        final var rows = new ArrayList<>();
        if (current instanceof List<?> list) {
            rows.addAll(list);
        } else if (Objects.nonNull(current)) {
            issues.add(FieldIssue.error(path, "expected a list of rows"));
        }
        final var itemSchema = Objects.nonNull(spec) ? spec.itemSchema() : List.<FieldSchema>of();
        if (Objects.nonNull(spec) && Objects.nonNull(spec.maxItems()) && rows.size() > spec.maxItems()) {
            issues.add(FieldIssue.error(path, "must not exceed " + spec.maxItems() + " rows"));
        }
        var updated = work;
        for (int i = 0; i < rows.size(); i++) {
            final var rowPath = path + "[" + i + "]";
            if (!(rows.get(i) instanceof Map<?, ?>)) {
                issues.add(FieldIssue.error(rowPath, "row must be an object"));
                continue;
            }
            updated = updated.with(rowPath, rows.get(i));
            updated = resolveScoped(itemSchema, updated, ctx, runtime, rowPath, issues);
        }
        if (Objects.nonNull(spec) && Objects.nonNull(spec.minItems()) && rows.size() < spec.minItems()) {
            issues.add(FieldIssue.error(path, "must have at least " + spec.minItems() + " rows"));
        }
        issues.addAll(Validator.validate(field, updated.get(path), updated, ctx, path, path));
        return updated;
    }

    private static FormValues processFile(final FieldSchema field,
                                          final FormValues work,
                                          final EvalContext ctx,
                                          final FormRuntime runtime,
                                          final String scopePath,
                                          final String path,
                                          final List<FieldIssue> issues) {
        final var current = DefaultFiller.fillIfAbsent(field, work.get(path));
        final var provider = runtime.uploadProviderFor(field);
        final Object result;
        if (current instanceof List<?> list) {
            final var spec = field.file();
            if (Objects.nonNull(spec) && spec.maxCount() > 0 && list.size() > spec.maxCount()) {
                issues.add(FieldIssue.error(path, "must not exceed " + spec.maxCount() + " files"));
            }
            final var refs = new ArrayList<>();
            for (final Object item : list) {
                final var saved = saveSingleFile(field, provider, item, ctx, path, issues);
                if (Objects.nonNull(saved)) {
                    refs.add(saved);
                }
            }
            result = refs;
        } else {
            result = saveSingleFile(field, provider, current, ctx, path, issues);
        }
        issues.addAll(Validator.validate(field, result, work, ctx, path, scopePath));
        return work.with(path, result);
    }

    private static Object saveSingleFile(final FieldSchema field,
                                         final FileUploadProvider provider,
                                         final Object item,
                                         final EvalContext ctx,
                                         final String path,
                                         final List<FieldIssue> issues) {
        final var spec = field.file();
        if (Objects.isNull(item)) {
            return null;
        }
        if (item instanceof UploadedRef ref) {
            validateFilePolicy(spec, ref.mime(), ref.size(), path, issues);
            return ref;
        }
        if (item instanceof byte[] bytes) {
            validateFilePolicy(spec, null, bytes.length, path, issues);
            return provider.save(bytes, field.name(), "application/octet-stream", field, ctx);
        }
        if (item instanceof String ref) {
            return ref;
        }
        if (item instanceof Map<?, ?> upload) {
            if (upload.get("bytes") instanceof byte[] bytes) {
                final var fileName = upload.containsKey("fileName")
                        ? String.valueOf(upload.get("fileName")) : field.name();
                final var mime = upload.containsKey("mime")
                        ? String.valueOf(upload.get("mime")) : "application/octet-stream";
                validateFilePolicy(spec, mime, bytes.length, path, issues);
                return provider.save(bytes, fileName, mime, field, ctx);
            }
            if (upload.containsKey("ref") || upload.containsKey("url")) {
                return upload;
            }
        }
        issues.add(FieldIssue.error(path, "unsupported file value: " + item.getClass().getSimpleName()));
        return null;
    }

    private static void validateFilePolicy(final FileSpec spec,
                                           final String mime,
                                           final long size,
                                           final String path,
                                           final List<FieldIssue> issues) {
        if (Objects.isNull(spec)) {
            return;
        }
        if (!spec.accept().isEmpty() && Objects.nonNull(mime)
                && spec.accept().stream().noneMatch(accepted -> acceptMime(accepted, mime))) {
            issues.add(FieldIssue.error(path, "file type '" + mime + "' is not accepted"));
        }
        if (spec.maxBytes() > 0 && size > spec.maxBytes()) {
            issues.add(FieldIssue.error(path, "file exceeds maxBytes " + spec.maxBytes()));
        }
    }

    private static boolean acceptMime(final String accepted,
                                      final String mime) {
        if (accepted.equals("*/*") || accepted.equalsIgnoreCase(mime)) {
            return true;
        }
        if (accepted.endsWith("/*")) {
            return mime.startsWith(accepted.substring(0, accepted.length() - 1));
        }
        return false;
    }
}
