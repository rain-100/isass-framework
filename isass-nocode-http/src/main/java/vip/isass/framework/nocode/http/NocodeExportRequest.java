// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.http;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Invokes a named export profile or caller-supplied export plans. */
public class NocodeExportRequest {

    private String profileCode;
    private List<NocodeExportPlan> plans;
    private Map<String, Object> input = new LinkedHashMap<>();

    public String getProfileCode() {
        return profileCode;
    }

    public void setProfileCode(String profileCode) {
        this.profileCode = profileCode;
    }

    public List<NocodeExportPlan> getPlans() {
        return plans;
    }

    public void setPlans(List<NocodeExportPlan> plans) {
        this.plans = plans;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
    }
}
