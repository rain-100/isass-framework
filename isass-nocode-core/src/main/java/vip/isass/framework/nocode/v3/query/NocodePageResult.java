package vip.isass.framework.nocode.v3.query;

import java.util.List;

/**
 * Framework-neutral paged query result.
 *
 * @param records    current page records
 * @param pageNumber one-based page number
 * @param pageSize   page size
 * @param total      total record count
 * @param <T>        record type
 */
public record NocodePageResult<T>(
        List<T> records,
        int pageNumber,
        int pageSize,
        long total
) {

    public NocodePageResult {
        records = records == null ? List.of() : List.copyOf(records);
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be greater than 0");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
    }

    public static <T> NocodePageResult<T> empty(NocodePageRequest pageRequest) {
        return new NocodePageResult<>(
                List.of(),
                pageRequest.pageNumber(),
                pageRequest.pageSize(),
                0
        );
    }

    public long totalPages() {
        if (total == 0) {
            return 0;
        }
        return (total + pageSize - 1L) / pageSize;
    }

    public boolean hasNext() {
        return pageNumber < totalPages();
    }

    public boolean hasPrevious() {
        return pageNumber > 1;
    }
}
