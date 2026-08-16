// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.page;

import java.io.Serializable;
import java.util.List;

/**
 * ORM-neutral result of an offset-based paged query.
 */
public final class Page<T> implements Serializable {

    private List<T> records = List.of();
    private long pageNum = 1L;
    private long pageSize = 20L;
    private long total;
    private long pageCount;

    public Page() {
    }

    public Page(List<T> records, long pageNum, long pageSize, long total) {
        setRecords(records);
        setPageNum(pageNum);
        setPageSize(pageSize);
        setTotal(total);
        this.pageCount = calculatePageCount(total, pageSize);
    }

    public static <T> Page<T> of(List<T> records, long pageNum, long pageSize, long total) {
        return new Page<>(records, pageNum, pageSize, total);
    }

    public static <T> Page<T> empty(long pageNum, long pageSize) {
        return new Page<>(List.of(), pageNum, pageSize, 0L);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records == null ? List.of() : List.copyOf(records);
    }

    public long getPageNum() {
        return pageNum;
    }

    public void setPageNum(long pageNum) {
        if (pageNum < 1L) throw new IllegalArgumentException("pageNum 必须大于 0");
        this.pageNum = pageNum;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        if (pageSize < 1L) throw new IllegalArgumentException("pageSize 必须大于 0");
        this.pageSize = pageSize;
        this.pageCount = calculatePageCount(total, pageSize);
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        if (total < 0L) throw new IllegalArgumentException("total 不能小于 0");
        this.total = total;
        this.pageCount = calculatePageCount(total, pageSize);
    }

    public long getPageCount() {
        return pageCount;
    }

    public void setPageCount(long pageCount) {
        if (pageCount < 0L) throw new IllegalArgumentException("pageCount 不能小于 0");
        this.pageCount = pageCount;
    }

    public boolean hasNext() {
        return pageNum < pageCount;
    }

    public boolean hasPrevious() {
        return pageNum > 1L;
    }

    private static long calculatePageCount(long total, long pageSize) {
        return total / pageSize + (total % pageSize == 0L ? 0L : 1L);
    }
}
