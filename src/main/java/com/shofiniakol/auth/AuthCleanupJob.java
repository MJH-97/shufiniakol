package com.shofiniakol.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Housekeeping: removes expired OTP codes and refresh tokens. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthCleanupJob {

    private final OtpCodeRepository otpRepo;
    private final RefreshTokenRepository refreshRepo;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanup() {
        int otps = otpRepo.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));
        int tokens = refreshRepo.deleteExpiredBefore(Instant.now());
        log.info("Auth cleanup removed {} otp codes and {} refresh tokens", otps, tokens);
    }
}
