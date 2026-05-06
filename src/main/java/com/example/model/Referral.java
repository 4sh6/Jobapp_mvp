package com.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks the referral relationship between a referrer (sharer) and a referee (new signup).
 *
 * Status flow: SIGNED_UP → PROFILE_APPROVED → HIRED → REWARD_PAID
 */
@Entity
@Table(name = "referrals")
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The jobseeker who shared the referral link. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id", nullable = false)
    private Jobseeker referrer;

    /** The jobseeker who signed up via the referral link. One referee per referral. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_id", nullable = false, unique = true)
    private Jobseeker referee;

    /**
     * Lifecycle:
     *  SIGNED_UP       – referee completed registration
     *  PROFILE_APPROVED – admin approved referee's profile
     *  HIRED           – referee was marked HIRED by a recruiter
     *  REWARD_PAID     – admin has paid the referral reward to the referrer
     */
    @Column(nullable = false, length = 20)
    private String status = "SIGNED_UP";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "hired_at")
    private LocalDateTime hiredAt;

    @Column(name = "reward_paid_at")
    private LocalDateTime rewardPaidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Jobseeker getReferrer() { return referrer; }
    public void setReferrer(Jobseeker referrer) { this.referrer = referrer; }

    public Jobseeker getReferee() { return referee; }
    public void setReferee(Jobseeker referee) { this.referee = referee; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getHiredAt() { return hiredAt; }
    public void setHiredAt(LocalDateTime hiredAt) { this.hiredAt = hiredAt; }

    public LocalDateTime getRewardPaidAt() { return rewardPaidAt; }
    public void setRewardPaidAt(LocalDateTime rewardPaidAt) { this.rewardPaidAt = rewardPaidAt; }
}
