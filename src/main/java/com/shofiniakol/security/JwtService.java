package com.shofiniakol.security;

import com.shofiniakol.config.AppProperties;
import com.shofiniakol.user.Role;
import com.shofiniakol.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private static final String DEV_SECRET_PREFIX = "dev-only-secret";

    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(AppProperties props) {
        String secret = props.getSecurity().getJwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.security.jwt-secret (JWT_SECRET) must be at least 32 bytes long");
        }
        if (secret.startsWith(DEV_SECRET_PREFIX)) {
            log.warn("JWT_SECRET is the built-in development secret. Set a strong JWT_SECRET before deploying.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(props.getSecurity().getAccessTokenTtlMinutes());
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("phone", user.getPhone())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    public Optional<AuthUser> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthUser(
                    UUID.fromString(c.getSubject()),
                    c.get("phone", String.class),
                    Role.valueOf(c.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
