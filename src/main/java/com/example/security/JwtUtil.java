package com.example.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${admin.jwt.secret}")
    private String secret;

    @Value("${admin.jwt.expiration-ms}")
    private long expirationMs;

    /** Fail fast at startup if the JWT secret is too short or is the old insecure placeholder. */
    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "admin.jwt.secret must be at least 32 characters. " +
                    "Set the ADMIN_JWT_SECRET environment variable to a strong random value.");
        }
        // Reject the old placeholder that was accidentally shipped as a default
        if (secret.equals("change-me-to-a-256-bit-secret-key-for-production-use")) {
            throw new IllegalStateException(
                    "admin.jwt.secret is set to the insecure placeholder. " +
                    "Set ADMIN_JWT_SECRET to a strong, unique random value before running.");
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns true only if the token is not expired AND carries the ADMIN role claim. */
    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            boolean notExpired = claims.getExpiration().after(new Date());
            boolean isAdmin = "ADMIN".equals(claims.get("role", String.class));
            return notExpired && isAdmin;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}