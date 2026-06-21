package vip.isass.framework.nocode.v3.model;

import java.util.Objects;

/**
 * Framework-neutral field metadata used by nocode v3.
 */
public record NocodeFieldDefinition(
        String fieldName,
        Class<?> fieldType,
        String displayName,
        String columnName,
        boolean idField,
        boolean queryable,
        boolean sortable,
        boolean clientWritable,
        NocodeFieldAutoFill autoFill
) {

    public NocodeFieldDefinition {
        fieldName = requireText(fieldName, "fieldName");
        fieldType = fieldType == null ? Object.class : fieldType;
        displayName = normalize(displayName, fieldName);
        columnName = normalize(columnName, fieldName);
        autoFill = autoFill == null ? NocodeFieldAutoFill.NONE : autoFill;
    }

    public static Builder builder(String fieldName, Class<?> fieldType) {
        return new Builder(fieldName, fieldType);
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

        private final String fieldName;
        private final Class<?> fieldType;
        private String displayName;
        private String columnName;
        private boolean idField;
        private boolean queryable = true;
        private boolean sortable;
        private boolean clientWritable = true;
        private NocodeFieldAutoFill autoFill = NocodeFieldAutoFill.NONE;

        private Builder(String fieldName, Class<?> fieldType) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public Builder idField(boolean idField) {
            this.idField = idField;
            return this;
        }

        public Builder queryable(boolean queryable) {
            this.queryable = queryable;
            return this;
        }

        public Builder sortable(boolean sortable) {
            this.sortable = sortable;
            return this;
        }

        public Builder clientWritable(boolean clientWritable) {
            this.clientWritable = clientWritable;
            return this;
        }

        public Builder autoFill(NocodeFieldAutoFill autoFill) {
            this.autoFill = autoFill;
            return this;
        }

        public NocodeFieldDefinition build() {
            return new NocodeFieldDefinition(
                    fieldName,
                    fieldType,
                    displayName,
                    columnName,
                    idField,
                    queryable,
                    sortable,
                    clientWritable,
                    autoFill
            );
        }
    }
}
