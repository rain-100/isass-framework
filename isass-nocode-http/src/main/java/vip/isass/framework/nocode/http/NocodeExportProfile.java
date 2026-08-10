// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.http;

import java.util.ArrayList;
import java.util.List;

/** Complete, self-contained export definition loaded from export-profiles/*.yaml. */
public class NocodeExportProfile {

    private String code;
    private String name;
    private String description;
    private List<NocodeExportPlan> entities = new ArrayList<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<NocodeExportPlan> getEntities() {
        return entities;
    }

    public void setEntities(List<NocodeExportPlan> entities) {
        this.entities = entities == null ? new ArrayList<>() : new ArrayList<>(entities);
    }
}
