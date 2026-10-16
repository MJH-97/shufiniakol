package com.shofiniakol.catalog;

import com.shofiniakol.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repo;

    @Transactional(readOnly = true)
    public List<Category> active() {
        return repo.findByActiveTrueOrderBySortOrderAsc();
    }

    /** null / blank / "all" / "الكل" mean "no category filter" and resolve to null. */
    @Transactional(readOnly = true)
    public Category resolve(String slugOrAll) {
        if (slugOrAll == null || slugOrAll.isBlank()) {
            return null;
        }
        String s = slugOrAll.trim().toLowerCase();
        if (s.equals("all") || s.equals("الكل")) {
            return null;
        }
        return repo.findBySlug(s)
                .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "الفئة غير موجودة: " + slugOrAll));
    }
}
