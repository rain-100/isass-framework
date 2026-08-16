// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("isass.nocode.initialization")
public class NocodeInitializationProperties {

    private boolean enabled = true;
    private String location = "classpath*:init/**/*.json";
    private boolean failFast = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isFailFast() { return failFast; }
    public void setFailFast(boolean failFast) { this.failFast = failFast; }
}
