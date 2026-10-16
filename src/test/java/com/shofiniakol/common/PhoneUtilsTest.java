package com.shofiniakol.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneUtilsTest {

    @Test
    void acceptsBothFormatsShownInTheApp() {
        assertEquals("+963968726559", PhoneUtils.normalize("0968726559"));
        assertEquals("+963968726559", PhoneUtils.normalize("+963968726559"));
        assertEquals("+963968726559", PhoneUtils.normalize("00963968726559"));
        assertEquals("+963968726559", PhoneUtils.normalize("963968726559"));
    }

    @Test
    void ignoresSpacesAndDashes() {
        assertEquals("+963968726559", PhoneUtils.normalize("096 872-6559"));
    }

    @Test
    void convertsArabicIndicDigits() {
        assertEquals("+963968726559", PhoneUtils.normalize("٠٩٦٨٧٢٦٥٥٩"));
    }

    @Test
    void rejectsInvalidNumbers() {
        assertThrows(ApiException.class, () -> PhoneUtils.normalize("0568726559"));  // must start with 09
        assertThrows(ApiException.class, () -> PhoneUtils.normalize("096872655"));   // too short
        assertThrows(ApiException.class, () -> PhoneUtils.normalize("abc"));
        assertThrows(ApiException.class, () -> PhoneUtils.normalize(null));
    }
}
