package com.shofiniakol.auth;

import com.shofiniakol.common.ApiException;
import com.shofiniakol.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpCodeRepository repo;
    private final AppProperties props;
    private final SmsSender smsSender;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void request(String phone) {
        AppProperties.Otp cfg = props.getOtp();
        Instant now = Instant.now();

        var latest = repo.findFirstByPhoneOrderByCreatedAtDesc(phone);
        if (latest.isPresent()) {
            long elapsed = Duration.between(latest.get().getCreatedAt(), now).getSeconds();
            if (elapsed < cfg.getResendCooldownSeconds()) {
                long wait = cfg.getResendCooldownSeconds() - elapsed;
                throw ApiException.tooMany("OTP_COOLDOWN", "انتظر قليلاً قبل إعادة إرسال الرمز")
                        .with("retry_after_seconds", wait);
            }
        }
        if (repo.countByPhoneAndCreatedAtAfter(phone, now.minus(Duration.ofHours(1))) >= cfg.getMaxPerHour()) {
            throw ApiException.tooMany("OTP_LIMIT", "تجاوزت عدد المحاولات المسموحة، حاول لاحقاً");
        }

        String code = generateCode(cfg.getLength());
        OtpCode otp = new OtpCode();
        otp.setPhone(phone);
        otp.setCodeHash(hash(phone, code));
        otp.setExpiresAt(now.plusSeconds(cfg.getTtlSeconds()));
        repo.save(otp);

        smsSender.send(phone, "رمز التحقق الخاص بك في شو فيني آكل: " + code);
    }

    /** Attempt counter must survive the thrown exception, hence noRollbackFor. */
    @Transactional(noRollbackFor = ApiException.class)
    public void verify(String phone, String code) {
        AppProperties.Otp cfg = props.getOtp();
        String devCode = cfg.getDevFixedCode();
        if (devCode != null && !devCode.isBlank() && devCode.equals(code)) {
            return;
        }

        Instant now = Instant.now();
        OtpCode otp = repo.findFirstByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> ApiException.badRequest("OTP_NOT_FOUND", "اطلب رمز تحقق جديد"));

        if (otp.getExpiresAt().isBefore(now)) {
            throw ApiException.badRequest("OTP_EXPIRED", "انتهت صلاحية الرمز، اطلب رمزاً جديداً");
        }
        if (otp.getAttempts() >= cfg.getMaxAttempts()) {
            throw ApiException.tooMany("OTP_ATTEMPTS_EXCEEDED", "تجاوزت عدد المحاولات، اطلب رمزاً جديداً");
        }

        byte[] expected = otp.getCodeHash().getBytes(StandardCharsets.UTF_8);
        byte[] actual = hash(phone, code).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            otp.setAttempts(otp.getAttempts() + 1);
            throw ApiException.badRequest("OTP_INVALID", "رمز التحقق غير صحيح")
                    .with("remaining_attempts", Math.max(0, cfg.getMaxAttempts() - otp.getAttempts()));
        }
        otp.setConsumedAt(now);
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String hash(String phone, String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((phone + ":" + code + ":" + props.getOtp().getPepper())
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
