package vip.isass.framework.nocode;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;

import java.util.HashMap;
import java.util.Map;

public class TableMeta {

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
    public TableMeta tableName(String v) { this.tableName = v; return this; }

    public IdType idType() { return idType; }
    public TableMeta idType(IdType v) { this.idType = v; return this; }

    public Class<?> keyType() { return keyType; }
    public TableMeta keyType(Class<?> v) { this.keyType = v; return this; }

    public String idColumnName() { return idColumnName; }
    public TableMeta idColumnName(String v) { this.idColumnName = v; return this; }

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
}
