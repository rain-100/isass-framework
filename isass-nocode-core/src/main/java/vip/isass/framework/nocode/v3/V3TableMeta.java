package vip.isass.framework.nocode.v3;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;

import java.util.HashMap;
import java.util.Map;

public class V3TableMeta {

    private String tableName;
    private IdType idType;
    private Class<?> keyType;
    private String idColumnName;
    private String logicDeleteField;
    private String versionField;
    private String tenantIdField;
    private String parentIdField;
    private final Map<String, FieldFill> fillFields = new HashMap<>();

    public String tableName() { return tableName; }
    public V3TableMeta tableName(String v) { this.tableName = v; return this; }

    public IdType idType() { return idType; }
    public V3TableMeta idType(IdType v) { this.idType = v; return this; }

    public Class<?> keyType() { return keyType; }
    public V3TableMeta keyType(Class<?> v) { this.keyType = v; return this; }

    public String idColumnName() { return idColumnName; }
    public V3TableMeta idColumnName(String v) { this.idColumnName = v; return this; }

    public String logicDeleteField() { return logicDeleteField; }
    public V3TableMeta logicDeleteField(String v) { this.logicDeleteField = v; return this; }

    public String versionField() { return versionField; }
    public V3TableMeta versionField(String v) { this.versionField = v; return this; }

    public String tenantIdField() { return tenantIdField; }
    public V3TableMeta tenantIdField(String v) { this.tenantIdField = v; return this; }

    public String parentIdField() { return parentIdField; }
    public V3TableMeta parentIdField(String v) { this.parentIdField = v; return this; }

    public Map<String, FieldFill> fillFields() { return fillFields; }
    public V3TableMeta fillFields(Map<String, FieldFill> v) { fillFields.putAll(v); return this; }
}
