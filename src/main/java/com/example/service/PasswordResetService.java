package com.example.service;

import com.example.model.Jobseeker;
import com.example.model.PasswordResetToken;
import com.example.repository.JobseekerRepository;
import com.example.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Generates a reset token and sends a reset link email.
     * Silently does nothing if the email is not registered, to avoid user enumeration.
     */
    @Transactional
    public void initiateReset(String email, String baseUrl) {
        String normalized = email.trim().toLowerCase();
        Optional<Jobseeker> jobseekerOpt = jobseekerRepository.findByEmail(normalized);
        if (jobseekerOpt.isEmpty()) {
            return;
        }

        // Invalidate any previous tokens for this email
        tokenRepository.deleteByEmail(normalized);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(normalized);
        resetToken.setToken(token);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        tokenRepository.save(resetToken);

        String resetLink = baseUrl + "/jobseeker/reset-password?token=" + token;
        emailService.sendPasswordReset(normalized, resetLink);
    }

    public boolean isValidToken(String token) {
        return tokenRepository.findByToken(token)
                .map(t -> !t.isUsed() && LocalDateTime.now().isBefore(t.getExpiresAt()))
                .orElse(false);
    }

    /**
     * Validates the token and updates the jobseeker's password.
     * Returns true on success, false if token is invalid/expired/already used.
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) return false;

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isUsed() || LocalDateTime.now().isAfter(resetToken.getExpiresAt())) {
            return false;
        }

        Optional<Jobseeker> jobseekerOpt = jobseekerRepository.findByEmail(resetToken.getEmail());
        if (jobseekerOpt.isEmpty()) return false;

        Jobseeker jobseeker = jobseekerOpt.get();
        jobseeker.setPassword(passwordEncoder.encode(newPassword));
        jobseekerRepository.save(jobseeker);

        // Mark token as used so it cannot be reused
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return true;
    }
}