
package com.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobseekers")
public class Jobseeker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String fullName;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    @Column(nullable = false)
    private String password;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "profile_completed")
    private boolean profileCompleted = false;

    @Column(name = "resume_uploaded")
    private boolean resumeUploaded = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Column(name = "auth_provider")
    private String authProvider;

    @Column(name = "active")
    private boolean active = true;

    // PENDING → admin reviews → APPROVED or REJECTED → HIRED when placed
    @Column(name = "approval_status")
    private String approvalStatus = "PENDING";

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Unique code used in the referral link. Generated once on first save. */
    @Column(name = "referral_code", unique = true, length = 10)
    private String referralCode;

    // Tracks how many dashboard sessions the "Profile Approved" banner has been shown (max 3)
    @Column(name = "approval_banner_shown_count", columnDefinition = "integer default 0")
    private Integer approvalBannerShownCount = 0;

    // True after "Profile Complete!" banner has been shown once; reset when profile drops below 100%
    @Column(name = "profile_complete_banner_shown", columnDefinition = "boolean default false")
    private Boolean profileCompleteBannerShown = false;

    /** Set to true by the candidate to pause their profile — hidden from recruiter browse. */
    @Column(name = "profile_paused", columnDefinition = "boolean default false")
    private boolean profilePaused = false;

    /**
     * When true, recruiters whose company name matches the candidate's currentCompany
     * will NOT see this candidate in browse results — protects active employees.
     */
    @Column(name = "hide_from_current_employer", columnDefinition = "boolean default false")
    private boolean hideFromCurrentEmployer = false;

    /** Timestamp when candidate last paused their profile. */
    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    /** Timestamp when admin approved this profile — used for recency sort in recruiter browse. */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** Timestamp when admin rejected this profile — used to enforce 30-day re-registration cooldown. */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public boolean isResumeUploaded() {
        return resumeUploaded;
    }

    public void setResumeUploaded(boolean resumeUploaded) {
        this.resumeUploaded = resumeUploaded;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public int getApprovalBannerShownCount() { return approvalBannerShownCount != null ? approvalBannerShownCount : 0; }
    public void setApprovalBannerShownCount(int approvalBannerShownCount) { this.approvalBannerShownCount = approvalBannerShownCount; }

    public boolean isProfileCompleteBannerShown() { return profileCompleteBannerShown != null && profileCompleteBannerShown; }
    public void setProfileCompleteBannerShown(boolean profileCompleteBannerShown) { this.profileCompleteBannerShown = profileCompleteBannerShown; }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    public boolean isProfilePaused() { return profilePaused; }
    public void setProfilePaused(boolean profilePaused) { this.profilePaused = profilePaused; }

    public boolean isHideFromCurrentEmployer() { return hideFromCurrentEmployer; }
    public void setHideFromCurrentEmployer(boolean hideFromCurrentEmployer) { this.hideFromCurrentEmployer = hideFromCurrentEmployer; }

    public LocalDateTime getPausedAt() { return pausedAt; }
    public void setPausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }
}
