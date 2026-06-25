package vip.isass.framework.apidoc.zyplayer;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

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

    private boolean openApiEnabled = true;

    private String openApiDocsPath;

    private String openApiSourcePath = "classpath:service-docs/api/openapi.json";

    private String apiBaseUrl;

    private String groupName = "isass";

    private List<String> excludeControllers = new ArrayList<>();

    private List<String> excludePaths = new ArrayList<>();

    private List<String> excludePathPatterns = new ArrayList<>();

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

    public boolean isOpenApiEnabled() {
        return openApiEnabled;
    }

    public void setOpenApiEnabled(boolean openApiEnabled) {
        this.openApiEnabled = openApiEnabled;
    }

    public String getOpenApiDocsPath() {
        return openApiDocsPath;
    }

    public void setOpenApiDocsPath(String openApiDocsPath) {
        this.openApiDocsPath = openApiDocsPath;
    }

    public String getOpenApiSourcePath() {
        return openApiSourcePath;
    }

    public void setOpenApiSourcePath(String openApiSourcePath) {
        this.openApiSourcePath = openApiSourcePath;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<String> getExcludeControllers() {
        return excludeControllers;
    }

    public void setExcludeControllers(List<String> excludeControllers) {
        this.excludeControllers = excludeControllers;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public List<String> getExcludePathPatterns() {
        return excludePathPatterns;
    }

    public void setExcludePathPatterns(List<String> excludePathPatterns) {
        this.excludePathPatterns = excludePathPatterns;
    }
}
