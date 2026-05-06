package com.example.controller.recruiter;

import com.example.dto.JobDto;
import com.example.model.*;
import com.example.model.recruiter.Recruiter;
import com.example.repository.JobInviteRepository;
import com.example.repository.ResumeRepository;
import com.example.service.ApplicationService;
import com.example.service.EmailService;
import com.example.service.FileStorageService;
import com.example.service.JobService;
import com.example.service.ReferralService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/recruiter")
public class RecruiterJobController extends RecruiterBaseController {

    @Autowired private JobService jobService;
    @Autowired private ApplicationService applicationService;
    @Autowired private JobInviteRepository inviteRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private EmailService emailService;
    @Autowired private ReferralService referralService;

    @GetMapping("/jobs/new")
    public String newJobForm(@AuthenticationPrincipal UserDetails userDetails, Model model, RedirectAttributes ra) {
        try { getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recruiter/dashboard";
        }
        model.addAttribute("jobDto", new JobDto());
        return "recruiter/job-form";
    }

    @PostMapping("/jobs")
    public String createJob(@AuthenticationPrincipal UserDetails userDetails,
                            @ModelAttribute("jobDto") @Valid JobDto jobDto,
                            BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) return "recruiter/job-form";
        Recruiter recruiter;
        try { recruiter = getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recruiter/dashboard";
        }
        try {
            jobService.createJob(jobDto, recruiter);
        } catch (IllegalArgumentException e) {
            result.rejectValue("salaryMin", "range", e.getMessage());
            return "recruiter/job-form";
        }
        ra.addFlashAttribute("success", "Job posted successfully!");
        return "redirect:/recruiter/dashboard";
    }

    @GetMapping("/jobs/{id}/edit")
    public String editJobForm(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable Long id, Model model, RedirectAttributes ra) {
        Recruiter recruiter;
        try { recruiter = getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recruiter/dashboard";
        }
        Job job = jobService.findById(id).orElseThrow();
        if (job.getRecruiter() == null || !job.getRecruiter().getId().equals(recruiter.getId())) {
            return "redirect:/recruiter/dashboard";
        }
        JobDto dto = new JobDto();
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setLocation(job.getLocation());
        dto.setRequiredSkills(job.getRequiredSkills());
        dto.setWorkMode(job.getWorkMode());
        dto.setJobType(job.getJobType());
        dto.setExperienceMin(job.getExperienceMin());
        dto.setExperienceMax(job.getExperienceMax());
        dto.setSalaryMin(job.getSalaryMin());
        dto.setSalaryMax(job.getSalaryMax());
        model.addAttribute("jobDto", dto);
        model.addAttribute("jobId", id);
        model.addAttribute("recruiter", recruiter);
        return "recruiter/job-edit";
    }

    @PostMapping("/jobs/{id}")
    public String updateJob(@AuthenticationPrincipal UserDetails userDetails,
                            @PathVariable Long id,
                            @ModelAttribute("jobDto") @Valid JobDto jobDto,
                            BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("jobId", id);
            return "recruiter/job-edit";
        }
        Recruiter recruiter;
        try { recruiter = getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recruiter/dashboard";
        }
        try {
            jobService.updateJob(id, jobDto, recruiter);
            ra.addFlashAttribute("success", "Job updated successfully!");
        } catch (IllegalArgumentException e) {
            model.addAttribute("jobId", id);
            model.addAttribute("error", e.getMessage());
            return "recruiter/job-edit";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recruiter/dashboard";
    }

    @PostMapping("/jobs/{id}/close")
    public String closeJob(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Long id, RedirectAttributes ra) {
        Recruiter recruiter;
        try { recruiter = getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recruiter/dashboard";
        }
        Job job = jobService.findById(id).orElseThrow();
        if (job.getRecruiter() == null || !job.getRecruiter().getId().equals(recruiter.getId())) {
            return "redirect:/recruiter/dashboard";
        }
        jobService.closeJob(job);
        ra.addFlashAttribute("success", "Job closed.");
        return "redirect:/recruiter/dashboard";
    }

    @GetMapping("/jobs/{id}/applications")
    public String viewApplicants(@AuthenticationPrincipal UserDetails userDetails,
                                 @PathVariable Long id, Model model) {
        Recruiter recruiter = getRecruiter(userDetails);
        Job job = jobService.findById(id).orElseThrow();
        if (job.getRecruiter() == null || !job.getRecruiter().getId().equals(recruiter.getId())) {
            return "redirect:/recruiter/dashboard";
        }
        List<Application> applications = applicationService.findByJob(job);
        model.addAttribute("job", job);
        model.addAttribute("applications", applications);
        model.addAttribute("recruiter", recruiter);
        return "recruiter/applications";
    }

    @PostMapping("/applications/{id}/status")
    public String updateStatus(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Long id,
                               @RequestParam ApplicationStatus status,
                               HttpServletRequest request,
                               RedirectAttributes ra) {
        Recruiter recruiter = getRecruiter(userDetails);
        Application app = applicationService.findById(id).orElseThrow();

        if (!app.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            ra.addFlashAttribute("error", "Unauthorized: you do not own this job.");
            return "redirect:/recruiter/dashboard";
        }

        app.setStatus(status);
        applicationService.save(app);

        // Advance referral lifecycle when candidate is hired
        if (status == ApplicationStatus.HIRED) {
            try { referralService.onHired(app.getJobseeker()); }
            catch (Exception e) { /* referral update must not break the status change */ }
        }

        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                   ? ":" + request.getServerPort() : "");
        String companyName = recruiter.getCompany() != null ? recruiter.getCompany().getName() : "A company";
        emailService.sendStatusUpdateEmail(
                app.getJobseeker().getEmail(),
                app.getJobseeker().getFullName(),
                app.getJob().getTitle(),
                companyName,
                status.name(),
                baseUrl + "/jobseeker/applications");

        ra.addFlashAttribute("success", "Status updated to " + status + ".");
        return "redirect:/recruiter/jobs/" + app.getJob().getId() + "/applications";
    }

    @GetMapping("/resume/{jobseekerId}")
    @ResponseBody
    public ResponseEntity<Resource> downloadResume(@AuthenticationPrincipal UserDetails userDetails,
                                                   @PathVariable Long jobseekerId) {
        Recruiter recruiter;
        try { recruiter = getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) { return ResponseEntity.status(403).build(); }

        boolean contactUnlocked = inviteRepository.existsByJobseekerIdAndJob_RecruiterAndStatus(
                jobseekerId, recruiter, InviteStatus.ACCEPTED);
        if (!contactUnlocked) return ResponseEntity.status(403).build();

        Resume resume = resumeRepository.findById(jobseekerId).orElse(null);
        if (resume == null || resume.getFileName() == null) return ResponseEntity.notFound().build();

        Resource resource = fileStorageService.loadFileAsResource(resume.getFileName());
        String contentType = resume.getFileName().toLowerCase().endsWith(".pdf")
                ? "application/pdf" : "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resume.getFileName() + "\"")
                .body(resource);
    }
}
