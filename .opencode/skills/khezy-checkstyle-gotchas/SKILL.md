---
name: khezy-checkstyle-gotchas
description: "Checkstyle rules unique to khezy-kit that cause recurring violations. Load whens writing or modifying any Java file."
---

Use this skill when writing Java code in khezy-kit to avoid Checkstyle violations that cost time in review loops.

## LeftCurly — opening brace must be the last character on its line

Checkstyle's `LeftCurly` (option `eol`) rejects any code after `{` on the same line:

```java
// VIOLATION — `{` immediately followed by code
public String name() { return name; }
if (x) { return y; }
try { doStuff(); } catch (final Exception e) { log(e); }
```

**Fix**: break after `{`:
```java
public String name() {
    return name;
}
if (x) {
    return y;
}
try {
    doStuff();
} catch (final Exception e) {
    log(e);
}
```

This applies everywhere: accessors, single-statement blocks, empty method bodies with `{ }`, even simple record methods.

## WhitespaceAround — empty blocks: `{ }` not `{}`

```java
// VIOLATION
private Foo() {}
// FIX
private Foo() { }
```

Applies to: private constructors in utility classes, empty `case` bodies, empty `catch` blocks, no-op lambdas `() -> { }`.

## NoWhitespaceAfter — array initializer `{` must not be followed by whitespace

`NoWhitespaceAfter` is enabled with default tokens, which include `ARRAY_INITIALIZER`:

```java
// VIOLATION
new String[] { "a", "b" }
new TrustManager[] { TRUST_ALL }

// FIX (brace touches the brackets)
new String[]{"a", "b"}
new TrustManager[]{TRUST_ALL}
```

This is the inverse of LeftCurly: array initializers are the one place a `{` must hug its preceding token.

## MethodLength — max 150 lines

Methods exceeding 150 lines are rejected. Split long methods into private helpers:

```java
// VIOLATION (163 lines)
public void withBuiltins() { ... }

// FIX
private static void registerBuiltins() {
    registerCoreBuiltins(r);
    registerStringBuiltins(r);
}
private static void registerCoreBuiltins(FunctionRegistry r) { ... }
private static void registerStringBuiltins(FunctionRegistry r) { ... }
```

## ParameterNumber — max 7 parameters

`ParameterNumber` (max 7) rejects constructors/methods with 8+ parameters (e.g. a 9-component
record's manual constructor). `@SuppressWarnings("checkstyle:ParameterNumber")` works because both
`SuppressWarningsFilter` and `SuppressWarningsHolder` are enabled:

```java
@SuppressWarnings("checkstyle:ParameterNumber")
public HttpRequestSpec(final String baseUrl, final Map<String, String> headers,
        final long timeout, final boolean skipSsl, final List<Operation> ops, ...) {
    this(baseUrl, headers, timeout, skipSsl, ops, ..., null);
}
```

## UnusedImports — enforced for test code too

Every imported class must be referenced. For rarely-used types, use fully qualified names instead of adding an import:

```java
// Don't add: import io.github.khezyapp.ast.core.model.FunctionId;
// Instead, use inline:
io.github.khezyapp.ast.core.model.FunctionId.of("custom:fn")
```

## FinalParameters — all method parameters must be final

```java
// VIOLATION
public void foo(String name) { }

// FIX
public void foo(final String name) { }
```

This applies to: constructors, methods, lambdas (lambda params should use `final var`).

## FinalLocalVariable — all local variables must be final

Use `final var` universally:
```java
// VIOLATION
List<String> list = new ArrayList<>();
String result = compute(list);

// FIX
final var list = new ArrayList<String>();
final var result = compute(list);
```

Exception: loop variables in for-each (Checkstyle does not require `final` here).
