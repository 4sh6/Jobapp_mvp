
package com.example.service;

import com.example.model.EmailOtp;
import com.example.repository.EmailOtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    @Autowired
    private EmailOtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    // SECURITY: use SecureRandom instead of Random for cryptographic OTP generation
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void generateAndSendOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        String code = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        EmailOtp otp = new EmailOtp(normalizedEmail, code, expiresAt);
        otpRepository.save(otp);

        emailService.sendOtp(normalizedEmail, code);
    }

    @Transactional
    public boolean verifyOtp(String email, String code) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String trimmedCode = code == null ? "" : code.trim();
        Optional<EmailOtp> latest = otpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail);
        if (latest.isEmpty()) {
            return false;
        }
        EmailOtp otp = latest.get();
        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            return false;
        }
        if (!otp.getCode().equals(trimmedCode)) {
            return false;
        }
        // Invalidate OTP after successful verification — prevents reuse
        otpRepository.delete(otp);
        return true;
    }

    @Transactional
    public void resendOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        // Delete all previous OTPs for this email then generate a fresh one
        otpRepository.deleteByEmail(normalizedEmail);
        generateAndSendOtp(normalizedEmail);
    }

    /** Runs every 15 minutes to purge expired OTPs and prevent database bloat. */
    @Scheduled(fixedRate = 900_000)
    public void cleanupExpiredOtps() {
        otpRepository.deleteExpiredBefore(LocalDateTime.now());
    }
}
