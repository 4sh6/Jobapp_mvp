package com.example.admin.dto;

import com.example.model.recruiter.Recruiter;
import java.time.LocalDateTime;

public class AdminRecruiterResponse {
    private Long id;
    private String name;
    private String email;
    private String companyName;
    private String jobTitle;
    private String phoneNumber;
    private String activationStatus;
    private String plan;
    private boolean suspended;
    private LocalDateTime createdAt;

    public static AdminRecruiterResponse from(Recruiter r) {
        AdminRecruiterResponse dto = new AdminRecruiterResponse();
        dto.id = r.getId();
        dto.name = r.getName();
        dto.email = r.getEmail();
        dto.companyName = r.getCompany() != null ? r.getCompany().getName() : null;
        dto.jobTitle = r.getJobTitle();
        dto.phoneNumber = r.getPhoneNumber();
        dto.activationStatus = r.getActivationStatus();
        dto.plan = r.getPlan();
        dto.suspended = r.isSuspended();
        dto.createdAt = r.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getCompanyName() { return companyName; }
    public String getJobTitle() { return jobTitle; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getActivationStatus() { return activationStatus; }
    public String getPlan() { return plan; }
    public boolean isSuspended() { return suspended; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}