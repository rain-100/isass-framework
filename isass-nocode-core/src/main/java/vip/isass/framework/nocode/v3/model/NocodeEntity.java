package vip.isass.framework.nocode.v3.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Marker contract for custom nocode v3 entities.
 */
public interface NocodeEntity {

    DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    @SuppressWarnings("unchecked")
    default <E extends NocodeEntity> String formatTimestamp(Function<E, Long> gettingMapper) {
        Long timestamp = gettingMapper.apply((E) this);
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .format(TIMESTAMP_FORMATTER);
    }

    @SuppressWarnings("unchecked")
    default <E extends NocodeEntity> E setupTimestamp(String dateTime, BiConsumer<E, Long> settingMapper) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, TIMESTAMP_FORMATTER);
        long timestamp = localDateTime.atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        E entity = (E) this;
        settingMapper.accept(entity, timestamp);
        return entity;
    }
}
