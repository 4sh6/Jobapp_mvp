package com.example.admin.dto;

import com.example.model.Job;
import java.time.LocalDateTime;

public class AdminJobResponse {
    private Long id;
    private String title;
    private String location;
    private String workMode;
    private String jobType;
    private String status;
    private boolean active;
    private Integer salaryMin;
    private Integer salaryMax;
    private Integer experienceMin;
    private Integer experienceMax;
    private String recruiterName;
    private String companyName;
    private LocalDateTime postedDate;

    public static AdminJobResponse from(Job j) {
        AdminJobResponse dto = new AdminJobResponse();
        dto.id = j.getId();
        dto.title = j.getTitle();
        dto.location = j.getLocation();
        dto.workMode = j.getWorkMode();
        dto.jobType = j.getJobType();
        dto.status = j.getStatus();
        dto.active = j.isActive();
        dto.salaryMin = j.getSalaryMin();
        dto.salaryMax = j.getSalaryMax();
        dto.experienceMin = j.getExperienceMin();
        dto.experienceMax = j.getExperienceMax();
        dto.postedDate = j.getPostedDate();
        if (j.getRecruiter() != null) {
            dto.recruiterName = j.getRecruiter().getName();
            dto.companyName = j.getRecruiter().getCompany() != null
                    ? j.getRecruiter().getCompany().getName() : null;
        }
        return dto;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public String getWorkMode() { return workMode; }
    public String getJobType() { return jobType; }
    public String getStatus() { return status; }
    public boolean isActive() { return active; }
    public Integer getSalaryMin() { return salaryMin; }
    public Integer getSalaryMax() { return salaryMax; }
    public Integer getExperienceMin() { return experienceMin; }
    public Integer getExperienceMax() { return experienceMax; }
    public String getRecruiterName() { return recruiterName; }
    public String getCompanyName() { return companyName; }
    public LocalDateTime getPostedDate() { return postedDate; }
}