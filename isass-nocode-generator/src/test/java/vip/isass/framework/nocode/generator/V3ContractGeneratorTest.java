package vip.isass.framework.nocode.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.v3.contract.V3ContractDocument;
import vip.isass.framework.nocode.v3.contract.V3HttpMethod;
import vip.isass.framework.nocode.v3.contract.V3ParameterSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V3ContractGeneratorTest {

    @TempDir
    Path temp;

    @Test
    void generatesContractAndProtoFromServiceJavadoc() throws Exception {
        Path source = temp.resolve("src");
        Path output = temp.resolve("out");
        Files.createDirectories(source.resolve("vip/isass/attachment/api"));
        Files.writeString(source.resolve("vip/isass/attachment/api/IV3IconService.java"), """
                package vip.isass.attachment.api;
                import java.util.List;
                class V3Icon {
                    /** 图标名称 */
                    String iconName;
                }
                interface V3IconCriteria {}
                interface IV3Service<E,C> {}
                public interface IV3IconService extends IV3Service<V3Icon,V3IconCriteria> {
                    /**
                     * 查询可用图标
                     * @param tenantId 租户 ID
                     * @return 可用图标
                     * @http GET /available/{tenantId}
                     * @order 501
                     */
                    List<V3Icon> findAvailableIcons(Long tenantId);
                }
                """);
        Files.writeString(source.resolve("vip/isass/attachment/api/IV3GroupService.java"), """
                package vip.isass.attachment.api;
                class V3Group { String name; }
                interface V3GroupCriteria {}
                public interface IV3GroupService extends IV3Service<V3Group,V3GroupCriteria> {}
                """);

        new V3ContractGenerator(new ObjectMapper()).generate(source, output);

        V3ContractDocument document = new ObjectMapper().readValue(
                output.resolve("META-INF/isass/v3-contract.json").toFile(),
                V3ContractDocument.class);
        var operation = document.services().getFirst().operations().stream()
                .filter(item -> item.name().equals("findAvailableIcons"))
                .findFirst().orElseThrow();
        assertEquals("findAvailableIcons", operation.name());
        assertEquals(V3HttpMethod.GET, operation.httpMethod());
        assertEquals(V3ParameterSource.PATH, operation.parameters().getFirst().source());
        assertEquals(501, operation.order());
        assertEquals("图标名称", document.types().getFirst()
                .properties().getFirst().description());
        String proto = Files.readString(output.resolve("proto/attachment-v3.proto"));
        assertTrue(proto.contains("rpc FindAvailableIcons"));
        assertTrue(proto.contains("service IconService"));
        assertTrue(proto.contains("service GroupService"));
    }

    @Test
    void generatesContractFromGeneratedV3PackageLayout() throws Exception {
        Path source = temp.resolve("generated-layout-src");
        Path output = temp.resolve("generated-layout-out");
        Files.createDirectories(source.resolve("vip/isass/attachment/api/model/entity"));
        Files.createDirectories(source.resolve("vip/isass/attachment/api/model/criteria"));
        Files.createDirectories(source.resolve("vip/isass/attachment/api/service"));
        Files.writeString(source.resolve("vip/isass/attachment/api/model/entity/V3Attachment.java"), """
                package vip.isass.attachment.api.model.entity;
                public class V3Attachment {
                    /** 原始文件名 */
                    String originalFileName;
                }
                """);
        Files.writeString(source.resolve("vip/isass/attachment/api/model/criteria/V3AttachmentCriteria.java"), """
                package vip.isass.attachment.api.model.criteria;
                public class V3AttachmentCriteria {}
                """);
        Files.writeString(source.resolve("vip/isass/attachment/api/service/IV3AttachmentService.java"), """
                package vip.isass.attachment.api.service;
                import vip.isass.attachment.api.model.criteria.V3AttachmentCriteria;
                import vip.isass.attachment.api.model.entity.V3Attachment;
                interface IV3Service<E,C> {}
                public interface IV3AttachmentService extends IV3Service<V3Attachment, V3AttachmentCriteria> {}
                """);

        new V3ContractGenerator(new ObjectMapper()).generate(source, output);

        V3ContractDocument document = new ObjectMapper().readValue(
                output.resolve("META-INF/isass/v3-contract.json").toFile(),
                V3ContractDocument.class);
        assertEquals(1, document.services().size());
        assertEquals("attachment", document.services().getFirst().entityName());
        assertEquals("V3Attachment", document.types().getFirst().schemaName());
        assertEquals("原始文件名", document.types().getFirst().properties().getFirst().description());
    }

    @Test
    void rejectsCustomServiceMethodWithoutHttpTag() throws Exception {
        Path source = temp.resolve("missing-http-src");
        Files.createDirectories(source.resolve("vip/isass/attachment/api"));
        Files.writeString(source.resolve("vip/isass/attachment/api/IV3IconService.java"), """
                package vip.isass.attachment.api;
                class V3Icon {}
                class V3IconCriteria {}
                interface IV3Service<E,C> {}
                public interface IV3IconService extends IV3Service<V3Icon,V3IconCriteria> {
                    V3Icon findAvailableIcon();
                }
                """);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new V3ContractGenerator(new ObjectMapper()).generate(source, temp.resolve("missing-http-out")));
        assertEquals("Custom V3 method requires @http METHOD /path: vip.isass.attachment.api.IV3IconService#findAvailableIcon",
                exception.getMessage());
    }
}
