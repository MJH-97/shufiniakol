package com.shofiniakol.security;

import com.shofiniakol.user.Role;

import java.util.UUID;

/** Authenticated principal extracted from the access token. */
public record AuthUser(UUID id, String phone, Role role) {
}
