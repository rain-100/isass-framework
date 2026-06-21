package vip.isass.framework.nocode.v3.model;

import java.util.List;

public class TestNocodeEntityDefinitionProvider implements NocodeEntityDefinitionProvider {

    @Override
    public List<NocodeEntityDefinition> definitions() {
        return List.of(NocodeEntityDefinition.builder("spiAttachment", Object.class)
                .displayName("SPI附件")
                .field(NocodeFieldDefinition.builder("id", Long.class).idField(true).build())
                .build());
    }
}
