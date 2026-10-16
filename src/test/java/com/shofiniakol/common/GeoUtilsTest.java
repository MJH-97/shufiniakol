package com.shofiniakol.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoUtilsTest {

    @Test
    void zeroDistanceForSamePoint() {
        assertEquals(0.0, GeoUtils.distanceKm(33.5138, 36.2765, 33.5138, 36.2765), 1e-9);
    }

    @Test
    void damascusToHomsIsAbout160Km() {
        double d = GeoUtils.distanceKm(33.5138, 36.2765, 34.7324, 36.7137);
        assertTrue(d > 140 && d < 160, "distance was " + d);
    }

    @Test
    void validatesCoordinates() {
        assertTrue(GeoUtils.validLatLng(33.5, 36.2));
        assertFalse(GeoUtils.validLatLng(null, 36.2));
        assertFalse(GeoUtils.validLatLng(91.0, 36.2));
    }
}
