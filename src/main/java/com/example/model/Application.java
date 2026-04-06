package com.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "jobseeker_id"}))
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "jobseeker_id")
    private Jobseeker jobseeker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "applied_date")
    private LocalDateTime appliedDate;

    @Column(name = "source")
    private String source;

    @PrePersist
    protected void onCreate() {
        appliedDate = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public Jobseeker getJobseeker() {
        return jobseeker;
    }

    public void setJobseeker(Jobseeker jobseeker) {
        this.jobseeker = jobseeker;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDateTime appliedDate) {
        this.appliedDate = appliedDate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}