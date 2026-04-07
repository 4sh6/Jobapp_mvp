package com.example.controller.recruiter;

import com.example.dto.JobDto;
import com.example.dto.RecruiterRegistrationDto;
import com.example.model.*;
import com.example.model.recruiter.Recruiter;
import com.example.repositary.ApplicationRepository;
import com.example.repositary.CompanyRepository;
import com.example.repositary.JobInviteRepository;
import com.example.repositary.JobseekerProfileRepository;
import com.example.repositary.JobseekerRepository;
import com.example.repositary.ResumeRepository;
import com.example.repositary.recruiter.RecruiterRepository;
import com.example.service.ApplicationService;
import com.example.service.EmailService;
import com.example.service.FileStorageService;
import com.example.service.JobService;
import com.example.service.recruiter.RecruiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/recruiter")
public class RecruiterController {

    @Autowired private RecruiterService recruiterService;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JobService jobService;
    @Autowired private ApplicationService applicationService;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private JobseekerProfileRepository profileRepository;
    @Autowired private JobInviteRepository inviteRepository;
    @Autowired private JobseekerRepository jobseekerRepository;
    @Autowired private EmailService emailService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private ResumeRepository resumeRepository;

    // ─── Helper: load recruiter and enforce activation + suspension ───

    private Recruiter getActiveRecruiter(UserDetails userDetails) {
        Recruiter recruiter = recruiterRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (recruiter.isSuspended()) {
            throw new IllegalStateException("Your account has been suspended. Contact support.");
        }
        if (!"Active".equals(recruiter.getActivationStatus())) {
            throw new IllegalStateException("Your account is pending admin approval.");
        }
        return recruiter;
    }

    /** Returns recruiter regardless of status — for read-only/profile pages */
    private Recruiter getRecruiter(UserDetails userDetails) {
        return recruiterRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    // ──────────────────────────── Auth ────────────────────────────

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("recruiterRegistrationDto", new RecruiterRegistrationDto());
        return "recruiter/register";
    }

    @PostMapping("/register")
    public String registerRecruiter(@ModelAttribute("recruiterRegistrationDto") @Valid RecruiterRegistrationDto dto,
                                    BindingResult result, Model model) {
        if (result.hasErrors()) return "recruiter/register";
        try {
            recruiterService.registerRecruiter(dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "recruiter/register";
        }
        return "recruiter/signup-success";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "recruiter/login";
    }

    // ──────────────────────────── Dashboard ────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
                            @PageableDefault(size = 10, sort = "postedDate", direction = Sort.Direction.DESC) Pageable pageable,
                            Model model) {
        if (userDetails == null) return "redirect:/recruiter/login";

        Recruiter recruiter = getRecruiter(userDetails);

        // If account is not active yet, show a pending page instead of crashing
        if (!"Active".equals(recruiter.getActivationStatus())) {
            model.addAttribute("recruiter", recruiter);
            model.addAttribute("status", recruiter.getActivationStatus());
            return "recruiter/pending-approval";
        }

        Page<Job> jobsPage = jobService.listJobsByRecruiter(recruiter, pageable);

        Map<Long, Integer> applicationCounts = new HashMap<>();
        for (Job job : jobsPage.getContent()) {
            applicationCounts.put(job.getId(), applicationService.findByJob(job).size());
        }

        long totalJobs        = jobsPage.getTotalElements();
        long totalApplicants  = applicationRepository.countByJobRecruiter(recruiter);
        long totalInvitesSent = inviteRepository.countByJob_Recruiter(recruiter);
        long totalHired       = applicationRepository.countHiredByJobRecruiter(recruiter);

        model.addAttribute("recruiter", recruiter);
        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("applicationCounts", applicationCounts);
        model.addAttribute("totalJobs", totalJobs);
        model.addAttribute("totalApplicants", totalApplicants);
        model.addAttribute("totalInvitesSent", totalInvitesSent);
        model.addAttribute("totalHired", totalHired);
        return "recruiter/dashboard";
    }

    // ──────────────────────────── Job CRUD ────────────────────────────

