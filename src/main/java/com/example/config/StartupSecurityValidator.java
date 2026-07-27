package com.example.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StartupSecurityValidator {

    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final String DEFAULT_JWT_SECRET_PREFIX = "koderhyre-dev-only";

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.jwt.secret}")
    private String jwtSecret;

    @Value("${security.skip-prod-secret-validation:false}")
    private boolean skipValidation;

    @PostConstruct
    public void validate() {
        if (skipValidation) return;

        if (DEFAULT_ADMIN_PASSWORD.equals(adminPassword)) {
            throw new IllegalStateException(
                "STARTUP BLOCKED: ADMIN_PASSWORD is set to the insecure default. " +
                "Set the ADMIN_PASSWORD environment variable before deploying. " +
                "To bypass in local development, activate the 'dev' Spring profile " +
                "(--spring.profiles.active=dev).");
        }
        if (jwtSecret.startsWith(DEFAULT_JWT_SECRET_PREFIX)) {
            throw new IllegalStateException(
                "STARTUP BLOCKED: ADMIN_JWT_SECRET is set to the insecure default. " +
                "Set the ADMIN_JWT_SECRET environment variable before deploying. " +
                "To bypass in local development, activate the 'dev' Spring profile " +
                "(--spring.profiles.active=dev).");
        }
    }
}