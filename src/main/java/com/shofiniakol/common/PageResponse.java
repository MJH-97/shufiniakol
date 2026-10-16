package com.shofiniakol.common;

import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** 1-based pagination envelope: {items,page,limit,total,total_pages,has_next}. */
public record PageResponse<T>(List<T> items, int page, int limit, long total, int totalPages, boolean hasNext) {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    public static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /** Slices an already-sorted in-memory list. */
    public static <T> PageResponse<T> of(List<T> all, Integer page, Integer limit) {
        int p = normalizePage(page);
        int l = normalizeLimit(limit);
        long total = all.size();
        int totalPages = (int) Math.ceil(total / (double) l);
        long from = (long) (p - 1) * l;
        List<T> slice = from >= total
                ? Collections.emptyList()
                : all.subList((int) from, (int) Math.min(total, from + l));
        return new PageResponse<>(slice, p, l, total, totalPages, p < totalPages);
    }

    /** Converts a Spring Data (0-based) page. */
    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        List<T> items = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(items, page.getNumber() + 1, page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.hasNext());
    }
}
