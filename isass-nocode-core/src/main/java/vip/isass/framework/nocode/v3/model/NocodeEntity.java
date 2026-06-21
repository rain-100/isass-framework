package vip.isass.framework.nocode.v3.model;

import java.util.List;

/**
 * Marker contract for custom nocode v3 entities.
 */
public interface NocodeEntity {

    String nocodeEntityName();

    default String nocodeDisplayName() {
        return nocodeEntityName();
    }

    default String nocodeTableName() {
        return nocodeEntityName();
    }

    List<NocodeFieldDefinition> nocodeFields();

    default NocodeEntityDefinition nocodeDefinition() {
        NocodeEntityDefinition.Builder builder = NocodeEntityDefinition.builder(nocodeEntityName(), getClass())
                .displayName(nocodeDisplayName())
                .tableName(nocodeTableName());
        for (NocodeFieldDefinition field : nocodeFields()) {
            builder.field(field);
        }
        return builder.build();
    }
}