    @GetMapping("/jobs/new")
    public String newJobForm(@AuthenticationPrincipal UserDetails userDetails, Model model,
                             RedirectAttributes ra) {
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
                            BindingResult result,
                            RedirectAttributes ra) {
        if (result.hasErrors()) return "recruiter/job-form";
        Recruiter recruiter;
        try { recruiter = getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recruiter/dashboard";
        }
        jobService.createJob(jobDto, recruiter);
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
                            BindingResult result, Model model,
                            RedirectAttributes ra) {
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

    // ──────────────────────────── Applications ────────────────────────────

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
                               RedirectAttributes ra) {
        // Verify recruiter owns this application's job
        Recruiter recruiter = getRecruiter(userDetails);
        Application app = applicationService.findById(id).orElseThrow();

        if (!app.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            ra.addFlashAttribute("error", "Unauthorized: you do not own this job.");
            return "redirect:/recruiter/dashboard";
        }

        app.setStatus(status);
        applicationService.save(app);
        ra.addFlashAttribute("success", "Status updated to " + status + ".");
        return "redirect:/recruiter/jobs/" + app.getJob().getId() + "/applications";
    }

    // ──────────────────────────── Resume Download ────────────────────────────

    @GetMapping("/resume/{jobseekerId}")
    @ResponseBody
    public ResponseEntity<Resource> downloadResume(@AuthenticationPrincipal UserDetails userDetails,
                                                   @PathVariable Long jobseekerId) {
        // Only active recruiters can download resumes
        try { getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) {
            return ResponseEntity.status(403).build();
        }

        Resume resume = resumeRepository.findById(jobseekerId).orElse(null);
        if (resume == null || resume.getFileName() == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = fileStorageService.loadFileAsResource(resume.getFileName());
        String contentType = resume.getFileName().toLowerCase().endsWith(".pdf")
                ? "application/pdf" : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resume.getFileName() + "\"")
                .body(resource);
    }

    // ──────────────────────────── Candidate Discovery ────────────────────────────

    @GetMapping("/jobs/{id}/candidates")
    public String shortlistForJob(@PathVariable Long id,
                                  @RequestParam(defaultValue = "") String skills,
                                  @RequestParam(defaultValue = "0") Integer expMin,
                                  @RequestParam(defaultValue = "50") Integer expMax,
                                  @PageableDefault(size = 12) Pageable pageable,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model, RedirectAttributes ra) {
        Recruiter recruiter;
        try { recruiter = getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recruiter/dashboard";
        }
        Job job = jobService.findById(id).orElseThrow();

        String searchSkills = skills.isBlank() ? (job.getRequiredSkills() != null ? job.getRequiredSkills() : "") : skills;
        int searchExpMin    = (expMin == 0 && skills.isBlank()) ? (job.getExperienceMin() != null ? job.getExperienceMin() : 0) : expMin;
        int searchExpMax    = (expMax == 50 && skills.isBlank()) ? (job.getExperienceMax() != null ? job.getExperienceMax() : 50) : expMax;

        Page<JobseekerProfile> candidatesPage = profileRepository.findMatchingCandidates(
                searchSkills, searchExpMin, searchExpMax, pageable);

        model.addAttribute("job", job);
        model.addAttribute("candidatesPage", candidatesPage);
        model.addAttribute("filterSkills", searchSkills);
        model.addAttribute("filterExpMin", searchExpMin);
        model.addAttribute("filterExpMax", searchExpMax);
        model.addAttribute("recruiter", recruiter);
        return "recruiter/candidates";
    }

    @GetMapping("/candidates")
    public String browseCandidates(@RequestParam(defaultValue = "") String skills,
                                   @RequestParam(defaultValue = "0") Integer expMin,
                                   @RequestParam(defaultValue = "100") Integer expMax,
                                   @PageableDefault(size = 12) Pageable pageable,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Model model, RedirectAttributes ra) {
        Recruiter recruiter;
        try { recruiter = getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recruiter/dashboard";
        }
        Page<JobseekerProfile> candidatesPage = profileRepository.browseWithFilters(skills, expMin, expMax, pageable);
        model.addAttribute("candidatesPage", candidatesPage);
        model.addAttribute("filterSkills", skills);
        model.addAttribute("filterExpMin", expMin);
        model.addAttribute("filterExpMax", expMax);
        model.addAttribute("recruiter", recruiter);
        return "recruiter/browse-candidates";
    }

