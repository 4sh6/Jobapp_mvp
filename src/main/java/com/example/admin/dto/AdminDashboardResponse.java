package com.example.admin.dto;

public class AdminDashboardResponse {
    private long totalUsers;
    private long totalRecruiters;
    private long totalJobs;
    private long totalApplications;
    private long pendingRecruiters;

    public AdminDashboardResponse(long totalUsers, long totalRecruiters, long totalJobs,
                                   long totalApplications, long pendingRecruiters) {
        this.totalUsers = totalUsers;
        this.totalRecruiters = totalRecruiters;
        this.totalJobs = totalJobs;
        this.totalApplications = totalApplications;
        this.pendingRecruiters = pendingRecruiters;
    }

    public long getTotalUsers() { return totalUsers; }
    public long getTotalRecruiters() { return totalRecruiters; }
    public long getTotalJobs() { return totalJobs; }
    public long getTotalApplications() { return totalApplications; }
    public long getPendingRecruiters() { return pendingRecruiters; }
}