package io.github.khezyapp.ast.core.model;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for {@link Node} AST model class.
 * <p>
 * Covers constant and function factory methods, equality, string representation,
 * and validation of null constraints.
 * </p>
 */
@DisplayName("Node")
class NodeTest {

    @Nested
    @DisplayName("constant factory")
    class ConstantFactoryTests {

        @Test
        @DisplayName("creates constant node with correct function and value")
        void createsConstantNodeWithCorrectFunctionAndValue() {
            final var node = Node.constant(42);

            assertAll(
                () -> assertEquals(CoreFunctions.CONSTANT, node.function()),
                () -> assertEquals(42, node.constant()),
                () -> assertTrue(node.isConstant()),
                () -> assertEquals(0, node.children().size()),
                () -> assertTrue(node.namedChildren().isEmpty())
            );
        }

        @Test
        @DisplayName("accepts null constant value")
        void acceptsNullConstantValue() {
            final var node = Node.constant(null);

            assertAll(
                () -> assertTrue(node.isConstant()),
                () -> assertNull(node.constant())
            );
        }

        @Test
        @DisplayName("accepts string constant")
        void acceptsStringConstant() {
            final var node = Node.constant("hello");
            assertEquals("hello", node.constant());
        }
    }

    @Nested
    @DisplayName("function factory (varargs)")
    class FunctionFactoryVarargsTests {

        @Test
        @DisplayName("creates function node with children")
        void createsFunctionNodeWithChildren() {
            final var child = Node.constant(1);
            final var node = Node.function(CoreFunctions.ADD, child);

            assertAll(
                () -> assertEquals(CoreFunctions.ADD, node.function()),
                () -> assertNull(node.constant()),
                () -> assertFalse(node.isConstant()),
                () -> assertEquals(1, node.children().size()),
                () -> assertTrue(node.namedChildren().isEmpty())
            );
        }

        @Test
        @DisplayName("creates function node with multiple children")
        void createsFunctionNodeWithMultipleChildren() {
            final var a = Node.constant(1);
            final var b = Node.constant(2);
            final var c = Node.constant(3);
            final var node = Node.function(CoreFunctions.ADD, a, b, c);

            assertEquals(3, node.children().size());
        }

        @Test
        @DisplayName("creates function node with no children")
        void createsFunctionNodeWithNoChildren() {
            final var node = Node.function(CoreFunctions.CONSTANT);
            assertEquals(0, node.children().size());
        }
    }

    @Nested
    @DisplayName("function factory (list + named)")
    class FunctionFactoryListTests {

        @Test
        @DisplayName("creates function node with children and named children")
        void createsFunctionNodeWithChildrenAndNamedChildren() {
            final var child = Node.constant(1);
            final var named = Map.of("x", Node.constant(10));
            final var node = Node.function(CoreFunctions.ADD, List.of(child), named);

            assertAll(
                () -> assertEquals(CoreFunctions.ADD, node.function()),
                () -> assertEquals(1, node.children().size()),
                () -> assertEquals(1, node.namedChildren().size()),
                () -> assertEquals(10, node.namedChildren().get("x").constant())
            );
        }

        @Test
        @DisplayName("defensive copy prevents external mutation")
        void defensiveCopyPreventsExternalMutation() {
            final var children = new java.util.ArrayList<>(List.of(Node.constant(1)));
            final var named = new java.util.HashMap<>(Map.of("a", Node.constant(2)));
            final var node = Node.function(CoreFunctions.ADD, children, named);

            children.clear();
            named.clear();

            assertEquals(1, node.children().size());
            assertEquals(1, node.namedChildren().size());
        }
    }

    @Nested
    @DisplayName("equality")
    class EqualityTests {

        @Test
        @DisplayName("equal nodes are equal")
        void equalNodesAreEqual() {
            final var a = Node.constant(42);
            final var b = Node.constant(42);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("different constant values are not equal")
        void differentConstantValuesAreNotEqual() {
            final var a = Node.constant(1);
            final var b = Node.constant(2);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("different functions are not equal")
        void differentFunctionsAreNotEqual() {
            final var a = Node.function(CoreFunctions.ADD, Node.constant(1));
            final var b = Node.function(CoreFunctions.SUBTRACT, Node.constant(1));

            assertNotEquals(a, b);
        }
    }

    @Nested
    @DisplayName("string representation")
    class ToStringTests {

        @Test
        @DisplayName("constant node toString contains value")
        void constantNodeToStringContainsValue() {
            final var node = Node.constant("test");
            assertEquals("Node{test}", node.toString());
        }

        @Test
        @DisplayName("function node toString contains function name and child count")
        void functionNodeToStringContainsFunctionNameAndChildCount() {
            final var node = Node.function(CoreFunctions.ADD, Node.constant(1), Node.constant(2));
            final var str = node.toString();

            assertAll(
                () -> assertTrue(str.contains(CoreFunctions.ADD.value())),
                () -> assertTrue(str.contains("children=2"))
            );
        }
    }

    @Nested
    @DisplayName("validation")
    class ValidationTests {

        @Test
        @DisplayName("throws on null function in function factory")
        void throwsOnNullFunctionInFunctionFactory() {
            assertThrows(NullPointerException.class,
                () -> Node.function(null, Node.constant(1)));
        }

        @Test
        @DisplayName("throws on null function in list factory")
        void throwsOnNullFunctionInListFactory() {
            assertThrows(NullPointerException.class,
                () -> Node.function(null, List.of(), Map.of()));
        }
    }
}
