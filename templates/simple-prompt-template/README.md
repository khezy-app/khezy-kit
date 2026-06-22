# Simple Prompt Template

A lightweight Java library for resolving prompt templates with shell-like argument syntax and
inline shell command execution. Designed for building AI agent prompts that need dynamic
placeholder substitution and controlled shell integration.

## Motivation

AI agent prompts often need to reference user-provided arguments or capture output from shell
commands at runtime. Doing this safely and consistently across different prompt definitions
requires a structured approach — raw string concatenation leads to injection risks and
maintenance headaches.

This library provides a **resolver chain** pattern that processes prompt templates through
multiple stages:

1. **Argument resolution** — substitute `$1`, `$2`, `$ARGUMENTS` with positional values
2. **Shell placeholder resolution** — execute `` !`command` `` snippets and inline their output
3. **Escaping** — backslash-escaped `\$` and `\!` pass through literally

A **plugin SPI** lets you intercept every stage of resolution, making it possible to add
audit logging, content filtering, or security checks without modifying the core engine.

## Installation

**Maven**

```xml
<dependency>
    <groupId>io.github.khezyapp</groupId>
    <artifactId>simple-prompt-template</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle**

```groovy
implementation 'io.github.khezyapp:simple-prompt-template:1.0.0'
```

## Quick start

```java
import io.github.khezyapp.templates.TemplateEngine;
import io.github.khezyapp.templates.config.SecurityConfig;
import io.github.khezyapp.templates.config.TemplateConfig;
import io.github.khezyapp.templates.plugin.PluginRegistry;
import io.github.khezyapp.templates.resolver.ArgumentResolver;
import io.github.khezyapp.templates.resolver.ResolverChain;
import io.github.khezyapp.templates.resolver.ShellPlaceholderResolver;
import io.github.khezyapp.templates.runner.DefaultShellRunner;
import java.util.List;

var securityConfig = SecurityConfig.builder().build();
var shellRunner = new DefaultShellRunner(securityConfig);
var pluginRegistry = new PluginRegistry(List.of());

var resolvers = List.of(
        new ArgumentResolver(),
        new ShellPlaceholderResolver(shellRunner, pluginRegistry)
);

var config = TemplateConfig.builder()
        .resolverChain(new ResolverChain(resolvers))
        .pluginRegistry(pluginRegistry)
        .shellRunner(shellRunner)
        .securityConfig(securityConfig)
        .build();

var engine = new TemplateEngine(config);

// Positional arguments
var r1 = engine.resolve("Hello $1!", "World");
// r1.resolvedText() → "Hello World!"

// Shell command output
var r2 = engine.resolve("Today is !`date +%A`");
// r2.resolvedText() → "Today is Monday"

// Mixed argument + shell
var r3 = engine.resolve("$1: !`echo hi`", "greeting");
// r3.resolvedText() → "greeting: hi"

// Escaping (backslash prevents substitution)
var r4 = engine.resolve("Price: \\$10");
// r4.resolvedText() → "Price: $10"
```

## How it works

### Architecture

The library is organized around four core concepts:

| Component | Role |
|---|---|
| `TemplateEngine` | Main entry point. Takes a `TemplateConfig`, dispatches resolution through the chain, fires plugin lifecycle hooks. |
| `TemplateConfig` | Aggregates the `ResolverChain`, `PluginRegistry`, `ShellRunner`, and `SecurityConfig`. All fields are required. |
| `TemplateContext` | Immutable carrier of the raw input string and the positional argument list. |
| `TemplateResult` | Immutable result with three accessors: `resolvedText()`, `executedCommands()`, `errors()`. |

### Resolver chain

Resolvers implement `PlaceholderResolver` and are run in sequence. The library ships with two
built-in resolvers:

- **`ArgumentResolver`** — matches `$1`, `$2`, ..., `$ARGUMENTS`. The highest-numbered positional
  slurps all remaining arguments (e.g., `$2` in a 4-arg call receives args 2, 3, and 4 joined by
  spaces).
- **`ShellPlaceholderResolver`** — matches `` !`command` `` placeholders. Each command is executed
  via `ProcessBuilder("sh", "-c", command)` and its stdout replaces the placeholder. Thread-safe
  via `ThreadLocal` state.

Before resolution, `EscapeUtils.escape()` replaces `\$` and `\!` with private-use Unicode
codepoints so they survive the resolver passes. After resolution, `unescape()` restores them.

### Security

`SecurityConfig` provides a builder with a built-in blocklist covering destructive or
network-facing commands:

- `rm -rf /`, `rm -rf /*`
- `mkfs`, `dd if=`, `:(){` (fork bomb)
- `> /dev/sda`, `mv /`
- `wget`, `curl`
- `chmod -R 000`

You can extend the blocklist with `SecurityConfig.builder().blockCommands("pattern", ...).build()`.
A configurable timeout (default 30 seconds) prevents runaway commands.

## Extensibility

### Plugins

The `Plugin` interface exposes five lifecycle hooks, each with a default no-op implementation:

| Hook | Signature | Veto? |
|---|---|---|
| `beforeResolve` | `boolean beforeResolve(TemplateContext)` | Yes — return `false` to abort resolution |
| `afterResolve` | `void afterResolve(TemplateResult)` | No |
| `beforeShellRun` | `boolean beforeShellRun(String command)` | Yes — return `false` to skip the command |
| `afterShellRun` | `String afterShellRun(String command, String output)` | No — return modified output |
| `onResolveError` | `void onResolveError(String placeholder, Exception error)` | No |

Register plugins via `PluginRegistry`:

```java
var plugin = new Plugin() {
    @Override
    public boolean beforeResolve(TemplateContext ctx) {
        System.out.println("Resolving: " + ctx.rawInput());
        return true;
    }

    @Override
    public String afterShellRun(String command, String output) {
        return output.strip();
    }
};

var pluginRegistry = new PluginRegistry(List.of(plugin));
```

### Custom resolvers

Implement `PlaceholderResolver` to add custom syntax (e.g., environment variables, date
formatting). Add your resolver to the `ResolverChain` — it will run in the declared order.

### Custom shell runner

Implement `ShellRunner` to replace the default `ProcessBuilder`-based execution with, for
example, a Docker container runner or a mock for testing.

## Thread safety

`TemplateEngine`, `ResolverChain`, and all resolvers are designed to be shared across threads.
`ShellPlaceholderResolver` uses `ThreadLocal` state to keep per-thread command/error lists
isolated. The test suite verifies correctness under 10 concurrent threads (100 iterations each).

## Requirements

- Java 17+
- No external dependencies

## License

Part of the [KHEZY](https://github.com/khezyapp) ecosystem.
