package com.shofiniakol.user;

import java.time.Instant;
import java.util.UUID;

public record UserDto(UUID id, String phone, String fullName, String email, String language, Role role,
                      UserStatus status, Instant createdAt) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getPhone(), u.getFullName(), u.getEmail(), u.getLanguage(), u.getRole(),
                u.getStatus(), u.getCreatedAt());
    }
}
