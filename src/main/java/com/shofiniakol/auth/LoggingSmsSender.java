package com.shofiniakol.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Development fallback: writes the SMS to the application log instead of sending it. */
@Slf4j
@Component
public class LoggingSmsSender implements SmsSender {

    @Override
    public void send(String phoneE164, String message) {
        log.warn("[SMS-NOT-SENT] No SMS gateway configured. To={} Message={}", phoneE164, message);
    }
}
