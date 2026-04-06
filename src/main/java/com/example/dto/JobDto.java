package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class JobDto {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Job description is required")
    @Size(max = 4000, message = "Description must not exceed 4000 characters")
    private String description;

    @NotBlank(message = "Required skills are required")
    @Size(max = 1000, message = "Required skills must not exceed 1000 characters")
    private String requiredSkills;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Work mode is required")
    private String workMode; // ONSITE, REMOTE, HYBRID

    private String jobType; // FULL_TIME, PART_TIME, CONTRACT

    @NotNull(message = "Minimum experience is required")
    private Integer experienceMin;

    @NotNull(message = "Maximum experience is required")
    private Integer experienceMax;

    @NotNull(message = "Minimum salary is required")
    private Integer salaryMin;

    @NotNull(message = "Maximum salary is required")
    private Integer salaryMax;

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

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
}
