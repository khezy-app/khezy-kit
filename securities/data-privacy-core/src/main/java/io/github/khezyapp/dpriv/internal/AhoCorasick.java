package io.github.khezyapp.dpriv.internal;

import java.util.*;

/**
 * A literal multi-pattern string matcher (Aho-Corasick) with failure links and longest-match
 * emission (design §7.3). It powers the streaming redactor in Task 09 and the in-memory
 * {@code Redactor}. For every end position it emits the longest pattern ending there and keeps
 * scanning (it never skips). Matches are therefore reported in ascending end-position order.
 *
 * <p>This is an internal implementation detail of the redaction engine, not part of the public
 * API contract.</p>
 */
public final class AhoCorasick {

    private static final class Node {
        private final Map<Character, Node> next = new HashMap<>();
        private Node fail;
        private Output best;
    }

    private record Output(String token, String entityType) { }

    private final Node root;

    private AhoCorasick(final Node root) {
        this.root = root;
    }

    /**
     * Builds an automaton over {@code maskEntities} ({@code entityType → tokens}).
     *
     * @param maskEntities the entity type to token list mapping; must be non-empty
     * @return the compiled automaton
     * @throws IllegalArgumentException if {@code maskEntities} is empty or any token text is empty
     */
    public static AhoCorasick compile(final Map<String, List<String>> maskEntities) {
        if (maskEntities.isEmpty()) {
            throw new IllegalArgumentException("maskEntities must not be empty");
        }
        final var root = new Node();
        for (final var entry : maskEntities.entrySet()) {
            final var entityType = entry.getKey();
            for (final var token : entry.getValue()) {
                if (token.isBlank()) {
                    throw new IllegalArgumentException("token text must not be blank");
                }
                insert(root, token, entityType);
            }
        }
        buildFailureLinks(root);
        return new AhoCorasick(root);
    }

    /**
     * Scans {@code input}, resetting to the root at the start of the call, and reports the longest
     * match ending at each position to {@code visitor}.
     *
     * @param input   the text to scan
     * @param visitor the callback invoked for each emitted match
     */
    public void scan(final CharSequence input,
                     final MatchVisitor visitor) {
        var cur = root;
        for (var i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            while (cur != root && !cur.next.containsKey(c)) {
                cur = cur.fail;
            }
            final var next = cur.next.get(c);
            cur = Objects.nonNull(next) ? next : root;
            final var best = cur.best;
            if (Objects.nonNull(best)) {
                visitor.match(i - best.token().length() + 1, i + 1, best.token(), best.entityType());
            }
        }
    }

    private static void insert(final Node root,
                               final String token,
                               final String entityType) {
        var cur = root;
        for (var i = 0; i < token.length(); i++) {
            cur = cur.next.computeIfAbsent(token.charAt(i), key -> new Node());
        }
        final var existing = cur.best;
        if (Objects.isNull(existing) || entityType.compareTo(existing.entityType()) < 0) {
            cur.best = new Output(token, entityType);
        }
    }

    private static void buildFailureLinks(final Node root) {
        final var queue = new ArrayDeque<Node>();
        for (final var child : root.next.values()) {
            child.fail = root;
            queue.add(child);
        }
        while (!queue.isEmpty()) {
            final var node = queue.poll();
            for (final var entry : node.next.entrySet()) {
                final var child = entry.getValue();
                var fail = node.fail;
                while (Objects.nonNull(fail) && !fail.next.containsKey(entry.getKey())) {
                    fail = fail.fail;
                }
                child.fail = Objects.isNull(fail) ? root : fail.next.get(entry.getKey());
                if (Objects.isNull(child.best)) {
                    child.best = child.fail.best;
                }
                queue.add(child);
            }
        }
    }

    /**
     * Receives matches emitted by {@link AhoCorasick#scan(CharSequence, MatchVisitor)}.
     */
    @FunctionalInterface
    public interface MatchVisitor {

        /**
         * Called with a matched token's span.
         *
         * @param start      the inclusive start index in the scanned input
         * @param end        the exclusive end index in the scanned input
         * @param token      the matched token text
         * @param entityType the entity type the token belongs to
         */
        void match(int start, int end, String token, String entityType);
    }
}
