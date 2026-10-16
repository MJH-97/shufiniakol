package com.shofiniakol.auth;

import com.shofiniakol.user.UserDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RequestOtpRequest(@NotBlank(message = "اكتب رقم الموبايل") String phone) {
    }

    public record RequestOtpResponse(String phone, int expiresInSeconds, int resendAfterSeconds) {
    }

    public record VerifyOtpRequest(
            @NotBlank(message = "اكتب رقم الموبايل") String phone,
            @NotBlank(message = "اكتب رمز التحقق") @Size(min = 4, max = 8, message = "رمز التحقق غير صحيح") String code) {
    }

    public record RefreshRequest(@NotBlank(message = "refresh_token مطلوب") String refreshToken) {
    }

    public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
                               boolean newUser, UserDto user) {
    }
}
