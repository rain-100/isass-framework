package vip.isass.framework.apidoc.zyplayer;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
