
package com.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class JobseekerProfileDto {

    @Size(max = 500, message = "Headline must not exceed 500 characters")
    private String profileHeadline;

    @Size(max = 2000, message = "About must not exceed 2000 characters")
    private String about;

    @NotNull(message = "Years of experience is required")
    @Min(value = 0, message = "Experience cannot be negative")
    private Integer experienceYears;

    @Min(value = 0, message = "Months cannot be negative")
    private Integer experienceMonths;

    private String currentCompany;

    private String pastCompanies;

    private String currentRole;

    @Min(value = 0, message = "Current CTC cannot be negative")
    private Double currentCtc;

    @NotNull(message = "Expected CTC is required")
    @Min(value = 0, message = "Expected CTC cannot be negative")
    private Double expectedCtc;

    @NotBlank(message = "Skills are required")
    private String skills;

    @NotBlank(message = "Primary skills are required")
    private String primarySkills;

    @NotBlank(message = "Highest education is required")
    private String highestEducation;

    private String institution;

    private String fieldOfStudy;

    private Integer graduationYear;

    @NotBlank(message = "Preferred locations are required")
    private String preferredLocations;

    @NotEmpty(message = "Work mode is required")
    private List<String> workMode;

    private Integer noticePeriodDays;

    private Boolean servingNotice;

    private LocalDate noticeStartDate;

    // ===== GETTERS & SETTERS =====

    public String getProfileHeadline() {
        return profileHeadline;
    }

    public void setProfileHeadline(String profileHeadline) {
        this.profileHeadline = profileHeadline;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getPastCompanies() {
        return pastCompanies;
    }

    public void setPastCompanies(String pastCompanies) {
        this.pastCompanies = pastCompanies;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public Integer getExperienceMonths() {
        return experienceMonths;
    }

    public void setExperienceMonths(Integer experienceMonths) {
        this.experienceMonths = experienceMonths;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    public Double getCurrentCtc() {
        return currentCtc;
    }

    public void setCurrentCtc(Double currentCtc) {
        this.currentCtc = currentCtc;
    }

    public Double getExpectedCtc() {
        return expectedCtc;
    }

    public void setExpectedCtc(Double expectedCtc) {
        this.expectedCtc = expectedCtc;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getPrimarySkills() {
        return primarySkills;
    }

    public void setPrimarySkills(String primarySkills) {
        this.primarySkills = primarySkills;
    }

    public String getHighestEducation() {
        return highestEducation;
    }

    public void setHighestEducation(String highestEducation) {
        this.highestEducation = highestEducation;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getFieldOfStudy() {
        return fieldOfStudy;
    }

    public void setFieldOfStudy(String fieldOfStudy) {
        this.fieldOfStudy = fieldOfStudy;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getPreferredLocations() {
        return preferredLocations;
    }

    public void setPreferredLocations(String preferredLocations) {
        this.preferredLocations = preferredLocations;
    }

    public List<String> getWorkMode() {
        return workMode;
    }

    public void setWorkMode(List<String> workMode) {
        this.workMode = workMode;
    }

    public Integer getNoticePeriodDays() {
        return noticePeriodDays;
    }

    public void setNoticePeriodDays(Integer noticePeriodDays) {
        this.noticePeriodDays = noticePeriodDays;
    }

    public Boolean getServingNotice() {
        return servingNotice;
    }

    public void setServingNotice(Boolean servingNotice) {
        this.servingNotice = servingNotice;
    }

    public LocalDate getNoticeStartDate() {
        return noticeStartDate;
    }

    public void setNoticeStartDate(LocalDate noticeStartDate) {
        this.noticeStartDate = noticeStartDate;
    }

}
