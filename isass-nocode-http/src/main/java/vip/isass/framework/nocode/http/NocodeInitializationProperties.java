package vip.isass.framework.nocode.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings for classpath JSON data imported after the application starts. */
@ConfigurationProperties("isass.nocode.initialization")
public class NocodeInitializationProperties {

    private boolean enabled = true;
    private String location = "classpath*:init/*.json";
    private boolean failFast = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }
}