    // ──────────────────────────── Invites ────────────────────────────

    @PostMapping("/jobs/{jobId}/invite/{jobseekerId}")
    public String invite(@PathVariable Long jobId,
                         @PathVariable Long jobseekerId,
                         @AuthenticationPrincipal UserDetails userDetails,
                         HttpServletRequest request,
                         RedirectAttributes ra) {
        Recruiter recruiter;
        try { recruiter = getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recruiter/dashboard";
        }
        Job job = jobService.findById(jobId).orElseThrow();

        // Only the recruiter who owns the job can invite
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            ra.addFlashAttribute("error", "Unauthorized: you do not own this job.");
            return "redirect:/recruiter/jobs/" + jobId + "/candidates";
        }

        Jobseeker js = jobseekerRepository.findById(jobseekerId).orElseThrow();

        if (inviteRepository.existsByJobAndJobseeker(job, js)) {
            ra.addFlashAttribute("error", "An invitation for this candidate has already been sent.");
            return "redirect:/recruiter/jobs/" + jobId + "/candidates";
        }

        JobInvite invite = new JobInvite();
        invite.setJob(job);
        invite.setJobseeker(js);
        inviteRepository.save(invite);

        // Email notification to candidate
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                   ? ":" + request.getServerPort() : "");
        String companyName = recruiter.getCompany() != null ? recruiter.getCompany().getName() : "A company";
        emailService.sendInviteNotification(
                js.getEmail(), js.getFullName(),
                job.getTitle(), companyName,
                baseUrl + "/jobseeker/interview-requests");

        ra.addFlashAttribute("success", "Interview invitation sent to " + js.getFullName() + "!");
        return "redirect:/recruiter/jobs/" + jobId + "/candidates";
    }

    @GetMapping("/invites")
    public String viewSentInvites(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Recruiter recruiter = getRecruiter(userDetails);
        List<JobInvite> invites = inviteRepository.findByJob_RecruiterOrderByCreatedAtDesc(recruiter);
        model.addAttribute("invites", invites);
        model.addAttribute("recruiter", recruiter);
        return "recruiter/invites";
    }

    // ──────────────────────────── Company Profile ────────────────────────────

    @GetMapping("/company")
    public String viewCompanyProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Recruiter recruiter = getRecruiter(userDetails);
        model.addAttribute("recruiter", recruiter);
        model.addAttribute("company", recruiter.getCompany());
        return "recruiter/company-profile";
    }

    @PostMapping("/company")
    public String updateCompanyProfile(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestParam String companyName,
                                       @RequestParam(required = false) String website,
                                       @RequestParam(required = false) String industry,
                                       @RequestParam(required = false) String sizeBand,
                                       @RequestParam(required = false) String description,
                                       @RequestParam(required = false) String jobTitle,
                                       @RequestParam(required = false) String phoneNumber,
                                       RedirectAttributes ra) {
        Recruiter recruiter = getRecruiter(userDetails);
        Company company = recruiter.getCompany();
        if (company != null) {
            if (companyName != null && !companyName.isBlank()) company.setName(companyName.trim());
            company.setWebsite(website);
            company.setIndustry(industry);
            company.setSizeBand(sizeBand);
            company.setDescription(description);
            companyRepository.save(company);
        }
        if (jobTitle != null && !jobTitle.isBlank()) recruiter.setJobTitle(jobTitle.trim());
        if (phoneNumber != null && !phoneNumber.isBlank()) recruiter.setPhoneNumber(phoneNumber.trim());
        recruiterRepository.save(recruiter);
        ra.addFlashAttribute("success", "Company profile updated.");
        return "redirect:/recruiter/company";
    }
}