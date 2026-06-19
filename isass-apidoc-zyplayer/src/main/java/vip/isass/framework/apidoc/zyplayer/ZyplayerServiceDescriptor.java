package vip.isass.framework.apidoc.zyplayer;

import org.springframework.util.StringUtils;

/**
 * @author Rain
 */
public record ZyplayerServiceDescriptor(
        String applicationName,
        String serviceNameCn,
        String version,
        String groupName
) {

    public ZyplayerServiceDescriptor {
        applicationName = StringUtils.hasText(applicationName) ? applicationName.trim() : "application";
        serviceNameCn = StringUtils.hasText(serviceNameCn) ? serviceNameCn.trim() : applicationName;
        version = ZyplayerVersion.normalize(version);
        groupName = StringUtils.hasText(groupName) ? groupName.trim() : "isass";
    }

    public ZyplayerServiceDescriptor(String applicationName, String serviceNameCn, String version) {
        this(applicationName, serviceNameCn, version, "isass");
    }

    public String spaceName() {
        return serviceNameCn;
    }

    public String spaceUuid() {
        return applicationName;
    }
}
