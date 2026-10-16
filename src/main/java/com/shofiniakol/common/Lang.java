package com.shofiniakol.common;

import org.springframework.context.i18n.LocaleContextHolder;

/** Resolves the response language (ar default, en, fr) from the request's Accept-Language header. */
public final class Lang {

    private Lang() {
    }

    public static String current() {
        String l = LocaleContextHolder.getLocale().getLanguage();
        return switch (l) {
            case "en", "fr" -> l;
            default -> "ar";
        };
    }

    public static String pick(String ar, String en, String fr) {
        return switch (current()) {
            case "en" -> en != null ? en : ar;
            case "fr" -> fr != null ? fr : (en != null ? en : ar);
            default -> ar;
        };
    }
}
