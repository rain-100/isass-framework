package vip.isass.framework.nocode.v3.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Registry for nocode v3 entity metadata.
 */
public class NocodeEntityRegistry {

    private final Map<String, NocodeEntityDefinition> definitions = new LinkedHashMap<>();

    public NocodeEntityRegistry() {
    }

    public NocodeEntityRegistry(Collection<NocodeEntityDefinition> definitions) {
        if (definitions != null) {
            definitions.forEach(this::register);
        }
    }

    public static NocodeEntityRegistry fromServiceLoader() {
        return fromServiceLoader(Thread.currentThread().getContextClassLoader());
    }

    public static NocodeEntityRegistry fromServiceLoader(ClassLoader classLoader) {
        ServiceLoader<NocodeEntityDefinitionProvider> loader = classLoader == null
                ? ServiceLoader.load(NocodeEntityDefinitionProvider.class)
                : ServiceLoader.load(NocodeEntityDefinitionProvider.class, classLoader);
        NocodeEntityRegistry registry = new NocodeEntityRegistry();
        for (NocodeEntityDefinitionProvider provider : loader) {
            Collection<NocodeEntityDefinition> definitions = provider.definitions();
            if (definitions != null) {
                definitions.forEach(registry::register);
            }
        }
        return registry;
    }

    public synchronized void register(NocodeEntityDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        NocodeEntityDefinition previous = definitions.put(definition.entityName(), definition);
        if (previous != null) {
            definitions.put(previous.entityName(), previous);
            throw new IllegalArgumentException("Duplicate entity: " + definition.entityName());
        }
    }

    public synchronized Optional<NocodeEntityDefinition> find(String entityName) {
        return Optional.ofNullable(definitions.get(entityName));
    }

    public synchronized NocodeEntityDefinition get(String entityName) {
        return find(entityName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity: " + entityName));
    }

    public synchronized List<NocodeEntityDefinition> definitions() {
        return List.copyOf(definitions.values());
    }
}
