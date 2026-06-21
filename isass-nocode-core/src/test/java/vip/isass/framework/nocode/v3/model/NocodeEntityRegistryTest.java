package vip.isass.framework.nocode.v3.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeEntityRegistryTest {

    @Test
    void registersAndFindsEntityDefinitions() {
        NocodeEntityDefinition attachment = definition("attachment");
        NocodeEntityRegistry registry = new NocodeEntityRegistry(List.of(attachment));

        assertThat(registry.find("attachment")).contains(attachment);
        assertThat(registry.get("attachment")).isSameAs(attachment);
        assertThat(registry.definitions()).containsExactly(attachment);
    }

    @Test
    void rejectsDuplicateEntityNames() {
        NocodeEntityDefinition first = definition("attachment");
        NocodeEntityDefinition second = definition("attachment");

        assertThatThrownBy(() -> new NocodeEntityRegistry(List.of(first, second)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate entity");
    }

    @Test
    void canRegisterAdditionalDefinitionExplicitly() {
        NocodeEntityRegistry registry = new NocodeEntityRegistry();
        NocodeEntityDefinition attachment = definition("attachment");

        registry.register(attachment);

        assertThat(registry.find("attachment")).contains(attachment);
    }

    @Test
    void canLoadDefinitionsFromJavaServiceLoader() {
        NocodeEntityRegistry registry = NocodeEntityRegistry.fromServiceLoader();

        assertThat(registry.find("spiAttachment"))
                .map(NocodeEntityDefinition::displayName)
                .contains("SPI附件");
    }

    private static NocodeEntityDefinition definition(String entityName) {
        return NocodeEntityDefinition.builder(entityName, Object.class)
                .field(NocodeFieldDefinition.builder("id", Long.class).idField(true).build())
                .build();
    }
}
