package com.shofiniakol.user;

public enum Role {
    CUSTOMER,
    /** Manages only the restaurants he owns (dashboard in /api/v1/admin). */
    RESTAURANT_OWNER,
    ADMIN
}
