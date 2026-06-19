package vip.isass.framework.apidoc.zyplayer;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Rain
 */
@ConfigurationProperties("isass.apidoc.zyplayer")
public class ZyplayerApidocProperties {

    private boolean enabled;

    private String baseUrl;

    private String apiKey;

    private String privateKey;

    private boolean deleteMissing;

    private boolean release = true;

    private boolean failOnError;

    private String version = "4.0.0-SNAPSHOT";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public boolean isDeleteMissing() {
        return deleteMissing;
    }

    public void setDeleteMissing(boolean deleteMissing) {
        this.deleteMissing = deleteMissing;
    }

    public boolean isRelease() {
        return release;
    }

    public void setRelease(boolean release) {
        this.release = release;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
