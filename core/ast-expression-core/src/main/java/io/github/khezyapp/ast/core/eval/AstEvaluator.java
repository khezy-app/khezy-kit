package io.github.khezyapp.ast.core.eval;

import java.util.Objects;

import io.github.khezyapp.ast.core.CoreUtils;
import io.github.khezyapp.ast.core.error.EvaluationError;
import io.github.khezyapp.ast.core.error.StandardErrors;
import io.github.khezyapp.ast.core.function.FunctionDefinition;
import io.github.khezyapp.ast.core.model.Arguments;
import io.github.khezyapp.ast.core.model.FunctionId;
import io.github.khezyapp.ast.core.model.Node;

import io.github.khezyapp.ast.core.nullstrategy.NullHandlingStrategy;
import io.github.khezyapp.ast.core.result.EvaluationOutcome;
import io.github.khezyapp.ast.core.result.EvaluationResult;
import io.github.khezyapp.ast.core.result.EvaluationTrace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recursive AST evaluation engine that walks a {@link io.github.khezyapp.ast.core.model.Node}
 * tree and produces an {@link io.github.khezyapp.ast.core.result.EvaluationResult}.
 * <p>
 * The evaluation pipeline consists of the following phases:
 * <ol>
 *   <li><b>Cache lookup</b> — return cached result if available</li>
 *   <li><b>Constant shortcut</b> — return immediately for constant nodes</li>
 *   <li><b>Child evaluation</b> — evaluate positional children (with short-circuit support)</li>
 *   <li><b>Named child evaluation</b> — evaluate named children</li>
 *   <li><b>Error propagation</b> — collect child errors</li>
 *   <li><b>Argument assembly</b> — build resolved arguments from child results</li>
 *   <li><b>Null strategy</b> — apply per-function or registry null handling</li>
 *   <li><b>Validation</b> — validate arguments against function parameter specs</li>
 *   <li><b>Invocation</b> — call the registered evaluator</li>
 *   <li><b>Result construction</b> — build the result with trace information</li>
 * </ol>
 * </p>
 */
public final class AstEvaluator {

    /**
     * Evaluates an AST node and returns the result.
     *
     * @param node the root node of the AST to evaluate
     * @param ctx  the evaluation context (registry, cache, payload, etc.)
     * @return the evaluation result with return value, errors, and trace information
     */
    public EvaluationResult evaluate(final Node node,
                                     final EvaluationContext ctx) {
        final long hash = node.hashCode();
        final var cache = ctx.cache();
        if (Objects.nonNull(cache)) {
            final var cached = cache.get(hash);
            if (Objects.nonNull(cached)) {
                return cached.withTrace(new EvaluationTrace(false, true, 0));
            }
        }

        if (node.isConstant()) {
            final var result = EvaluationResult.constant(node.constant());
            if (Objects.nonNull(cache)) {
                cache.put(hash, result);
            }
            return result;
        }

        final var startNanos = System.nanoTime();
        final var def = ctx.registry().getDefinition(node.function());

        // Phase 1: evaluate positional children (with short-circuit support)
        final var childrenAndSc = evaluatePositionalChildren(node.children(), ctx, def);

        // Phase 2: evaluate named children
        final var namedChildren = evaluateNamedChildren(node.namedChildren(), ctx);

        // If short-circuited, return immediately
        if (childrenAndSc.shortCircuited) {
            final var duration = System.nanoTime() - startNanos;
            final var result = EvaluationResult.shortCircuit(
                    node.function(),
                    childrenAndSc.shortCircuitValue,
                    childrenAndSc.children,
                    namedChildren,
                    duration
            );
            if (Objects.nonNull(cache)) {
                cache.put(hash, result);
            }
            return result;
        }

        final var children = childrenAndSc.children;

        // Phase 3: check child errors
        final var childErrors = children.stream()
                .filter(r -> CoreUtils.isNotEmpty(r.errors()))
                .flatMap(r -> r.errors().stream())
                .toList();
        if (!childErrors.isEmpty()) {
            final long duration = System.nanoTime() - startNanos;
            return buildFailureResult(node.function(), children, namedChildren,
                    childErrors, duration);
        }

        // Phase 4: build raw arguments from child results
        final var positionalValues = children.stream()
                .map(EvaluationResult::returnValue)
                .toList();
        final var namedValues = CoreUtils.transormMap(
                namedChildren,
                Map.Entry::getKey,
                e -> e.getValue().returnValue()
        );
        final var rawArgs = new Arguments(positionalValues, namedValues);

        // Phase 5: apply null strategy — per-function first, then registry fallback
        final var effectiveStrategy = Objects.nonNull(def.nullStrategy())
                ? def.nullStrategy()
                : ctx.registry().nullHandlingStrategy();
        final var resolvedArgs = applyNullStrategy(rawArgs, def, effectiveStrategy);

        // Phase 6: validate arguments against function definition
        final var validationErrors = def.validate(resolvedArgs);
        if (!validationErrors.isEmpty()) {
            final long duration = System.nanoTime() - startNanos;
            return buildFailureResult(node.function(), children, namedChildren,
                    validationErrors, duration);
        }

        // Phase 7: invoke evaluator
        EvaluationOutcome outcome;
        try {
            outcome = def.evaluator().evaluate(ctx, resolvedArgs);
        } catch (final Exception e) {
            outcome = EvaluationOutcome.failure(
                    EvaluationError.of(StandardErrors.RUNTIME_ERROR,
                            "Evaluator threw exception: " + e.getMessage()));
        }

        // Phase 8: build result with evidence
        final long duration = System.nanoTime() - startNanos;
        final var result = EvaluationResult.function(node.function(), outcome,
                children, namedChildren, new EvaluationTrace(false, false, duration));

        if (Objects.nonNull(cache) && CoreUtils.isEmpty(result.errors())) {
            cache.put(hash, result);
        }
        return result;
    }

