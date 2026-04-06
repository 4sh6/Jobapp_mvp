
package com.example.model;

import com.example.model.recruiter.Recruiter;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(name = "required_skills", length = 1000)
    private String requiredSkills;

    @Column(name = "location")
    private String location;

    @Column(name = "work_mode")
    private String workMode; // ONSITE, REMOTE, HYBRID

    @Column(name = "job_type")
    private String jobType; // FULL_TIME, PART_TIME, CONTRACT

    @Column(name = "experience_min")
    private Integer experienceMin;

    @Column(name = "experience_max")
    private Integer experienceMax;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(nullable = false)
    private String status = "PUBLISHED"; // DRAFT, PUBLISHED, CLOSED

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "featured")
    private boolean featured = false;

    @Column(name = "posted_date")
    private LocalDateTime postedDate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;

    @PrePersist
    protected void onCreate() {
        if (postedDate == null) {
            postedDate = LocalDateTime.now();
        }
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWorkMode() {
        return workMode;
    }

    public void setWorkMode(String workMode) {
        this.workMode = workMode;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public Integer getExperienceMin() {
        return experienceMin;
    }

    public void setExperienceMin(Integer experienceMin) {
        this.experienceMin = experienceMin;
    }

    public Integer getExperienceMax() {
        return experienceMax;
    }

    public void setExperienceMax(Integer experienceMax) {
        this.experienceMax = experienceMax;
    }

    public Integer getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(Integer salaryMin) {
        this.salaryMin = salaryMin;
    }

    public Integer getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(Integer salaryMax) {
        this.salaryMax = salaryMax;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(LocalDateTime postedDate) {
        this.postedDate = postedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public Recruiter getRecruiter() {
        return recruiter;
    }

    public void setRecruiter(Recruiter recruiter) {
        this.recruiter = recruiter;
    }
}
