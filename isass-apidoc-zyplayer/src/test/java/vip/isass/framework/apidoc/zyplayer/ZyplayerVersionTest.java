package vip.isass.framework.apidoc.zyplayer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZyplayerVersionTest {

    @Test
    void normalizesSnapshotAndPrereleaseVersionToReleaseVersion() {
        assertThat(ZyplayerVersion.normalize("4.0.0-SNAPSHOT")).isEqualTo("4.0.0");
        assertThat(ZyplayerVersion.normalize("4.1.0-RC1")).isEqualTo("4.1.0");
        assertThat(ZyplayerVersion.normalize("4.2.3")).isEqualTo("4.2.3");
    }

    @Test
    void buildsSpaceNameWithoutVersionAndKeepsVersionForSpaceVersion() {
        ZyplayerServiceDescriptor descriptor = new ZyplayerServiceDescriptor(
                "attachment-service", "附件微服务", "4.0.0-SNAPSHOT");

        assertThat(descriptor.spaceName()).isEqualTo("附件微服务");
        assertThat(descriptor.spaceUuid()).isEqualTo("attachment-service");
        assertThat(descriptor.version()).isEqualTo("4.0.0");
    }

    @Test
    void fallsBackToApplicationNameWhenChineseNameIsBlank() {
        ZyplayerServiceDescriptor descriptor = new ZyplayerServiceDescriptor(
                "attachment-service", " ", "4.0.0-SNAPSHOT");

        assertThat(descriptor.spaceName()).isEqualTo("attachment-service");
        assertThat(descriptor.spaceUuid()).isEqualTo("attachment-service");
    }
}
