package com.shofiniakol.common;

public final class Money {

    private Money() {
    }

    /** Whole-percent discount between original and current price (0 when there is no discount). */
    public static int discountPercent(long price, long originalPrice) {
        if (originalPrice <= 0 || originalPrice <= price) {
            return 0;
        }
        return (int) Math.round((originalPrice - price) * 100.0 / originalPrice);
    }

    public static long saving(long price, long originalPrice) {
        return Math.max(0, originalPrice - price);
    }
}
