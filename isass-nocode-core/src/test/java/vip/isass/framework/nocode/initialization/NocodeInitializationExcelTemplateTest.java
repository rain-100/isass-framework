// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.entrypoint.annotation.ApiDoc;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeInitializationExcelTemplateTest {

    @Test
    void usesFieldDescriptionsAndExcludesAuditFieldsAssociationsAndNonPersistentState() throws Exception {
        byte[] bytes = NocodeInitializationExcelTemplate.create(Map.of("sample", TemplateEntity.class));
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            List<String> headers = new ArrayList<>();
            sheet.getRow(0).forEach(cell -> headers.add(cell.getStringCellValue()));
            assertThat(headers).containsExactly("id", "state", "options", "enabled", "inherited",
                    "tenantId", "version", "deleteFlag");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("主键");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).contains("状态", "1=OPEN", "2=CLOSED");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).contains("可选项", "直接填写", "无需 json: 前缀");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).contains("是否启用", "TRUE/FALSE");
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo("父类字段");
            assertThat(sheet.getColumnStyle(0).getDataFormatString()).isEqualTo("@");
            assertThat(sheet.getRow(2).getPhysicalNumberOfCells()).isZero();
            assertThat(sheet.getRow(2).isFormatted()).isFalse();
            assertThat(sheet.getRow(2).getHeight()).isEqualTo(sheet.getDefaultRowHeight());
            assertThat(sheet.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 2);
            assertThat(sheet.isDisplayGridlines()).isTrue();
            assertThat(sheet.getRow(1).getHeightInPoints()).isEqualTo(72);
            for (var cell : sheet.getRow(1)) {
                var style = (XSSFCellStyle) cell.getCellStyle();
                assertThat(style.getFillForegroundXSSFColor().getARGBHex()).isEqualTo("FFEEE9F6");
                assertThat(style.getFont().getXSSFColor().getARGBHex()).isEqualTo("FF393448");
                assertThat(style.getFont().getFontName()).isEqualTo("Arial");
                assertThat(style.getFont().getFontHeightInPoints()).isEqualTo((short) 11);
                assertThat(style.getWrapText()).isTrue();
                assertThat(style.getAlignment()).isEqualTo(HorizontalAlignment.LEFT);
                assertThat(style.getVerticalAlignment()).isEqualTo(VerticalAlignment.CENTER);
                assertThat(style.getBorderTop()).isEqualTo(BorderStyle.THIN);
                assertThat(style.getBorderRight()).isEqualTo(BorderStyle.THIN);
                assertThat(style.getBorderBottom()).isEqualTo(BorderStyle.THIN);
                assertThat(style.getBorderLeft()).isEqualTo(BorderStyle.THIN);
            }
        }
        var reader = new NocodeInitializationDataService(List.of());
        var document = reader.readDocument(new ByteArrayInputStream(bytes), "template.xlsx");
        assertThat(document.get("sample")).isEmpty();
    }

    @Test
    void rejectsUnsupportedSheetNamesAndCaseInsensitiveCollisions() {
        assertThatThrownBy(() -> NocodeInitializationExcelTemplate.create(
                Map.of("entityNameLongerThanThirtyOneCharacters", TemplateEntity.class)))
                .hasMessageContaining("sheet");
        Map<String, Class<?>> types = new LinkedHashMap<>();
        types.put("sample", TemplateEntity.class);
        types.put("Sample", TemplateEntity.class);
        assertThatThrownBy(() -> NocodeInitializationExcelTemplate.create(types))
                .hasMessageContaining("名称冲突");
    }

    static class TemplateBase {
        @ApiDoc(description = "父类字段")
        protected String inherited;
        protected Long tenantId;
        protected Long version;
        protected Boolean deleteFlag;
        protected Long createUserId;
        protected String createUserName;
        protected Long createTime;
    }

    static class TemplateEntity extends TemplateBase implements IEntity<TemplateEntity> {
        private static final long serialVersionUID = 1L;

        @ApiDoc(description = "主键")
        private Long id;
        @ApiDoc(description = "状态")
        private State state;
        @ApiDoc(description = "可选项")
        private List<String> options;
        @ApiDoc(description = "是否启用")
        private Boolean enabled;
        private Long modifyUserId;
        private String modifyUserName;
        private Long modifyTime;
        private transient String cache;
        @JsonIgnore
        private String ignored;
        private Collection<TemplateEntity> children;

        @Override
        public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.many("children", TemplateEntity.class, "id", "id"));
        }

        @Override
        public TemplateEntity randomEntity() {
            return this;
        }
    }

    enum State {
        OPEN(1), CLOSED(2);

        private final int code;

        State(int code) {
            this.code = code;
        }

        @JsonValue
        public int getCode() {
            return code;
        }
    }
}
