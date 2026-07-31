
package com.example.admin.controller;

import com.example.constant.ActivationStatus;
import com.example.constant.ApprovalStatus;
import com.example.model.Jobseeker;
import com.example.model.JobseekerProfile;
import com.example.model.Resume;
import com.example.model.recruiter.Recruiter;
import com.example.repository.EventLogRepository;
import com.example.repository.JobseekerProfileRepository;
import com.example.repository.JobseekerRepository;
import com.example.repository.ResumeRepository;
import com.example.repository.recruiter.RecruiterRepository;
import com.example.service.EmailService;
import com.example.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private JobseekerRepository jobseekerRepository;
    @Autowired private JobseekerProfileRepository profileRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private EventLogRepository eventLogRepository;
    @Autowired private EmailService emailService;

    @PostConstruct
    public void backfillApprovalStatus() {
        jobseekerRepository.backfillNullApprovalStatus();
    }

    @GetMapping("/login")
    public String adminLogin() {
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long jobseekerCount  = jobseekerRepository.count();
        long recruiterCount  = recruiterRepository.count();
        long views           = eventLogRepository.count();
        long pendingCount    = jobseekerRepository.countByApprovalStatus(ApprovalStatus.PENDING);
        long approvedCount   = jobseekerRepository.countByApprovalStatus(ApprovalStatus.APPROVED);
        long hiredCount      = jobseekerRepository.countByApprovalStatus(ApprovalStatus.HIRED);

        model.addAttribute("jobseekerCount", jobseekerCount);
        model.addAttribute("recruiterCount", recruiterCount);
        model.addAttribute("eventCount", views);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("hiredCount", hiredCount);
        return "admin/dashboard";
    }

    // ─── Candidate review queue ───────────────────────────────────────────────

    @GetMapping("/jobseekers")
    public String listJobseekers(@RequestParam(defaultValue = "PENDING") String status, Model model) {
        List<Jobseeker> jobseekers = jobseekerRepository.findByApprovalStatusOrderByCreatedAtDesc(status);
        model.addAttribute("jobseekers", jobseekers);
        model.addAttribute("activeTab", status);
        model.addAttribute("pendingCount",  jobseekerRepository.countByApprovalStatus(ApprovalStatus.PENDING));
        model.addAttribute("approvedCount", jobseekerRepository.countByApprovalStatus(ApprovalStatus.APPROVED));
        model.addAttribute("rejectedCount", jobseekerRepository.countByApprovalStatus(ApprovalStatus.REJECTED));
        model.addAttribute("hiredCount",    jobseekerRepository.countByApprovalStatus(ApprovalStatus.HIRED));
        return "admin/jobseekers";
    }

    @GetMapping("/jobseekers/{id}")
    public String viewCandidate(@PathVariable Long id, Model model) {
        Jobseeker js = jobseekerRepository.findById(id).orElseThrow();
        Optional<JobseekerProfile> profile = profileRepository.findByJobseeker(js);
        model.addAttribute("candidate", js);
        model.addAttribute("profile", profile.orElse(null));
        return "admin/candidate-review";
    }

    @GetMapping("/jobseekers/{id}/resume")
    @ResponseBody
    public ResponseEntity<Resource> downloadCandidateResume(@PathVariable Long id) {
        Resume resume = resumeRepository.findById(id).orElse(null);
        if (resume == null || resume.getFileName() == null) return ResponseEntity.notFound().build();

        Resource resource = fileStorageService.loadFileAsResource(resume.getFileName());
        String contentType = resume.getFileName().toLowerCase().endsWith(".pdf")
                ? "application/pdf" : "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resume.getFileName() + "\"")
                .body(resource);
    }

    @PostMapping("/jobseekers/{id}/approve")
    public String approveCandidate(@PathVariable Long id, RedirectAttributes ra) {
        Jobseeker js = jobseekerRepository.findById(id).orElseThrow();
        js.setApprovalStatus(ApprovalStatus.APPROVED);
        js.setRejectionReason(null);
        jobseekerRepository.save(js);
        emailService.sendProfileApproved(js.getEmail(), js.getFullName());
        ra.addFlashAttribute("success", js.getFullName() + " approved and notified.");
        return "redirect:/admin/jobseekers?status=PENDING";
    }

    @PostMapping("/jobseekers/{id}/reject")
    public String rejectCandidate(@PathVariable Long id,
                                  @RequestParam(required = false) String reason,
                                  RedirectAttributes ra) {
        Jobseeker js = jobseekerRepository.findById(id).orElseThrow();
        js.setApprovalStatus(ApprovalStatus.REJECTED);
        js.setRejectionReason(reason);
        jobseekerRepository.save(js);
        emailService.sendProfileRejected(js.getEmail(), js.getFullName(), reason);
        ra.addFlashAttribute("success", js.getFullName() + " rejected and notified.");
        return "redirect:/admin/jobseekers?status=PENDING";
    }

    @PostMapping("/jobseekers/{id}/hire")
    public String markHired(@PathVariable Long id, RedirectAttributes ra) {
        Jobseeker js = jobseekerRepository.findById(id).orElseThrow();
        js.setApprovalStatus(ApprovalStatus.HIRED);
        jobseekerRepository.save(js);
        ra.addFlashAttribute("success", js.getFullName() + " marked as hired.");
        return "redirect:/admin/jobseekers?status=APPROVED";
    }

    // ─── Recruiters ───────────────────────────────────────────────────────────

    @GetMapping("/recruiters")
    public String listRecruiters(Model model) {
        List<Recruiter> recruiters = recruiterRepository.findAll();
        model.addAttribute("recruiters", recruiters);
        return "admin/recruiters";
    }

    @PostMapping("/recruiters/{id}/approve")
    public String approveRecruiter(@PathVariable Long id, RedirectAttributes ra) {
        Recruiter recruiter = recruiterRepository.findById(id).orElseThrow();
        recruiter.setActivationStatus(ActivationStatus.ACTIVE);
        recruiterRepository.save(recruiter);
        emailService.sendRecruiterApproved(recruiter.getEmail(), recruiter.getName());
        ra.addFlashAttribute("success", recruiter.getName() + " approved and notified.");
        return "redirect:/admin/recruiters";
    }

    @PostMapping("/recruiters/{id}/reject")
    public String rejectRecruiter(@PathVariable Long id, RedirectAttributes ra) {
        Recruiter recruiter = recruiterRepository.findById(id).orElseThrow();
        recruiter.setActivationStatus(ActivationStatus.REJECTED);
        recruiterRepository.save(recruiter);
        emailService.sendRecruiterRejected(recruiter.getEmail(), recruiter.getName());
        ra.addFlashAttribute("success", recruiter.getName() + " rejected and notified.");
        return "redirect:/admin/recruiters";
    }
}
