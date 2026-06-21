package vip.isass.framework.nocode.v3.model;

import java.util.Collection;

/**
 * Java SPI provider for nocode v3 entity metadata.
 */
public interface NocodeEntityDefinitionProvider {

    Collection<NocodeEntityDefinition> definitions();
}
