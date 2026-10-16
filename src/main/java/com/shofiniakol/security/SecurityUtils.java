package com.shofiniakol.security;

import com.shofiniakol.common.ApiException;
import com.shofiniakol.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthUser currentOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser u) {
            return u;
        }
        return null;
    }

    public static AuthUser current() {
        AuthUser u = currentOrNull();
        if (u == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "يرجى تسجيل الدخول");
        }
        return u;
    }

    public static UUID currentUserId() {
        return current().id();
    }

    public static UUID currentUserIdOrNull() {
        AuthUser u = currentOrNull();
        return u == null ? null : u.id();
    }

    public static boolean isAdmin() {
        AuthUser u = currentOrNull();
        return u != null && u.role() == Role.ADMIN;
    }
}
