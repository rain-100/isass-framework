// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.database.mybatisplus.orm;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.wrapper.UpdateJoinWrapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vip.isass.framework.common.criteria.NullValueMode;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IIdEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MybatisPlusNullUpdateTest {
    @BeforeAll
    static void metadata() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "explicit-null-test");
        assistant.setCurrentNamespace(ProbeMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Probe.class);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void explicitNullDoesNotClearOmittedFieldsOrUpdateUnsubmittedValues() {
        var repository = new ProbeRepository();
        Probe submitted = new Probe();
        submitted.setId(1L);
        submitted.setLabel(null);
        submitted.markPresentProperty("label");
        // Deliberately untracked: presence, not values on a mutable object, defines this mode.
        submitted.setNote("not submitted");
        submitted.setTenantId(2L);
        submitted.setVersion(4);
        when(repository.base().update(any(Probe.class), any(Wrapper.class))).thenReturn(1);

        assertEquals(1, repository.updateCountByCriteria(submitted, criteria(NullValueMode.WRITE_PRESENT_NULL)));

        var entity = ArgumentCaptor.forClass(Probe.class);
        ArgumentCaptor<Wrapper<Probe>> wrapper = ArgumentCaptor.forClass((Class) Wrapper.class);
        verify(repository.base()).update(entity.capture(), wrapper.capture());
        var update = (UpdateWrapper<Probe>) wrapper.getValue();
        assertTrue(update.getSqlSet().contains("LABEL"), update.getSqlSet());
        assertFalse(update.getSqlSet().contains("NOTE"), update.getSqlSet());
        assertFalse(update.getSqlSet().contains("TENANT_ID"), update.getSqlSet());
        assertFalse(update.getSqlSet().contains("MODIFY_TIME"), update.getSqlSet());
        assertFalse(update.getSqlSet().contains("VERSION"), update.getSqlSet());
        assertFalse(update.getSqlSet().contains("DELETE_FLAG"), update.getSqlSet());
        assertNull(entity.getValue().getNote());
        assertNull(entity.getValue().getTenantId());
        assertEquals(4, entity.getValue().getVersion());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void legacyFullNullAndDefaultIgnoreSemanticsRemainUnchanged() {
        for (NullValueMode mode : new NullValueMode[]{NullValueMode.WRITE_NULL, NullValueMode.IGNORE_NULL}) {
            var repository = new ProbeRepository();
            Probe submitted = new Probe();
            submitted.setLabel(null);
            submitted.markPresentProperty("label");
            submitted.setNote("kept");
            submitted.markPresentProperty("note");
            when(repository.base().update(any(Probe.class), any(Wrapper.class))).thenReturn(1);
            repository.updateCountByCriteria(submitted, criteria(mode));
            ArgumentCaptor<Wrapper<Probe>> wrapper = ArgumentCaptor.forClass((Class) Wrapper.class);
            verify(repository.base()).update(any(Probe.class), wrapper.capture());
            String sql = ((UpdateWrapper<Probe>) wrapper.getValue()).getSqlSet();
            if (mode == NullValueMode.WRITE_NULL) {
                assertTrue(sql.contains("LABEL"));
                assertTrue(sql.contains("TENANT_ID"));
            } else {
                assertNull(sql);
            }
        }
    }

    @Test
    void missingPresenceOrOnlyFrameworkFieldsCannotBecomeFullNullWrites() {
        for (boolean tracked : new boolean[]{false, true}) {
            var repository = new ProbeRepository();
            Probe submitted = new Probe();
            submitted.setId(1L);
            if (tracked) {
                submitted.markPresentProperty("id");
                submitted.markPresentProperty("version");
                submitted.markPresentProperty("modifyTime");
                submitted.markPresentProperty("deleteFlag");
            }
            assertThrows(IllegalArgumentException.class, () -> repository.updateCountByCriteria(
                    submitted, criteria(NullValueMode.WRITE_PRESENT_NULL)));
            verifyNoInteractions(repository.base());
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void joinUpdatesAlsoClearOnlyExplicitNullProperties() {
        var repository = new ProbeRepository();
        Probe submitted = new Probe();
        submitted.markPresentProperty("label");
        var criteria = criteria(NullValueMode.WRITE_PRESENT_NULL)
                .leftJoin(Probe.class, Probe::getId, Probe::getId);
        when(repository.base().updateJoin(any(Probe.class), any())).thenReturn(1);
        repository.updateCountByCriteria(submitted, criteria);
        ArgumentCaptor<UpdateJoinWrapper<Probe>> wrapper = ArgumentCaptor.forClass((Class) UpdateJoinWrapper.class);
        verify(repository.base()).updateJoin(any(Probe.class), wrapper.capture());
        assertTrue(wrapper.getValue().getSqlSet().contains("LABEL = NULL"));
        assertFalse(wrapper.getValue().getSqlSet().contains("NOTE"));
        assertFalse(wrapper.getValue().getSqlSet().contains("TENANT_ID"));
    }

    private static ProbeCriteria criteria(NullValueMode mode) {
        return new ProbeCriteria().equals("id", 1L).setNullValueMode(mode);
    }

    @Getter
    @Setter
    @TableName("explicit_null_probe")
    static class Probe implements IIdEntity<Long, Probe> {
        @TableId("ID")
        private Long id;
        @TableField("LABEL")
        private String label;
        @TableField("NOTE")
        private String note;
        @TableField("TENANT_ID")
        private Long tenantId;
        @TableField(value = "MODIFY_TIME", fill = FieldFill.UPDATE)
        private Long modifyTime;
        @Version
        private Integer version;
        @TableLogic
        private Integer deleteFlag;

        @Override
        public Probe randomEntity() {
            return this;
        }
    }

    static class ProbeCriteria extends FullTypeCriteria<Probe, ProbeCriteria> {
    }

    interface ProbeMapper extends MPJBaseMapper<Probe> {
    }

    static class ProbeRepository extends MybatisPlusRepository<Probe, ProbeCriteria, ProbeMapper> {
        ProbeRepository() {
            baseMapper = mock(ProbeMapper.class);
        }

        ProbeMapper base() {
            return baseMapper;
        }
    }
}
