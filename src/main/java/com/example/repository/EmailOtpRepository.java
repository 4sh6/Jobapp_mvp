package com.example.repository;

import com.example.model.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByEmailOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);

    // Return all OTPs for an email, most recent first
    List<EmailOtp> findByEmailOrderByCreatedAtDesc(String email);

    /** Bulk-delete all OTPs whose expiry has passed — called by scheduled cleanup job. */
    @Modifying
    @Transactional
    @Query("DELETE FROM EmailOtp o WHERE o.expiresAt < :now")
    void deleteExpiredBefore(@org.springframework.data.repository.query.Param("now") LocalDateTime now);
}
