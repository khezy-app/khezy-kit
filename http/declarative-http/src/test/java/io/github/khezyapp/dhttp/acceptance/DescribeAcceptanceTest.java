package io.github.khezyapp.dhttp.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.khezyapp.dhttp.action.ActionRegistry;
import io.github.khezyapp.dhttp.config.DeclarativeHttp;
import io.github.khezyapp.dhttp.config.DeclarativeHttpConfig;
import io.github.khezyapp.dhttp.engine.OptionItem;
import io.github.khezyapp.dhttp.engine.OptionPage;
import io.github.khezyapp.dhttp.engine.OutputRecord;
import io.github.khezyapp.dhttp.json.jackson3.JacksonJsonMapper;
import io.github.khezyapp.dhttp.plan.RequestContext;
import io.github.khezyapp.dhttp.spec.HttpMethod;
import io.github.khezyapp.dhttp.spec.HttpRequestSpec;
import io.github.khezyapp.dhttp.spec.Operation;
import io.github.khezyapp.dhttp.spec.Output;
import io.github.khezyapp.dhttp.spec.PostReceive;
import io.github.khezyapp.dhttp.spec.RequestShape;
import io.github.khezyapp.dhttp.spec.Route;
import io.github.khezyapp.dhttp.spec.SecurityPolicy;
import io.github.khezyapp.dhttp.transport.HttpResult;
import io.github.khezyapp.dhttp.transport.testutil.FakeTransport;

import java.util.List;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * §9 acceptance item 5: {@code describe(...)} returns shaped {@link OptionItem}s for a dropdown,
 * driven by a custom option-shaping action registered through the config builder.
 */
class DescribeAcceptanceTest {

    private static final JacksonJsonMapper JSON = JacksonJsonMapper.INSTANCE;

    @Test
    @DisplayName("Item 5: describe returns shaped OptionItems for a dropdown")
    @SuppressWarnings("unchecked")
    void describeReturnsShapedOptions() {
        final var registry = ActionRegistry.withBuiltins().register("loadContacts",
                (descriptor, evaluator) -> (records, response) -> {
                    final var data = JSON.read(response.bodyString(), Map.class);
                    final var items = (List<Map<String, Object>>) data.get("data");
                    return items.stream()
                            .map(item -> OutputRecord.ofJson(Map.of(
                                    "name", item.get("name"),
                                    "value", item.get("id"),
                                    "description", item.get("description"),
                                    "icon", item.get("icon"),
                                    "group", item.get("group"),
                                    "disabled", item.get("disabled"))))
                            .toList();
                });
        final var transport = new FakeTransport(HttpResult.of(200, JSON.write(Map.of("data",
                List.of(Map.of("id", 1, "name", "SOK", "description", "Project manager",
                                "icon", "lucide:user", "group", "Team", "disabled", false),
                        Map.of("id", 2, "name", "VISAL", "description", "Developer",
                                "icon", "lucide:code", "group", "Team", "disabled", true))))));
        final var http = DeclarativeHttp.create(DeclarativeHttpConfig.builder()
                .transport(transport)
                .keyProvider(DescribeAcceptanceTest::newKey)
                .registry(registry)
                .build());
        final var shape = new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("contact.list", new Route(shape, List.of(),
                new Output(50, List.of(new PostReceive.CustomPostReceive("loadContacts", Map.of()))),
                null, List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        final var page = http.describe(spec, new RequestContext("contact.list", Map.of()),
                "loadContacts");

        assertEquals(new OptionPage(List.of(
                new OptionItem("SOK", "1", "Project manager", "lucide:user", "Team", false),
                new OptionItem("VISAL", "2", "Developer", "lucide:code", "Team", true)),
                false, null), page);
        assertEquals(Map.of(), page.nextParameters());
        assertEquals(1, transport.callCount());
    }

    private static SecretKey newKey() {
        try {
            final var generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            return generator.generateKey();
        } catch (final java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("AES unavailable", e);
        }
    }
}
