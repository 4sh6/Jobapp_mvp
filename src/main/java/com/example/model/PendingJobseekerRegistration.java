package com.example.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Holds registration form data between "Create Account" and OTP verification.
 * No Jobseeker row (and therefore no admin-portal visibility) exists until the
 * OTP is confirmed — see JobseekerAuthController.handleVerifyOtp().
 */
@Entity
@Table(name = "pending_jobseeker_registrations")
public class PendingJobseekerRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Already BCrypt-encoded — never store a raw password. */
    @Column(nullable = false)
    private String password;

    @Column(name = "referred_by_code")
    private String referredByCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PendingJobseekerRegistration() {
    }

    public PendingJobseekerRegistration(String email, String fullName, String password, String referredByCode) {
        this.email = email;
        this.fullName = fullName;
        this.password = password;
        this.referredByCode = referredByCode;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getReferredByCode() { return referredByCode; }
    public void setReferredByCode(String referredByCode) { this.referredByCode = referredByCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
