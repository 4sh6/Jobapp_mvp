package com.example.repositary;

import com.example.model.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByEmailOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);

    // Return all OTPs for an email, most recent first
    List<EmailOtp> findByEmailOrderByCreatedAtDesc(String email);
}
