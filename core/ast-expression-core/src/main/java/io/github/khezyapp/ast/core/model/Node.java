package io.github.khezyapp.ast.core.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a node in an Abstract Syntax Tree (AST) for expression evaluation.
 * <p>
 * A {@code Node} can be either a constant value (leaf) or a function invocation with
 * positional and/or named child nodes. This forms the fundamental building block
 * for defining evaluable expressions.
 * </p>
 *
 * <p>Use the factory methods {@link #constant(Object)} and {@link #function(FunctionId, Node...)}
 * to create nodes rather than calling the constructor directly.</p>
 */
public final class Node {
    private final FunctionId function;
    private final Object constant;
    private final List<Node> children;
    private final Map<String, Node> namedChildren;

    private Node(final FunctionId function,
                 final Object constant,
                 final List<Node> children,
                 final Map<String, Node> namedChildren) {
        this.function = Objects.requireNonNull(function);
        this.constant = constant;
        this.children = List.copyOf(children);
        this.namedChildren = Map.copyOf(namedChildren);
    }

    /**
     * Creates a constant node that wraps a literal value.
     *
     * @param value the constant value (may be {@code null})
     * @return a new constant node with {@link CoreFunctions#CONSTANT} as its function
     */
    public static Node constant(final Object value) {
        return new Node(CoreFunctions.CONSTANT, value, List.of(), Map.of());
    }

    /**
     * Creates a function node with positional child nodes (varargs).
     *
     * @param function the function identifier
     * @param children the positional child nodes (may be empty)
     * @return a new function node with the given children
     */
    public static Node function(final FunctionId function,
                                final Node... children) {
        return new Node(function, null, List.of(children), Map.of());
    }

    /**
     * Creates a function node with both positional and named child nodes.
     *
     * @param function      the function identifier
     * @param children      the positional child nodes
     * @param namedChildren the named child nodes (keyed by parameter name)
     * @return a new function node with the given children
     */
    public static Node function(final FunctionId function,
                                final List<Node> children,
                                final Map<String, Node> namedChildren) {
        return new Node(function, null, children, namedChildren);
    }

    public FunctionId function() {
        return function;
    }

    public Object constant() {
        return constant;
    }

    public List<Node> children() {
        return children;
    }

    public Map<String, Node> namedChildren() {
        return namedChildren;
    }

    /**
     * Returns whether this node represents a constant value.
     *
     * @return {@code true} if the function is {@link CoreFunctions#CONSTANT}
     */
    public boolean isConstant() {
        return CoreFunctions.CONSTANT.equals(function);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Node node)) {
            return false;
        }
        return function.equals(node.function)
            && Objects.equals(constant, node.constant)
            && children.equals(node.children)
            && namedChildren.equals(node.namedChildren);
    }

    @Override
    public int hashCode() {
        return Objects.hash(function, constant, children, namedChildren);
    }

    @Override
    public String toString() {
        if (isConstant()) {
            return "Node{" + constant + "}";
        }
        return "Node{" + function.value()
            + ", children=" + children.size()
            + ", named=" + namedChildren.size() + "}";
    }
}
