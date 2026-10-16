package com.shofiniakol.auth;

import com.shofiniakol.auth.AuthDtos.AuthResponse;
import com.shofiniakol.auth.AuthDtos.RefreshRequest;
import com.shofiniakol.auth.AuthDtos.RequestOtpRequest;
import com.shofiniakol.auth.AuthDtos.RequestOtpResponse;
import com.shofiniakol.auth.AuthDtos.VerifyOtpRequest;
import com.shofiniakol.config.AppProperties;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AppProperties props;

    @PostMapping("/request-otp")
    public RequestOtpResponse requestOtp(@Valid @RequestBody RequestOtpRequest req) {
        String phone = authService.requestOtp(req.phone());
        return new RequestOtpResponse(phone, props.getOtp().getTtlSeconds(), props.getOtp().getResendCooldownSeconds());
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return authService.verifyOtp(req.phone(), req.code());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
