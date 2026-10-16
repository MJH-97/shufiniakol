package com.shofiniakol.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {

    @Test
    void discountMatchesBriefExample() {
        // "وجبة برغر دبل": 45,000 instead of 60,000 -> 25% off, saves 15,000
        assertEquals(25, Money.discountPercent(45_000, 60_000));
        assertEquals(15_000, Money.saving(45_000, 60_000));
    }

    @Test
    void noDiscountWhenPricesEqualOrInvalid() {
        assertEquals(0, Money.discountPercent(50_000, 50_000));
        assertEquals(0, Money.discountPercent(50_000, 0));
        assertEquals(0, Money.saving(50_000, 40_000));
    }
}
