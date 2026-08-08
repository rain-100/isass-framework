package vip.isass.framework.nocode.http;

import java.util.LinkedHashMap;
import java.util.Map;

/** One entity selection in a portable export profile. */
public class NocodeExportPlan {

    private String service;
    private String entity;
    private Map<String, Object> criteria = new LinkedHashMap<>();

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public Map<String, Object> getCriteria() {
        return criteria;
    }

    public void setCriteria(Map<String, Object> criteria) {
        this.criteria = criteria == null ? new LinkedHashMap<>() : new LinkedHashMap<>(criteria);
    }
}
