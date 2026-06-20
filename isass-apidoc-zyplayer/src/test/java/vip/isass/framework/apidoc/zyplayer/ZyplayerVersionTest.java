package vip.isass.framework.apidoc.zyplayer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZyplayerVersionTest {

    @Test
    void normalizesSnapshotAndPrereleaseVersionToMajorVersion() {
        assertThat(ZyplayerVersion.normalize("4.0.0-SNAPSHOT")).isEqualTo("v4.x");
        assertThat(ZyplayerVersion.normalize("4.1.0-RC1")).isEqualTo("v4.x");
        assertThat(ZyplayerVersion.normalize("v4.2.3")).isEqualTo("v4.x");
    }

    @Test
    void buildsSpaceNameWithoutVersionAndKeepsVersionForSpaceVersion() {
        ZyplayerServiceDescriptor descriptor = new ZyplayerServiceDescriptor(
                "attachment-service", "附件微服务", "4.0.0-SNAPSHOT");

        assertThat(descriptor.spaceName()).isEqualTo("附件微服务");
        assertThat(descriptor.spaceUuidPrefix()).isEqualTo("attachment-service");
        assertThat(descriptor.version()).isEqualTo("v4.x");
    }

    @Test
    void fallsBackToApplicationNameWhenChineseNameIsBlank() {
        ZyplayerServiceDescriptor descriptor = new ZyplayerServiceDescriptor(
                "attachment-service", " ", "4.0.0-SNAPSHOT");

        assertThat(descriptor.spaceName()).isEqualTo("attachment-service");
        assertThat(descriptor.spaceUuidPrefix()).isEqualTo("attachment-service");
    }
}
