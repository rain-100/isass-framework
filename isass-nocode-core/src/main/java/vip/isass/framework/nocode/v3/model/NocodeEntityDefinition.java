package vip.isass.framework.nocode.v3.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Framework-neutral entity metadata used by nocode v3 access and ORM adapters.
 */
public record NocodeEntityDefinition(
        String entityName,
        Class<?> entityType,
        String displayName,
        String tableName,
        List<NocodeFieldDefinition> fields,
        List<NocodeEntityRelation> relations,
        Map<String, NocodeFieldDefinition> fieldMap
) {

    public NocodeEntityDefinition {
        entityName = requireText(entityName, "entityName");
        entityType = entityType == null ? Object.class : entityType;
        displayName = normalize(displayName, entityName);
        tableName = normalize(tableName, entityName);
        List<NocodeFieldDefinition> fieldCopy = fields == null ? List.of() : List.copyOf(fields);
        LinkedHashMap<String, NocodeFieldDefinition> map = new LinkedHashMap<>();
        for (NocodeFieldDefinition field : fieldCopy) {
            NocodeFieldDefinition previous = map.put(field.fieldName(), field);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate field: " + field.fieldName());
            }
        }
        fields = Collections.unmodifiableList(fieldCopy);
        relations = relations == null ? List.of() : Collections.unmodifiableList(List.copyOf(relations));
        fieldMap = Collections.unmodifiableMap(map);
    }

    public static Builder builder(String entityName, Class<?> entityType) {
        return new Builder(entityName, entityType);
    }

    public Optional<NocodeFieldDefinition> field(String fieldName) {
        return Optional.ofNullable(fieldMap.get(fieldName));
    }

    public Optional<NocodeFieldDefinition> idField() {
        return fields.stream().filter(NocodeFieldDefinition::idField).findFirst();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static final class Builder {

        private final String entityName;
        private final Class<?> entityType;
        private String displayName;
        private String tableName;
        private final List<NocodeFieldDefinition> fields = new ArrayList<>();
        private final List<NocodeEntityRelation> relations = new ArrayList<>();

        private Builder(String entityName, Class<?> entityType) {
            this.entityName = entityName;
            this.entityType = entityType;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder field(NocodeFieldDefinition field) {
            fields.add(Objects.requireNonNull(field, "field"));
            return this;
        }

        public Builder relation(NocodeEntityRelation relation) {
            relations.add(Objects.requireNonNull(relation, "relation"));
            return this;
        }

        public NocodeEntityDefinition build() {
            return new NocodeEntityDefinition(entityName, entityType, displayName, tableName, fields, relations, Map.of());
        }
    }
}
