package vip.isass.framework.apidoc.zyplayer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Rain
 */
public record ZyplayerServiceDescriptor(
        String applicationName,
        String serviceNameCn,
        String groupName
) {

    public ZyplayerServiceDescriptor {
        applicationName = ZyplayerText.trimToDefault(applicationName, "application");
        serviceNameCn = ZyplayerText.trimToDefault(serviceNameCn, applicationName);
        groupName = ZyplayerText.trimToDefault(groupName, "isass");
    }

    public ZyplayerServiceDescriptor(String applicationName, String serviceNameCn) {
        this(applicationName, serviceNameCn, "isass");
    }

    private static final DateTimeFormatter SPACE_UUID_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public String spaceName() {
        return serviceNameCn;
    }

    public String spaceUuidPrefix() {
        return applicationName;
    }

    public String spaceUuid() {
        return spaceUuidPrefix() + "@" + LocalDateTime.now().format(SPACE_UUID_TIMESTAMP);
    }
}
