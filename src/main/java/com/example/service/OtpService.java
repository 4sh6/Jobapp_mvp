
package com.example.service;

import com.example.model.EmailOtp;
import com.example.repositary.EmailOtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private EmailOtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    private final Random random = new Random();

    public void generateAndSendOtp(String email) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        EmailOtp otp = new EmailOtp(email, code, expiresAt);
        otpRepository.save(otp);

        emailService.sendOtp(email, code);
    }

    public boolean verifyOtp(String email, String code) {
        Optional<EmailOtp> latest = otpRepository.findTopByEmailOrderByCreatedAtDesc(email);
        if (latest.isEmpty()) {
            return false;
        }
        EmailOtp otp = latest.get();
        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            return false;
        }
        if (!otp.getCode().equals(code)) {
            return false;
        }
        // Invalidate OTP after successful verification — prevents reuse
        otpRepository.delete(otp);
        return true;
    }

    public void resendOtp(String email) {
        // Delete all previous OTPs for this email then generate a fresh one
        otpRepository.deleteByEmail(email);
        generateAndSendOtp(email);
    }
}
