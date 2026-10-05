// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.orm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.yulichang.base.MPJBaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.criteria.impl.type.WhereConditionCriteria;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IIdEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Guards the bounded, no-count existence query and caller Criteria isolation.
 */
class MybatisPlusExistsTest {

    @BeforeAll
    static void initializeMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "exists-test");
        assistant.setCurrentNamespace(ProbeMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Probe.class);
    }

    @Test
    void criteriaExistenceReadsOnePrimaryKeyWithoutCountOrSorting() {
        ProbeRepository<ProbeCriteria> repository = new ProbeRepository<>();
        ProbeCriteria criteria = new ProbeCriteria()
                .equals("label", "needle")
                .setReturnFields(List.of("label"))
                .setOrderBy("label desc")
                .setPageNum(20L)
                .setPageSize(100L)
                .setSearchCountFlag(true);

        assertTrue(repository.isPresentByCriteria(criteria));

        assertBoundedPage(repository);
        assertEquals("t.FILE_ID", repository.wrapper.getSqlSelect());
        assertTrue(repository.wrapper.getSqlSegment().contains("FILE_LABEL ="));
        assertFalse(repository.wrapper.getSqlSegment().contains("ORDER BY"));
        assertTrue(((MpjWrapper<Probe>) repository.wrapper)
                .getParamNameValuePairs().containsValue("needle"));
        assertEquals("label desc", criteria.getOrderBy());
        assertEquals(List.of("label"), criteria.getReturnFields());
        assertEquals(20L, criteria.getPageNum());
        assertEquals(100L, criteria.getPageSize());
        assertTrue(criteria.getSearchCountFlag());

        repository.results = List.of();
        assertFalse(repository.isPresentByCriteria(criteria));
    }

    @Test
    void idAndColumnChecksUsePaginationInsteadOfHardcodedLimitOrCount() {
        ProbeRepository<ProbeCriteria> repository = new ProbeRepository<>();

        assertTrue(repository.isPresentById(42L));
        assertBoundedPage(repository);
        assertEquals("FILE_ID", repository.wrapper.getSqlSelect());
        assertTrue(repository.wrapper.getSqlSegment().contains("FILE_ID ="));
        assertFalse(repository.wrapper.getSqlSegment().toLowerCase().contains("limit"));

        repository.results = List.of();
        assertFalse(repository.isPresentByColumn("label", "missing"));
        assertBoundedPage(repository);
        assertTrue(repository.wrapper.getSqlSegment().contains("FILE_LABEL ="));
    }

    @Test
    void aMatchedRowWithNullProjectionStillCountsAsPresent() {
        ProbeRepository<ProbeCriteria> repository = new ProbeRepository<>();
        repository.results = Collections.singletonList(null);

        assertTrue(repository.isPresentByWrapper(
                new QueryWrapper<Probe>().select("FILE_LABEL")));
        assertBoundedPage(repository);
    }

    @Test
    void criteriaWithoutOrderingQueriesWithoutCopying() {
        ProbeRepository<UnorderedProbeCriteria> repository = new ProbeRepository<>();
        UnorderedProbeCriteria criteria = new UnorderedProbeCriteria().equals("label", "needle");

        assertTrue(repository.isPresentByCriteria(criteria));
        assertBoundedPage(repository);
        assertEquals("t.FILE_ID", repository.wrapper.getSqlSelect());
        assertTrue(repository.wrapper.getSqlSegment().contains("FILE_LABEL ="));
        assertTrue(((MpjWrapper<Probe>) repository.wrapper)
                .getParamNameValuePairs().containsValue("needle"));

        repository.results = List.of();
        assertFalse(repository.isPresentByCriteria(criteria));
    }

    private static void assertBoundedPage(ProbeRepository<?> repository) {
        assertEquals(1L, repository.page.getCurrent());
        assertEquals(1L, repository.page.getSize());
        assertFalse(repository.page.searchCount());
    }

    @Getter
    @Setter
    @TableName("exists_probe")
    static class Probe implements IIdEntity<Long, Probe> {

        @TableId("FILE_ID")
        private Long id;

        @TableField("FILE_LABEL")
        private String label;

        @TableField("CREATE_USER_ID")
        private Long createUserId;

        @TableField(exist = false)
        private List<Probe> related;

        @Override
        public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.many("related", Probe.class, "id", "id"));
        }

        @Override
        public Probe randomEntity() {
            return this;
        }
    }

    static class ProbeCriteria extends FullTypeCriteria<Probe, ProbeCriteria> {
    }

    static class UnorderedProbeCriteria extends WhereConditionCriteria<Probe, UnorderedProbeCriteria> {

        @Override
        public UnorderedProbeCriteria copy() {
            throw new AssertionError("不支持排序的 Criteria 不应为存在性查询创建副本");
        }
    }

    interface ProbeMapper extends MPJBaseMapper<Probe> {
    }

    static class ProbeRepository<C extends ICriteria<Probe, C>>
            extends MybatisPlusRepository<Probe, C, ProbeMapper> {

        private List<Probe> results = List.of(new Probe());
        private IPage<Probe> page;
        private Wrapper<Probe> wrapper;

        @SuppressWarnings("unchecked")
        ProbeRepository() {
            baseMapper = mock(ProbeMapper.class);
            doAnswer(invocation -> {
                IPage<Probe> queryPage = invocation.getArgument(0);
                Wrapper<Probe> queryWrapper = invocation.getArgument(2);
                return page(queryPage, queryWrapper);
            }).when(baseMapper).selectJoinPage(any(IPage.class), eq(Probe.class), any());
        }

        @Override
        public <P extends IPage<Probe>> P page(P page, Wrapper<Probe> wrapper) {
            this.page = page;
            this.wrapper = wrapper;
            page.setRecords(results);
            return page;
        }

        @Override
        public Integer countByWrapper(Wrapper<Probe> wrapper) {
            throw new AssertionError("存在性检查不能执行 COUNT");
        }
    }
}
