// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.nocode.criteria.type.IPageCriteria;
import vip.isass.framework.nocode.entity.IEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 批量操作工具（nocode）。
 * 基于 nocode 分页条件，提供分页全量查询和分批处理。
 */
@Slf4j
public final class BatchUtil {

    private BatchUtil() {
    }

    public static <R, E extends IEntity<E>, C extends IPageCriteria<E, C>> List<R> findAllByBatchPage(
            C countCriteria,
            Function<C, Integer> countFunction,
            C fetchCriteria,
            Function<C, Page<E>> fetchFunction,
            Function<Page<E>, List<R>> consumeFunction) {
        return findAllByBatchPage(countCriteria, countFunction, fetchCriteria, fetchFunction, consumeFunction, -1);
    }

    public static <R, E extends IEntity<E>, C extends IPageCriteria<E, C>> List<R> findAllByBatchPage(
            C countCriteria,
            Function<C, Integer> countFunction,
            C fetchCriteria,
            Function<C, Page<E>> fetchFunction,
            Function<Page<E>, List<R>> consumeFunction,
            int limitResultSize) {
        Assert.notNull(countCriteria, "countCriteria");
        int totalCount = countFunction.apply(countCriteria);
        int cap = limitResultSize > 0 ? Math.min(totalCount, limitResultSize) : totalCount;
        List<R> result = new ArrayList<>(cap);

        long currentPage = 1L;
        long pageSize = fetchCriteria.getPageSize() == 20L ? 1000L : fetchCriteria.getPageSize();
        fetchCriteria.setPageNum(currentPage).setPageSize(pageSize).setSearchCountFlag(false);
        long totalPageNum = (long) Math.ceil((double) totalCount / pageSize);

        do {
            log.debug("findAllByBatchPage 进度：{}/{}", currentPage, totalCount);
            Page<E> page = fetchFunction.apply(fetchCriteria);
            result.addAll(consumeFunction.apply(page));
            fetchCriteria.setPageNum(++currentPage);
            if (limitResultSize > -1 && result.size() >= limitResultSize) {
                break;
            }
        } while (currentPage <= totalPageNum);

        return result;
    }

    public static <E extends IEntity<E>, C extends IPageCriteria<E, C>> void batchFunction(
            C countCriteria,
            Function<C, Integer> countFunction,
            C fetchCriteria,
            Function<C, Page<E>> fetchFunction,
            Consumer<Page<E>> consumeFunction) {
        Assert.notNull(countCriteria, "countCriteria");
        int totalCount = countFunction.apply(countCriteria);

        long currentPage = 1L;
        long pageSize = fetchCriteria.getPageSize() == 20L ? 1000L : fetchCriteria.getPageSize();
        fetchCriteria.setPageNum(currentPage).setPageSize(pageSize).setSearchCountFlag(false);
        long totalPageNum = (long) Math.ceil((double) totalCount / pageSize);

        do {
            log.debug("batchFunction 进度：{}/{}", currentPage, totalCount);
            Page<E> page = fetchFunction.apply(fetchCriteria);
            fetchCriteria.setPageNum(++currentPage);
            consumeFunction.accept(page);
        } while (currentPage <= totalPageNum);
    }

    public static <T, R> List<R> batchFunction(Integer batchSize, Collection<T> collection,
                                                Function<List<T>, List<R>> function) {
        if (CollUtil.isEmpty(collection)) {
            return Collections.emptyList();
        }
        List<R> result = new ArrayList<>(Math.max(100_000, collection.size()));
        List<List<T>> split = CollUtil.split(collection, batchSize == null ? 1000 : batchSize);
        for (int i = 0; i < split.size(); i++) {
            log.debug("正在执行 batchFunction，进度：{}/{}", i + 1, split.size());
            result.addAll(function.apply(split.get(i)));
        }
        return result;
    }

    public static <T> void batchConsumer(Integer batchSize, Collection<T> collection,
                                          Consumer<List<T>> consumer) {
        if (CollUtil.isEmpty(collection)) {
            return;
        }
        List<List<T>> split = CollUtil.split(collection, batchSize == null ? 1000 : batchSize);
        for (int i = 0; i < split.size(); i++) {
            log.debug("正在执行 batchConsumer，进度：{}/{}", i + 1, split.size());
            consumer.accept(split.get(i));
        }
    }
}
