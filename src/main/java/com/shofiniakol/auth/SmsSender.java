package com.shofiniakol.auth;

/**
 * Integration point for the SMS gateway used to deliver OTP codes.
 * Provide your own {@code @Component @Primary} implementation (Syriatel, MTN, Twilio, Infobip, ...)
 * to replace {@link LoggingSmsSender}.
 */
public interface SmsSender {

    void send(String phoneE164, String message);
}
