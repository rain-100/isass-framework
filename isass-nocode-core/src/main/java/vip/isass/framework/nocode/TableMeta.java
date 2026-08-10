// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TableMeta {

    private String tableName;
    private IdType idType;
    private Class<?> keyType;
    private String keyPropertyName;
    private String keyColumnName;
    private String logicDeleteField;
    private String versionField;
    private String tenantIdField;
    private String parentIdField;
    private final Map<String, FieldFill> fillFields = new HashMap<>();
    private final Map<String, String> columnMappings = new HashMap<>();
    private final Set<String> associationFields = new LinkedHashSet<>();

    public String tableName() { return tableName; }
    public TableMeta tableName(String v) { this.tableName = v; return this; }

    public IdType idType() { return idType; }
    public TableMeta idType(IdType v) { this.idType = v; return this; }

    public Class<?> keyType() { return keyType; }
    public TableMeta keyType(Class<?> v) { this.keyType = v; return this; }

    public String keyPropertyName() { return keyPropertyName; }
    public TableMeta keyPropertyName(String v) { this.keyPropertyName = v; return this; }

    public String keyColumnName() { return keyColumnName; }
    public TableMeta keyColumnName(String v) { this.keyColumnName = v; return this; }

    public String logicDeleteField() { return logicDeleteField; }
    public TableMeta logicDeleteField(String v) { this.logicDeleteField = v; return this; }

    public String versionField() { return versionField; }
    public TableMeta versionField(String v) { this.versionField = v; return this; }

    public String tenantIdField() { return tenantIdField; }
    public TableMeta tenantIdField(String v) { this.tenantIdField = v; return this; }

    public String parentIdField() { return parentIdField; }
    public TableMeta parentIdField(String v) { this.parentIdField = v; return this; }

    public Map<String, FieldFill> fillFields() { return fillFields; }
    public TableMeta fillFields(Map<String, FieldFill> v) { fillFields.putAll(v); return this; }

    public Map<String, String> columnMappings() { return columnMappings; }
    public TableMeta column(String propertyName, String columnName) {
        columnMappings.put(propertyName, columnName);
        return this;
    }

    public Set<String> associationFields() { return associationFields; }
    public TableMeta associationFields(Set<String> v) { associationFields.addAll(v); return this; }
}
