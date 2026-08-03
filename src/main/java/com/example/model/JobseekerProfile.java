package com.example.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "jobseeker_profiles")
public class JobseekerProfile {

    @Id
    @Column(name = "jobseeker_id")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "jobseeker_id")
    private Jobseeker jobseeker;

    @Column(name = "mobile_number", length = 15)
    private String mobileNumber;

    @Column(name = "profile_headline", length = 500)
    private String profileHeadline;

    @Column(name = "about", columnDefinition = "TEXT")
    private String about;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "experience_months")
    private Integer experienceMonths;

    @Column(name = "current_company", length = 500)
    private String currentCompany;

    @Column(name = "past_companies", length = 1000)
    private String pastCompanies;

    @Column(name = "\"current_role\"", length = 500)
    private String currentRole;

    @Column(name = "current_ctc")
    private Double currentCtc;

    @Column(name = "expected_ctc")
    private Double expectedCtc;

    @Column(length = 1000)
    private String skills;

    @Column(name = "primary_skills", length = 1000)
    private String primarySkills;

    @Column(name = "highest_education", length = 500)
    private String highestEducation;

    @Column(length = 500)
    private String institution;

    @Column(name = "field_of_study", length = 500)
    private String fieldOfStudy;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "preferred_locations", length = 1000)
    private String preferredLocations;

    @Column(name = "work_mode", length = 500)
    private String workMode;

    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @Column(name = "serving_notice")
    private Boolean servingNotice;

    @Column(name = "notice_start_date")
    private LocalDate noticeStartDate;


    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Jobseeker getJobseeker() {
        return jobseeker;
    }

    public void setJobseeker(Jobseeker jobseeker) {
        this.jobseeker = jobseeker;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

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

    public String getPastCompanies() {
        return pastCompanies;
    }

    public void setPastCompanies(String pastCompanies) {
        this.pastCompanies = pastCompanies;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    public Double getCurrentCtc() { return currentCtc; }
    public void setCurrentCtc(Double currentCtc) { this.currentCtc = currentCtc; }

    public Double getExpectedCtc() { return expectedCtc; }
    public void setExpectedCtc(Double expectedCtc) { this.expectedCtc = expectedCtc; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getPrimarySkills() { return primarySkills; }
    public void setPrimarySkills(String primarySkills) { this.primarySkills = primarySkills; }

    public String getHighestEducation() { return highestEducation; }
    public void setHighestEducation(String highestEducation) { this.highestEducation = highestEducation; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getFieldOfStudy() { return fieldOfStudy; }
    public void setFieldOfStudy(String fieldOfStudy) { this.fieldOfStudy = fieldOfStudy; }

    public Integer getGraduationYear() { return graduationYear; }
    public void setGraduationYear(Integer graduationYear) { this.graduationYear = graduationYear; }

    public String getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(String preferredLocations) { this.preferredLocations = preferredLocations; }

    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }

    public Integer getNoticePeriodDays() { return noticePeriodDays; }
    public void setNoticePeriodDays(Integer noticePeriodDays) { this.noticePeriodDays = noticePeriodDays; }

    public Boolean getServingNotice() { return servingNotice; }
    public void setServingNotice(Boolean servingNotice) { this.servingNotice = servingNotice; }

    public LocalDate getNoticeStartDate() { return noticeStartDate; }
    public void setNoticeStartDate(LocalDate noticeStartDate) { this.noticeStartDate = noticeStartDate; }

    /** Days left in notice, if currently serving. Null when not serving or start date/notice length is unknown. */
    @Transient
    public Integer getNoticePeriodRemainingDays() {
        if (!Boolean.TRUE.equals(servingNotice) || noticeStartDate == null || noticePeriodDays == null) {
            return null;
        }
        long elapsed = ChronoUnit.DAYS.between(noticeStartDate, LocalDate.now());
        long remaining = noticePeriodDays - elapsed;
        return (int) Math.max(0, remaining);
    }

    /** Effective notice length right now: remaining days if serving, otherwise the full notice period. */
    @Transient
    public Integer getEffectiveNoticeDays() {
        Integer remaining = getNoticePeriodRemainingDays();
        return remaining != null ? remaining : noticePeriodDays;
    }

    /** A candidate is an Immediate Joiner when their effective notice is 30 days or less. */
    @Transient
    public boolean isImmediateJoiner() {
        Integer effective = getEffectiveNoticeDays();
        return effective != null && effective <= 30;
    }

}