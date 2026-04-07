package com.example.admin.controller;

import com.example.admin.dto.*;
import com.example.model.Job;
import com.example.model.Jobseeker;
import com.example.model.recruiter.Recruiter;
import com.example.repositary.ApplicationRepository;
import com.example.repositary.JobRepository;
import com.example.repositary.JobseekerRepository;
import com.example.repositary.recruiter.RecruiterRepository;
import com.example.service.EventLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    @Autowired private JobseekerRepository jobseekerRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private EventLogService eventLogService;

    // ─── Dashboard ───

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> dashboard() {
        long users = jobseekerRepository.count();
        long recruiters = recruiterRepository.count();
        long jobs = jobRepository.count();
        long applications = applicationRepository.count();
        long pendingRecruiters = recruiterRepository.countByActivationStatus("Pending");

        return ResponseEntity.ok(
                new AdminDashboardResponse(users, recruiters, jobs, applications, pendingRecruiters));
    }

    // ─── Users (Jobseekers) ───

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Jobseeker> result = jobseekerRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<AdminUserResponse> body = result.getContent().stream()
                .map(AdminUserResponse::from).toList();
        return ResponseEntity.ok(body);
    }

    @PutMapping("/users/{id}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long id,
                                        java.security.Principal principal) {
        Jobseeker js = jobseekerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        js.setActive(false);
        jobseekerRepository.save(js);
        eventLogService.log("admin_block_user", null, id, null,
                "Admin " + adminName(principal) + " blocked user " + js.getEmail());
        return ResponseEntity.ok(Map.of("message", "User blocked"));
    }

    @PutMapping("/users/{id}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable Long id,
                                          java.security.Principal principal) {
        Jobseeker js = jobseekerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        js.setActive(true);
        jobseekerRepository.save(js);
        eventLogService.log("admin_unblock_user", null, id, null,
                "Admin " + adminName(principal) + " unblocked user " + js.getEmail());
        return ResponseEntity.ok(Map.of("message", "User unblocked"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                         java.security.Principal principal) {
        Jobseeker js = jobseekerRepository.findById(id).orElse(null);
        if (js == null) return ResponseEntity.notFound().build();
        eventLogService.log("admin_delete_user", null, id, null,
                "Admin " + adminName(principal) + " deleted user " + js.getEmail());
        jobseekerRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    // ─── Jobs ───

    @GetMapping("/jobs")
    public ResponseEntity<List<AdminJobResponse>> listJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Job> result = jobRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "postedDate")));
        List<AdminJobResponse> body = result.getContent().stream()
                .map(AdminJobResponse::from).toList();
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id,
                                        java.security.Principal principal) {
        Job job = jobRepository.findById(id).orElse(null);
        if (job == null) return ResponseEntity.notFound().build();
        eventLogService.log("admin_delete_job", id, null, null,
                "Admin " + adminName(principal) + " deleted job: " + job.getTitle());
        jobRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Job deleted"));
    }

    // ─── Recruiters ───

    @GetMapping("/recruiters")
    public ResponseEntity<List<AdminRecruiterResponse>> listRecruiters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Recruiter> result = recruiterRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<AdminRecruiterResponse> body = result.getContent().stream()
                .map(AdminRecruiterResponse::from).toList();
        return ResponseEntity.ok(body);
    }

    @PutMapping("/recruiters/{id}/approve")
    public ResponseEntity<?> approveRecruiter(@PathVariable Long id,
                                               java.security.Principal principal) {
        Recruiter r = recruiterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
        r.setActivationStatus("Active");
        r.setSuspended(false);
        recruiterRepository.save(r);
        eventLogService.log("admin_approve_recruiter", null, null, id,
                "Admin " + adminName(principal) + " approved recruiter " + r.getEmail());
        return ResponseEntity.ok(Map.of("message", "Recruiter approved"));
    }

    @PutMapping("/recruiters/{id}/reject")
    public ResponseEntity<?> rejectRecruiter(@PathVariable Long id,
                                              java.security.Principal principal) {
        Recruiter r = recruiterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
        r.setActivationStatus("Rejected");
        recruiterRepository.save(r);
        eventLogService.log("admin_reject_recruiter", null, null, id,
                "Admin " + adminName(principal) + " rejected recruiter " + r.getEmail());
        return ResponseEntity.ok(Map.of("message", "Recruiter rejected"));
    }

    @PutMapping("/recruiters/{id}/suspend")
    public ResponseEntity<?> suspendRecruiter(@PathVariable Long id,
                                               java.security.Principal principal) {
        Recruiter r = recruiterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
        r.setSuspended(true);
        recruiterRepository.save(r);
        eventLogService.log("admin_suspend_recruiter", null, null, id,
                "Admin " + adminName(principal) + " suspended recruiter " + r.getEmail());
        return ResponseEntity.ok(Map.of("message", "Recruiter suspended"));
    }

    private static String adminName(java.security.Principal principal) {
        return principal != null ? principal.getName() : "unknown";
    }
}