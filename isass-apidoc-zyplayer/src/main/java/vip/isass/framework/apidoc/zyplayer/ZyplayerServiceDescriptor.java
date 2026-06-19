package vip.isass.framework.apidoc.zyplayer;

import org.springframework.util.StringUtils;

/**
 * @author Rain
 */
public record ZyplayerServiceDescriptor(
        String applicationName,
        String serviceNameCn,
        String version
) {

    public ZyplayerServiceDescriptor {
        applicationName = StringUtils.hasText(applicationName) ? applicationName.trim() : "application";
        serviceNameCn = StringUtils.hasText(serviceNameCn) ? serviceNameCn.trim() : applicationName;
        version = ZyplayerVersion.normalize(version);
    }

    public String spaceName() {
        return serviceNameCn + "v" + version;
    }

    public String spaceUuid() {
        return applicationName + ":" + version;
    }
}
