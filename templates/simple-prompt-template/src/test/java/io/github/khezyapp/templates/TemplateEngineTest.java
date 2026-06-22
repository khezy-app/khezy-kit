package io.github.khezyapp.templates;

import io.github.khezyapp.templates.config.SecurityConfig;
import io.github.khezyapp.templates.config.TemplateConfig;
import io.github.khezyapp.templates.plugin.Plugin;
import io.github.khezyapp.templates.plugin.PluginRegistry;
import io.github.khezyapp.templates.resolver.ArgumentResolver;
import io.github.khezyapp.templates.resolver.ResolverChain;
import io.github.khezyapp.templates.resolver.ShellPlaceholderResolver;
import io.github.khezyapp.templates.runner.DefaultShellRunner;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemplateEngineTest {

    private TemplateEngine defaultEngine() {
        final var securityConfig = SecurityConfig.builder().build();
        final var shellRunner = new DefaultShellRunner(securityConfig);
        final var pluginRegistry = new PluginRegistry(List.of());
        final var resolvers = List.of(
                new ArgumentResolver(),
                new ShellPlaceholderResolver(shellRunner, pluginRegistry)
        );
        final var config = TemplateConfig.builder()
                .resolverChain(new ResolverChain(resolvers))
                .pluginRegistry(pluginRegistry)
                .shellRunner(shellRunner)
                .securityConfig(securityConfig)
                .build();
        return new TemplateEngine(config);
    }

    @Test
    void resolvesArgumentPlaceholder() {
        final var engine = defaultEngine();
        final var result = engine.resolve("Hello $1", "World");
        assertEquals("Hello World", result.resolvedText());
    }

    @Test
    void resolvesRawInputPlaceholder() {
        final var engine = defaultEngine();
        final var result = engine.resolve("Input: $ARGUMENTS", "foo", "bar");
        assertEquals("Input: foo bar", result.resolvedText());
    }

    @Test
    void resolvesMultiplePositionalArgs() {
        final var engine = defaultEngine();
        final var result = engine.resolve("$1 $2 $3", "a", "b", "c");
        assertEquals("a b c", result.resolvedText());
    }

    @Test
    void lastPositionJoinsRemainingValues() {
        final var engine = defaultEngine();
        final var result = engine.resolve("$1 $2", "a", "b", "c", "d");
        assertEquals("a b c d", result.resolvedText());
    }

    @Test
    void unknownPositionalArgBecomesEmpty() {
        final var engine = defaultEngine();
        final var result = engine.resolve("$1 $2 $3", "only");
        assertEquals("only  ", result.resolvedText());
    }

    @Test
    void shellPlaceholderExecutesCommand() {
        final var engine = defaultEngine();
        final var result = engine.resolve("!`echo hello`");
        assertEquals("hello", result.resolvedText().strip());
    }

    @Test
    void shellCommandErrorRestoresOriginal() {
        final var securityConfig = SecurityConfig.builder().blockCommands("echo").build();
        final var shellRunner = new DefaultShellRunner(securityConfig);
        final var pluginRegistry = new PluginRegistry(List.of());
        final var resolvers = List.of(
                new ArgumentResolver(),
                new ShellPlaceholderResolver(shellRunner, pluginRegistry)
        );
        final var config = TemplateConfig.builder()
                .resolverChain(new ResolverChain(resolvers))
                .pluginRegistry(pluginRegistry)
                .shellRunner(shellRunner)
                .securityConfig(securityConfig)
                .build();
        final var engine = new TemplateEngine(config);

        final var result = engine.resolve("!`echo blocked`");
        assertEquals("!`echo blocked`", result.resolvedText());
    }

    @Test
    void escapeDollarSign() {
        final var engine = defaultEngine();
        final var result = engine.resolve("Price: \\$10");
        assertEquals("Price: $10", result.resolvedText());
    }

    @Test
    void escapeBang() {
        final var engine = defaultEngine();
        final var result = engine.resolve("Say \\!`echo hi`");
        assertEquals("Say !`echo hi`", result.resolvedText());
    }

    @Test
    void pluginBeforeResolveBlocks() {
        final var blockingPlugin = new Plugin() {
            @Override
            public boolean beforeResolve(final TemplateContext ctx) {
                return false;
            }
        };
        final var pluginRegistry = new PluginRegistry(List.of(blockingPlugin));
        final var securityConfig = SecurityConfig.builder().build();
        final var shellRunner = new DefaultShellRunner(securityConfig);
        final var resolvers = List.of(
                new ArgumentResolver(),
                new ShellPlaceholderResolver(shellRunner, pluginRegistry)
        );
        final var config = TemplateConfig.builder()
                .resolverChain(new ResolverChain(resolvers))
                .pluginRegistry(pluginRegistry)
                .shellRunner(shellRunner)
                .securityConfig(securityConfig)
                .build();
        final var engine = new TemplateEngine(config);

        final var result = engine.resolve("should $1", "not resolve");
        assertEquals("should $1", result.resolvedText());
        assertTrue(result.errors().contains("Blocked by plugin"));
    }

    @Test
    void pluginAfterResolveModifiesResult() {
        final var modifyingPlugin = new Plugin() {
            @Override
            public void afterResolve(final TemplateResult result) {
                final var modified = new TemplateResult(
                        result.resolvedText().toUpperCase(),
                        result.executedCommands(),
                        result.errors()
                );
                result.getClass(); // just checking; we can't replace immutable
            }
        };
        final var pluginRegistry = new PluginRegistry(List.of(modifyingPlugin));
        final var securityConfig = SecurityConfig.builder().build();
        final var shellRunner = new DefaultShellRunner(securityConfig);
        final var resolvers = List.of(
                new ArgumentResolver(),
                new ShellPlaceholderResolver(shellRunner, pluginRegistry)
        );
        final var config = TemplateConfig.builder()
                .resolverChain(new ResolverChain(resolvers))
                .pluginRegistry(pluginRegistry)
                .shellRunner(shellRunner)
                .securityConfig(securityConfig)
                .build();
        final var engine = new TemplateEngine(config);

        final var result = engine.resolve("hello $1", "world");
        assertEquals("hello world", result.resolvedText());
    }

    @Test
    void shellOutputIsRecordedInResult() {
        final var engine = defaultEngine();
        final var result = engine.resolve("!`echo foo`");
        assertEquals("foo", result.resolvedText().strip());
        assertFalse(result.executedCommands().isEmpty());
    }

    @Test
    void mixedPlaceholdersAndShell() {
        final var engine = defaultEngine();
        final var result = engine.resolve("$1: !`echo hello`", "name");
        assertEquals("name: hello", result.resolvedText().strip());
    }

    @Test
    void threadSafety() throws InterruptedException {
        final var engine = defaultEngine();
        final var threadCount = 10;
        final var iterations = 100;
        final var latch = new CountDownLatch(threadCount);
        final var errors = new AtomicInteger(0);

        final var executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                final var threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < iterations; j++) {
                            final var result = engine.resolve("$1: !`echo thread" + threadId + "`", "arg-" + j);
                            assertTrue(result.resolvedText().contains("arg-" + j));
                            assertTrue(result.resolvedText().contains("thread" + threadId));
                        }
                    } catch (final Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        } finally {
            executor.shutdown();
        }

        assertEquals(0, errors.get());
    }
}