    // Need Objects import for isNull/nonNull
    private ChildResult evaluatePositionalChildren(
            final List<Node> nodes,
            final EvaluationContext ctx,
            final FunctionDefinition def
    ) {
        final var attrs = def.attributes();
        final var results = new ArrayList<EvaluationResult>(nodes.size());

        for (int i = 0; i < nodes.size(); i++) {
            final var childResult = evaluate(nodes.get(i), ctx);
            results.add(childResult);

            if (attrs.lazyChildEvaluation()
                    && Objects.nonNull(attrs.shortCircuitPredicate())
                    && childResult.errors().isEmpty()
                    && attrs.shortCircuitPredicate().test(childResult)) {
                final var scValue = childResult.returnValue();
                for (int j = i + 1; j < nodes.size(); j++) {
                    results.add(EvaluationResult.skipped(nodes.get(j).function()));
                }
                return new ChildResult(true, scValue, List.copyOf(results));
            }
        }

        return new ChildResult(false, null, List.copyOf(results));
    }

    private Map<String, EvaluationResult> evaluateNamedChildren(
            final Map<String, Node> namedNodes,
            final EvaluationContext ctx
    ) {
        final var results = new HashMap<String, EvaluationResult>();
        for (final var entry : namedNodes.entrySet()) {
            results.put(entry.getKey(), evaluate(entry.getValue(), ctx));
        }
        return Map.copyOf(results);
    }

    private Arguments applyNullStrategy(final Arguments args,
                                        final FunctionDefinition def,
                                        final NullHandlingStrategy strategy) {
        final var positional = new ArrayList<>(CoreUtils.emptyListIfNull(args.positional()));
        for (int i = 0; i < positional.size() && i < def.positionalParams().size(); i++) {
            if (Objects.isNull(positional.get(i))) {
                final var spec = def.positionalParams().get(i);
                final int idx = i;
                strategy.handleNull(spec, args)
                        .ifPresentOrElse(v -> positional.set(idx, v), () -> { /* propagate */ });
            }
        }

        final var named = new HashMap<>(CoreUtils.emptyMapIfNull(args.named()));
        for (final var entry : def.namedParams().entrySet()) {
            final var name = entry.getKey();
            final var spec = entry.getValue();
            if (named.containsKey(name) && Objects.isNull(named.get(name))) {
                strategy.handleNull(spec, args)
                        .ifPresentOrElse(v -> named.put(name, v),
                                () -> named.remove(name));
            }
        }

        return new Arguments(
                Collections.unmodifiableList(new ArrayList<>(positional)),
                Map.copyOf(named)
        );
    }

    private EvaluationResult buildFailureResult(final FunctionId fn,
                                                final List<EvaluationResult> children,
                                                final Map<String, EvaluationResult> namedChildren,
                                                final List<EvaluationError> errors,
                                                final long durationNanos) {
        return EvaluationResult.failure(
                fn,
                errors,
                children, namedChildren,
                new EvaluationTrace(false, false, durationNanos)
        );
    }

    private record ChildResult(
            boolean shortCircuited,
            Object shortCircuitValue,
            List<EvaluationResult> children
    ) {
    }
}
