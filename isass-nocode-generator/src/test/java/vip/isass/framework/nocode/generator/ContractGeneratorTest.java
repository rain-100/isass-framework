package vip.isass.framework.nocode.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.contract.ContractDocument;
import vip.isass.framework.nocode.contract.HttpMethod;
import vip.isass.framework.nocode.contract.ParameterSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractGeneratorTest {

    @TempDir
    Path temp;

    @Test
    void generatesContractAndProtoFromServiceJavadoc() throws Exception {
        Path source = temp.resolve("src");
        Path output = temp.resolve("out");
        Files.createDirectories(source.resolve("vip/isass/attachment/api"));
        Files.writeString(source.resolve("vip/isass/attachment/api/IIconService.java"), """
                package vip.isass.attachment.api;
                import java.util.List;
                class Icon {
                    /** 图标名称 */
                    String iconName;
                }
                class IconResult {
                    /** 返回数量 */
                    Integer total;
                }
                interface IconCriteria {}
                interface IService<E,C> {}
                public interface IIconService extends IService<Icon,IconCriteria> {
                    /**
                     * 查询可用图标
                     * @param tenantId 租户 ID
                     * @return 可用图标
                     * @http GET /available/{tenantId}
                     * @order 501
                     */
                    IconResult findAvailableIcons(Long tenantId);
                }
                """);
        Files.writeString(source.resolve("vip/isass/attachment/api/IGroupService.java"), """
                package vip.isass.attachment.api;
                class Group { String name; }
                interface GroupCriteria {}
                public interface IGroupService extends IService<Group,GroupCriteria> {}
                """);

        new ContractGenerator(new ObjectMapper()).generate(source, output);

        ContractDocument document = new ObjectMapper().readValue(
                output.resolve("META-INF/isass/nocode-contract.json").toFile(),
                ContractDocument.class);
        var operation = document.services().getFirst().operations().stream()
                .filter(item -> item.name().equals("findAvailableIcons"))
                .findFirst().orElseThrow();
        assertEquals("findAvailableIcons", operation.name());
        assertEquals(HttpMethod.GET, operation.httpMethod());
        assertEquals(ParameterSource.PATH, operation.parameters().getFirst().source());
        assertEquals(501, operation.order());
        assertEquals("图标名称", document.types().getFirst()
                .properties().getFirst().description());
        assertTrue(document.types().stream()
                .anyMatch(type -> type.javaType().endsWith("IconResult")));
        String proto = Files.readString(output.resolve("proto/attachment-nocode.proto"));
        assertTrue(proto.contains("rpc FindAvailableIcons"));
        assertTrue(proto.contains("service IconService"));
        assertTrue(proto.contains("service GroupService"));
    }

    @Test
    void generatesContractFromGeneratedPackageLayout() throws Exception {
        Path source = temp.resolve("generated-layout-src");
        Path output = temp.resolve("generated-layout-out");
        Files.createDirectories(source.resolve("vip/isass/attachment/api/model/entity"));
        Files.createDirectories(source.resolve("vip/isass/attachment/api/model/criteria"));
        Files.createDirectories(source.resolve("vip/isass/attachment/api/service"));
        Files.writeString(source.resolve("vip/isass/attachment/api/model/entity/Attachment.java"), """
                package vip.isass.attachment.api.model.entity;
                public class Attachment {
                    /** 原始文件名 */
                    String originalFileName;
                }
                """);
        Files.writeString(source.resolve("vip/isass/attachment/api/model/criteria/AttachmentCriteria.java"), """
                package vip.isass.attachment.api.model.criteria;
                public class AttachmentCriteria {}
                """);
        Files.writeString(source.resolve("vip/isass/attachment/api/service/IAttachmentService.java"), """
                package vip.isass.attachment.api.service;
                import vip.isass.attachment.api.model.criteria.AttachmentCriteria;
                import vip.isass.attachment.api.model.entity.Attachment;
                interface IService<E,C> {}
                public interface IAttachmentService extends IService<Attachment, AttachmentCriteria> {}
                """);

        new ContractGenerator(new ObjectMapper()).generate(source, output);

        ContractDocument document = new ObjectMapper().readValue(
                output.resolve("META-INF/isass/nocode-contract.json").toFile(),
                ContractDocument.class);
        assertEquals(1, document.services().size());
        assertEquals("attachment", document.services().getFirst().entityName());
        assertEquals("Attachment", document.types().getFirst().schemaName());
        assertEquals("原始文件名", document.types().getFirst().properties().getFirst().description());
    }

    @Test
    void rejectsCustomServiceMethodWithoutHttpTag() throws Exception {
        Path source = temp.resolve("missing-http-src");
        Files.createDirectories(source.resolve("vip/isass/attachment/api"));
        Files.writeString(source.resolve("vip/isass/attachment/api/IIconService.java"), """
                package vip.isass.attachment.api;
                class Icon {}
                class IconCriteria {}
                interface IService<E,C> {}
                public interface IIconService extends IService<Icon,IconCriteria> {
                    Icon findAvailableIcon();
                }
                """);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ContractGenerator(new ObjectMapper()).generate(source, temp.resolve("missing-http-out")));
        assertEquals("Custom nocode method requires @http METHOD /path: vip.isass.attachment.api.IIconService#findAvailableIcon",
                exception.getMessage());
    }
}
