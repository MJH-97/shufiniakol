package com.shofiniakol.auth;

import com.shofiniakol.auth.AuthDtos.AuthResponse;
import com.shofiniakol.common.ApiException;
import com.shofiniakol.common.PhoneUtils;
import com.shofiniakol.config.AppProperties;
import com.shofiniakol.security.JwtService;
import com.shofiniakol.user.Role;
import com.shofiniakol.user.User;
import com.shofiniakol.user.UserDto;
import com.shofiniakol.user.UserRepository;
import com.shofiniakol.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public String requestOtp(String rawPhone) {
        String phone = PhoneUtils.normalize(rawPhone);
        otpService.request(phone);
        return phone;
    }

    /**
     * noRollbackFor: a wrong code throws ApiException, but the incremented attempt counter written by
     * {@link OtpService#verify} must still be committed (otherwise brute-force protection would be void).
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse verifyOtp(String rawPhone, String code) {
        String phone = PhoneUtils.normalize(rawPhone);
        otpService.verify(phone, code.trim());

        boolean isNew = false;
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            isNew = true;
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw ApiException.forbidden("ACCOUNT_BLOCKED", "تم إيقاف هذا الحساب، تواصل مع الدعم");
        }
        if (phone.equals(bootstrapAdminPhone()) && user.getRole() != Role.ADMIN) {
            user.setRole(Role.ADMIN);
        }
        user.setLastLoginAt(Instant.now());
        user = userRepository.save(user);
        return issueTokens(user, isNew);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(refreshToken))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "انتهت الجلسة، سجّل الدخول مجدداً"));
        Instant now = Instant.now();
        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(now)) {
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "انتهت الجلسة، سجّل الدخول مجدداً");
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "انتهت الجلسة، سجّل الدخول مجدداً"));
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw ApiException.forbidden("ACCOUNT_BLOCKED", "تم إيقاف هذا الحساب، تواصل مع الدعم");
        }
        stored.setRevokedAt(now); // rotation: a refresh token is single-use
        return issueTokens(user, false);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHash(sha256(refreshToken)).ifPresent(t -> {
            if (t.getRevokedAt() == null) {
                t.setRevokedAt(Instant.now());
            }
        });
    }

    private AuthResponse issueTokens(User user, boolean isNew) {
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setTokenHash(sha256(refreshToken));
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(props.getSecurity().getRefreshTokenTtlDays())));
        refreshTokenRepository.save(rt);

        return new AuthResponse(jwtService.createAccessToken(user), refreshToken, "Bearer",
                jwtService.accessTtlSeconds(), isNew, UserDto.from(user));
    }

    private String bootstrapAdminPhone() {
        String configured = props.getBootstrap().getAdminPhone();
        if (configured == null || configured.isBlank()) {
            return null;
        }
        try {
            return PhoneUtils.normalize(configured);
        } catch (ApiException e) {
            log.warn("BOOTSTRAP_ADMIN_PHONE '{}' is not a valid Syrian mobile number; ignored", configured);
            return null;
        }
    }

    static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
