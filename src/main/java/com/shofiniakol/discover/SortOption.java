package com.shofiniakol.discover;

import com.shofiniakol.common.ApiException;
import com.shofiniakol.common.Lang;

import java.util.Locale;

/** The four sort chips of the app plus lowest price. */
public enum SortOption {
    BEST_VALUE("أفضل قيمة", "Best value", "Meilleure valeur"),
    BIGGEST_SAVING("أكبر توفير", "Biggest saving", "Plus grande économie"),
    TOP_RATED("أعلى تقييم", "Top rated", "Mieux noté"),
    NEAREST("الأقرب", "Nearest", "Le plus proche"),
    LOWEST_PRICE("الأرخص", "Lowest price", "Moins cher");

    private final String ar;
    private final String en;
    private final String fr;

    SortOption(String ar, String en, String fr) {
        this.ar = ar;
        this.en = en;
        this.fr = fr;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String label() {
        return Lang.pick(ar, en, fr);
    }

    /** Lenient: null/blank means best_value; accepts "best_value", "best-value", "BEST_VALUE". */
    public static SortOption parse(String s) {
        if (s == null || s.isBlank()) {
            return BEST_VALUE;
        }
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_SORT", "نوع الترتيب غير مدعوم: " + s);
        }
    }
}
