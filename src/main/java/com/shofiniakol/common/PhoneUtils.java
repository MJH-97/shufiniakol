package com.shofiniakol.common;

/**
 * Syrian mobile numbers. The app accepts {@code 0968726559} or {@code +963968726559};
 * everything is stored in E.164 form ({@code +963968726559}).
 */
public final class PhoneUtils {

    private PhoneUtils() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            throw ApiException.badRequest("INVALID_PHONE", "رقم الموبايل غير صحيح");
        }
        StringBuilder sb = new StringBuilder();
        for (char c : raw.trim().toCharArray()) {
            if (c >= '\u0660' && c <= '\u0669') {          // Arabic-Indic digits
                sb.append((char) ('0' + (c - '\u0660')));
            } else if (c >= '\u06F0' && c <= '\u06F9') {   // Eastern Arabic-Indic digits
                sb.append((char) ('0' + (c - '\u06F0')));
            } else if (c == ' ' || c == '-' || c == '(' || c == ')') {
                continue;
            } else {
                sb.append(c);
            }
        }
        String s = sb.toString();
        if (s.startsWith("+963")) {
            s = s.substring(1);
        } else if (s.startsWith("00963")) {
            s = s.substring(2);
        } else if (s.startsWith("09")) {
            s = "963" + s.substring(1);
        }
        if (!s.matches("^9639\\d{8}$")) {
            throw ApiException.badRequest("INVALID_PHONE", "رقم الموبايل غير صحيح");
        }
        return "+" + s;
    }
}
