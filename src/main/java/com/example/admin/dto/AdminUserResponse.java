package com.example.admin.dto;

import com.example.model.Jobseeker;
import java.time.LocalDateTime;

public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private boolean emailVerified;
    private boolean resumeUploaded;
    private boolean active;
    private String authProvider;
    private LocalDateTime createdAt;

    public static AdminUserResponse from(Jobseeker js) {
        AdminUserResponse r = new AdminUserResponse();
        r.id = js.getId();
        r.fullName = js.getFullName();
        r.email = js.getEmail();
        r.emailVerified = js.isEmailVerified();
        r.resumeUploaded = js.isResumeUploaded();
        r.active = js.isActive();
        r.authProvider = js.getAuthProvider();
        r.createdAt = js.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean isResumeUploaded() { return resumeUploaded; }
    public boolean isActive() { return active; }
    public String getAuthProvider() { return authProvider; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}