package com.example.admin.controller;

import com.example.admin.dto.*;
import com.example.model.Job;
import com.example.model.Jobseeker;
import com.example.model.recruiter.Recruiter;
import com.example.repository.ApplicationRepository;
import com.example.repository.JobRepository;
import com.example.repository.JobseekerRepository;
import com.example.repository.recruiter.RecruiterRepository;
import com.example.service.EmailService;
import com.example.service.EventLogService;
import com.example.service.ReferralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class AdminApiController {

    @Autowired private JobseekerRepository jobseekerRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private EventLogService eventLogService;
    @Autowired private EmailService emailService;
    @Autowired private ReferralService referralService;

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
        size = Math.min(size, 100); // SECURITY: cap page size to prevent memory DoS
        Page<Jobseeker> result = jobseekerRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<AdminUserResponse> body = result.getContent().stream()
                .map(AdminUserResponse::from).toList();
        return ResponseEntity.ok(body);
    }

    @Transactional
    @PutMapping("/users/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id,
                                          java.security.Principal principal) {
        Jobseeker js = jobseekerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        js.setApprovalStatus("APPROVED");
        js.setRejectionReason(null);
        js.setApprovedAt(java.time.LocalDateTime.now());
        jobseekerRepository.save(js);
        eventLogService.log("admin_approve_user", null, id, null,
                "Admin " + adminName(principal) + " approved user " + js.getEmail());
        try { emailService.sendProfileApproved(js.getEmail(), js.getFullName()); }
        catch (Exception e) { /* email failure must not roll back the approval */ }
        try { referralService.onProfileApproved(js); }
        catch (Exception e) { /* referral update must not roll back the approval */ }
        return ResponseEntity.ok(Map.of("message", "User approved"));
    }

    @Transactional
    @PutMapping("/users/{id}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable Long id,
                                         @RequestParam(required = false) String reason,
                                         java.security.Principal principal) {
        Jobseeker js = jobseekerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        js.setApprovalStatus("REJECTED");
        js.setRejectedAt(java.time.LocalDateTime.now());
        // SECURITY: limit rejection reason to 500 chars to prevent abuse
        if (reason != null && reason.length() > 500) {
            reason = reason.substring(0, 500);
        }
        js.setRejectionReason(reason);
        jobseekerRepository.save(js);
        eventLogService.log("admin_reject_user", null, id, null,
                "Admin " + adminName(principal) + " rejected user " + js.getEmail());
        try { emailService.sendProfileRejected(js.getEmail(), js.getFullName(), reason); }
        catch (Exception e) { /* email failure must not roll back the rejection */ }
        return ResponseEntity.ok(Map.of("message", "User rejected"));
    }

    @Transactional
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

    @Transactional
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

    @Transactional
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
        size = Math.min(size, 100);
        // Uses JOIN FETCH to eagerly load recruiter + company, avoiding LazyInitializationException
        Page<Job> result = jobRepository.findAllWithRecruiter(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "postedDate")));
        List<AdminJobResponse> body = result.getContent().stream()
                .map(AdminJobResponse::from).toList();
        return ResponseEntity.ok(body);
    }

    @Transactional
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
        size = Math.min(size, 100);
        Page<Recruiter> result = recruiterRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<AdminRecruiterResponse> body = result.getContent().stream()
                .map(AdminRecruiterResponse::from).toList();
        return ResponseEntity.ok(body);
    }

    @Transactional
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
        try { emailService.sendRecruiterApproved(r.getEmail(), r.getName()); }
        catch (Exception e) { /* email failure must not roll back the approval */ }
        return ResponseEntity.ok(Map.of("message", "Recruiter approved"));
    }

    @Transactional
    @PutMapping("/recruiters/{id}/reject")
    public ResponseEntity<?> rejectRecruiter(@PathVariable Long id,
                                              java.security.Principal principal) {
        Recruiter r = recruiterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
        r.setActivationStatus("Rejected");
        recruiterRepository.save(r);
        eventLogService.log("admin_reject_recruiter", null, null, id,
                "Admin " + adminName(principal) + " rejected recruiter " + r.getEmail());
        try { emailService.sendRecruiterRejected(r.getEmail(), r.getName()); }
        catch (Exception e) { /* email failure must not roll back the rejection */ }
        return ResponseEntity.ok(Map.of("message", "Recruiter rejected"));
    }

    @Transactional
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

    // ─── Referrals ───

    @GetMapping("/referrals")
    public ResponseEntity<List<Map<String, Object>>> listReferrals(
            @RequestParam(defaultValue = "all") String status) {
        var referrals = "pending".equals(status)
                ? referralService.getPendingRewards()
                : referralService.getAllReferrals();
        List<Map<String, Object>> body = referrals.stream().map(r -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("referrerName", r.getReferrer().getFullName());
            m.put("referrerEmail", r.getReferrer().getEmail());
            m.put("refereeName", r.getReferee().getFullName());
            m.put("refereeEmail", r.getReferee().getEmail());
            m.put("status", r.getStatus());
            m.put("createdAt", r.getCreatedAt());
            m.put("hiredAt", r.getHiredAt());
            m.put("rewardPaidAt", r.getRewardPaidAt());
            return m;
        }).toList();
        return ResponseEntity.ok(body);
    }

    @Transactional
    @PutMapping("/referrals/{id}/mark-paid")
    public ResponseEntity<?> markReferralPaid(@PathVariable Long id,
                                               java.security.Principal principal) {
        boolean success = referralService.markRewardPaid(id);
        if (!success) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Referral not found or not in HIRED status"));
        }
        eventLogService.log("admin_referral_paid", null, null, null,
                "Admin " + adminName(principal) + " marked referral #" + id + " reward as paid");
        return ResponseEntity.ok(Map.of("message", "Reward marked as paid"));
    }

    private static String adminName(java.security.Principal principal) {
        return principal != null ? principal.getName() : "unknown";
    }
}