package com.example.controller;
import java.security.Principal;

import com.example.dto.JobseekerProfileDto;
import com.example.dto.JobseekerRegistrationDto;
import com.example.dto.ResumeUploadDto;
import com.example.model.*;
import com.example.repositary.JobInviteRepository;
import com.example.service.*;
import com.example.repositary.JobseekerProfileRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import com.example.repositary.JobseekerRepository;
import com.example.repositary.ResumeRepository;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.example.service.PasswordResetService;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/jobseeker")
public class JobseekerController {

    @Autowired
    private JobseekerService jobseekerService;

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private JobseekerProfileRepository profileRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JobRecommendationService recommendationService;

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private JobInviteRepository inviteRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitService rateLimitService;

    @GetMapping("/register")
    public String showRegisterForm(@RequestParam(required = false) String email,
                                   @RequestParam(required = false) String error,
                                   Model model) {
        JobseekerRegistrationDto dto = new JobseekerRegistrationDto();
        if (email != null) {
            dto.setEmail(email);
        }
        model.addAttribute("jobseekerRegistrationDto", dto);
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "jobseeker/register";
    }

    @PostMapping("/register")
    public String registerJobseeker(@ModelAttribute("jobseekerRegistrationDto") @Valid JobseekerRegistrationDto dto,
                                    BindingResult result,
                                    Model model) {
        if (result.hasErrors()) {
            return "jobseeker/register";
        }
        try {
            Jobseeker created = jobseekerService.registerJobseeker(dto);
            otpService.generateAndSendOtp(created.getEmail());
            model.addAttribute("email", created.getEmail());
            return "jobseeker/verify-otp";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "jobseeker/register";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtp(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "jobseeker/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String handleVerifyOtp(@RequestParam("email") String email,
                                  @RequestParam("code") String code,
                                  Model model,
                                  HttpSession session,
                                  HttpServletRequest request) {
        // Rate limit: 5 OTP attempts per 10 minutes per email
        String rateLimitKey = "otp:" + email.toLowerCase();
        if (!rateLimitService.isAllowed(rateLimitKey, 5, 600)) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Too many attempts. Please wait 10 minutes before trying again.");
            return "jobseeker/verify-otp";
        }

        boolean ok = otpService.verifyOtp(email, code);
        if (!ok) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Invalid or expired OTP. Please request a new one.");
            return "jobseeker/verify-otp";
        }
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElseThrow();
        jobseeker.setEmailVerified(true);
        jobseeker.setVerifiedAt(LocalDateTime.now());
        jobseekerRepository.save(jobseeker);

        // Store email in session to secure the onboarding flow (prevents URL param hijacking)
        session.setAttribute("pending_onboarding_email", email);
        return "redirect:/jobseeker/onboarding";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "jobseeker/login";
    }

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "jobseeker/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String email,
                                       HttpServletRequest request,
                                       Model model) {
        // Rate limit: 3 requests per hour per IP
        String ip = request.getRemoteAddr();
        if (!rateLimitService.isAllowed("forgot-pwd:" + ip, 3, 3600)) {
            model.addAttribute("error", "Too many requests. Please try again later.");
            return "jobseeker/forgot-password";
        }

        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                   ? ":" + request.getServerPort() : "");
        passwordResetService.initiateReset(email, baseUrl);
        // Always show the same message to avoid user enumeration
        model.addAttribute("message", "If that email is registered, you will receive a reset link shortly.");
        return "jobseeker/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPassword(@RequestParam String token, Model model) {
        if (!passwordResetService.isValidToken(token)) {
            model.addAttribute("error", "This reset link is invalid or has expired. Please request a new one.");
            return "jobseeker/forgot-password";
        }
        model.addAttribute("token", token);
        return "jobseeker/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String token,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Passwords do not match.");
            return "jobseeker/reset-password";
        }
        if (password.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "jobseeker/reset-password";
        }
        boolean success = passwordResetService.resetPassword(token, password);
        if (!success) {
            model.addAttribute("error", "This reset link is invalid or has expired. Please request a new one.");
            return "jobseeker/forgot-password";
        }
        return "redirect:/jobseeker/login?passwordReset=true";
    }

    @GetMapping("/onboarding")
    public String showOnboarding(HttpSession session,
                                 Principal principal,
                                 Model model) {
        // Resolve email securely: prefer authenticated principal, then session token
        String email = resolveOnboardingEmail(session, principal);
        if (email == null) return "redirect:/jobseeker/register";

        // Fetch jobseeker and profile
        Jobseeker jobseeker = jobseekerRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));

        JobseekerProfile profile = profileRepository
                .findByJobseeker(jobseeker)
                .orElseGet(() -> {
                    JobseekerProfile p = new JobseekerProfile();
                    p.setJobseeker(jobseeker);
                    p.setId(jobseeker.getId());
                    return p;
                });

        model.addAttribute("email", email);
        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("profile", profile);
        model.addAttribute("profileDto", new JobseekerProfileDto());

        return "jobseeker/onboarding";
    }

    @PostMapping("/onboarding")
    public String handleOnboarding(HttpSession session,
                                   Principal principal,
                                   @ModelAttribute("profileDto") @Valid JobseekerProfileDto dto,
                                   BindingResult result,
                                   Model model) {
        String email = resolveOnboardingEmail(session, principal);
        if (email == null) return "redirect:/jobseeker/register";

        if (result.hasErrors()) {
            Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Jobseeker not found"));
            JobseekerProfile profile = profileRepository.findByJobseeker(jobseeker)
                    .orElseGet(() -> { JobseekerProfile p = new JobseekerProfile(); p.setJobseeker(jobseeker); return p; });
            model.addAttribute("jobseeker", jobseeker);
            model.addAttribute("profile", profile);
            model.addAttribute("email", email);
            return "jobseeker/onboarding";
        }

        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));
        jobseekerService.updateProfile(jobseeker, dto);

        return "redirect:/jobseeker/resume-onboarding";
    }

    @GetMapping("/resume-onboarding")
    public String showResumeOnboarding(HttpSession session, Principal principal, Model model) {
        String email = resolveOnboardingEmail(session, principal);
        if (email == null) return "redirect:/jobseeker/register";

        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));
        model.addAttribute("email", email);
        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("resumeUploadDto", new ResumeUploadDto());
        return "jobseeker/resume-onboarding";
    }

    @PostMapping("/resume-onboarding")
    public String handleResumeOnboarding(HttpSession session,
                                         Principal principal,
                                         @ModelAttribute("resumeUploadDto") @Valid ResumeUploadDto dto,
                                         BindingResult result,
                                         Model model) throws IOException {
        String email = resolveOnboardingEmail(session, principal);
        if (email == null) return "redirect:/jobseeker/register";

        if (result.hasErrors()) {
            Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Jobseeker not found"));
            model.addAttribute("email", email);
            model.addAttribute("jobseeker", jobseeker);
            return "jobseeker/resume-onboarding";
        }

        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));

        if (dto.getFile().isEmpty()) {
            model.addAttribute("error", "Please select a resume file to upload.");
            model.addAttribute("email", email);
            model.addAttribute("jobseeker", jobseeker);
            return "jobseeker/resume-onboarding";
        }

        String contentType = dto.getFile().getContentType();
        if (contentType == null ||
                !(contentType.equals("application/pdf") ||
                  contentType.equals("application/msword") ||
                  contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            model.addAttribute("error", "Only PDF or Word documents are allowed.");
            model.addAttribute("email", email);
            model.addAttribute("jobseeker", jobseeker);
            return "jobseeker/resume-onboarding";
        }

        String fileName = fileStorageService.storeFile(dto.getFile(), jobseeker.getId());

        Resume resume = resumeRepository.findById(jobseeker.getId())
                .orElseGet(() -> {
                    Resume r = new Resume();
                    r.setJobseeker(jobseeker);
                    r.setId(jobseeker.getId());
                    return r;
                });
        resume.setFileName(fileName);
        resume.setFilePath(Paths.get(System.getProperty("user.dir"), "uploads", "resumes").resolve(fileName).toString());
        resume.setJobTypes(dto.getJobTypes());
        resume.setIndustries(dto.getIndustries());
        resumeRepository.save(resume);

        jobseeker.setResumeUploaded(true);
        jobseekerRepository.save(jobseeker);

        // Clear onboarding session marker and redirect to login
        session.removeAttribute("pending_onboarding_email");
        return "redirect:/jobseeker/login?registered=true";
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {

        // If user is not logged in → redirect to login
        if (principal == null) {
            return "redirect:/jobseeker/login";
        }

        String email = principal.getName();

        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));

        List<JobInvite> invites = inviteRepository.findByJobseeker(jobseeker);
        List<JobInvite> pendingInvites = invites.stream()
                .filter(inv -> inv.getStatus() == InviteStatus.PENDING)
                .collect(Collectors.toList());

        model.addAttribute("invites", pendingInvites);

        if (!jobseeker.isProfileCompleted()) {
            return "redirect:/jobseeker/onboarding?email=" + email;
        }

        if (!jobseeker.isResumeUploaded()) {
            return "redirect:/jobseeker/resume-onboarding?email=" + email;
        }

        JobseekerProfile profile = profileRepository.findByJobseeker(jobseeker).orElse(null);
        List<Job> recommended = recommendationService.recommendJobs(profile);
        List<Application> applications = applicationService.findByJobseeker(jobseeker);

        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("profile", profile);
        model.addAttribute("recommendedJobs", recommended);
        model.addAttribute("applications", applications);

        int completionPercent = onboardingService.getCompletionPercentage(jobseeker, profile);
        model.addAttribute("completionPercent", completionPercent);

        return "jobseeker/dashboard";
    }

    @GetMapping("/interview-requests")
    public String viewInterviewRequests(Principal principal, Model model) {
        if (principal == null) return "redirect:/jobseeker/login";
        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElseThrow();

        List<JobInvite> invites = inviteRepository.findByJobseeker(jobseeker);
        List<JobInvite> pendingInvites = invites.stream()
                .filter(inv -> inv.getStatus() == InviteStatus.PENDING)
                .collect(Collectors.toList());

        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("invites", pendingInvites);
        return "jobseeker/interview-requests";
    }

    @GetMapping("/active-interviews")
    public String viewActiveInterviews(Principal principal, Model model) {
        if (principal == null) return "redirect:/jobseeker/login";
        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElseThrow();

        List<JobInvite> invites = inviteRepository.findByJobseeker(jobseeker);
        List<JobInvite> activeInvites = invites.stream()
                .filter(inv -> inv.getStatus() == InviteStatus.ACCEPTED)
                .collect(Collectors.toList());

        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("invites", activeInvites);
        return "jobseeker/active-interviews";
    }

    @GetMapping("/account")
    public String viewAccountSettings(Principal principal,
                                      @RequestParam(required = false) String success,
                                      @RequestParam(required = false) String error,
                                      Model model) {
        if (principal == null) return "redirect:/jobseeker/login";
        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElseThrow();
        model.addAttribute("jobseeker", jobseeker);
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
        return "jobseeker/account";
    }

    @PostMapping("/account/change-password")
    public String changePassword(Principal principal,
                                  @RequestParam String currentPassword,
                                  @RequestParam String newPassword,
                                  @RequestParam String confirmPassword) {
        if (principal == null) return "redirect:/jobseeker/login";
        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElseThrow();

        if ("GOOGLE".equals(jobseeker.getAuthProvider())) {
            return "redirect:/jobseeker/account?error=Google+accounts+cannot+change+password+here";
        }
        if (!passwordEncoder.matches(currentPassword, jobseeker.getPassword())) {
            return "redirect:/jobseeker/account?error=Current+password+is+incorrect";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/jobseeker/account?error=New+passwords+do+not+match";
        }
        if (newPassword.length() < 9) {
            return "redirect:/jobseeker/account?error=Password+must+be+at+least+9+characters";
        }
        jobseeker.setPassword(passwordEncoder.encode(newPassword));
        jobseekerRepository.save(jobseeker);
        return "redirect:/jobseeker/account?success=Password+changed+successfully";
    }

    @PostMapping("/account/change-name")
    public String changeName(Principal principal, @RequestParam String fullName) {
        if (principal == null) return "redirect:/jobseeker/login";
        if (fullName == null || fullName.isBlank()) {
            return "redirect:/jobseeker/account?error=Name+cannot+be+blank";
        }
        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElseThrow();
        jobseeker.setFullName(fullName.trim());
        jobseekerRepository.save(jobseeker);
        return "redirect:/jobseeker/account?success=Name+updated+successfully";
    }

    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam String email, Model model) {
        jobseekerRepository.findByEmail(email.trim().toLowerCase()).ifPresent(js -> {
            if (!js.isEmailVerified()) {
                otpService.resendOtp(email.trim().toLowerCase());
            }
        });
        model.addAttribute("email", email);
        model.addAttribute("info", "A new OTP has been sent to your email.");
        return "jobseeker/verify-otp";
    }

    // ─── Resume download (own resume) ───

    @GetMapping("/resume/download")
    @ResponseBody
    public ResponseEntity<Resource> downloadOwnResume(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElseThrow();
        Resume resume = resumeRepository.findById(jobseeker.getId()).orElse(null);
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

    @GetMapping("/refer-and-earn")
    public String viewReferAndEarn(Principal principal, Model model) {
        if (principal == null) return "redirect:/jobseeker/login";
        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElseThrow();
        model.addAttribute("jobseeker", jobseeker);
        return "jobseeker/refer-and-earn";
    }

    @GetMapping("/profile")
    public String showProfile(Principal principal, Model model) {

        if (principal == null) {
            return "redirect:/jobseeker/login";
        }

        String email = principal.getName();

        Jobseeker jobseeker = jobseekerRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));

        JobseekerProfile profile = profileRepository
                .findByJobseeker(jobseeker)
                .orElseGet(() -> {
                    JobseekerProfile p = new JobseekerProfile();
                    p.setJobseeker(jobseeker);
                    p.setId(jobseeker.getId());
                    return p;
                });

        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("profile", profile);

        return "jobseeker/profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/jobseeker/login";
        }

        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));

        JobseekerProfile profile = profileRepository.findByJobseeker(jobseeker)
                .orElse(new JobseekerProfile());

        JobseekerProfileDto dto = new JobseekerProfileDto();
        dto.setExperienceYears(profile.getExperienceYears());
        dto.setExperienceMonths(profile.getExperienceMonths());
        dto.setCurrentCompany(profile.getCurrentCompany());
        dto.setCurrentRole(profile.getCurrentRole());
        dto.setCurrentCtc(profile.getCurrentCtc());
        dto.setExpectedCtc(profile.getExpectedCtc());
        dto.setSkills(profile.getSkills());
        dto.setPrimarySkills(profile.getPrimarySkills());
        dto.setHighestEducation(profile.getHighestEducation());
        dto.setInstitution(profile.getInstitution());
        dto.setFieldOfStudy(profile.getFieldOfStudy());
        dto.setGraduationYear(profile.getGraduationYear());
        dto.setPreferredLocations(profile.getPreferredLocations());
        dto.setWorkMode(profile.getWorkMode());
        dto.setNoticePeriodDays(profile.getNoticePeriodDays());

        model.addAttribute("profileDto", dto);
        return "jobseeker/profile-edit";
    }

    @PostMapping("/profile/edit")
    public String handleEditProfile(Principal principal,
                                    @ModelAttribute("profileDto") @Valid JobseekerProfileDto dto,
                                    BindingResult result,
                                    Model model) {
        if (principal == null) {
            return "redirect:/jobseeker/login";
        }

        if (result.hasErrors()) {
            return "jobseeker/profile-edit";
        }

        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));

        jobseekerService.updateProfile(jobseeker, dto);

        return "redirect:/jobseeker/profile";
    }

    @GetMapping("/resume")
    public String showResumeUpload(Principal principal, Model model) {

        if (principal == null) {
            return "redirect:/jobseeker/login";
        }

        model.addAttribute("resumeUploadDto", new ResumeUploadDto());
        return "jobseeker/resume-upload";
    }

    @PostMapping("/resume")
    public String handleResumeUpload(Principal principal,
                                     @ModelAttribute("resumeUploadDto") @Valid ResumeUploadDto dto,
                                     BindingResult result,
                                     Model model) throws IOException {

        if (principal == null) {
            return "redirect:/jobseeker/login";
        }

        if (result.hasErrors()) {
            return "jobseeker/resume-upload";
        }

        String email = principal.getName();

        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));

        if (dto.getFile().isEmpty()) {
            model.addAttribute("error", "Please select a resume file to upload.");
            return "jobseeker/resume-upload";
        }

        String contentType = dto.getFile().getContentType();
        if (contentType == null ||
                !(contentType.equals("application/pdf") ||
                  contentType.equals("application/msword") ||
                  contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            model.addAttribute("error", "Only PDF or Word documents are allowed.");
            return "jobseeker/resume-upload";
        }

        String fileName = fileStorageService.storeFile(dto.getFile(), jobseeker.getId());

        Resume resume = resumeRepository.findById(jobseeker.getId())
                .orElseGet(() -> {
                    Resume r = new Resume();
                    r.setJobseeker(jobseeker);
                    r.setId(jobseeker.getId());
                    return r;
                });
        resume.setFileName(fileName);
        resume.setFilePath(Paths.get(System.getProperty("user.dir"), "uploads", "resumes").resolve(fileName).toString());
        resume.setJobTypes(dto.getJobTypes());
        resume.setIndustries(dto.getIndustries());
        resumeRepository.save(resume);

        jobseeker.setResumeUploaded(true);
        jobseekerRepository.save(jobseeker);

        return "redirect:/jobseeker/dashboard";
    }

    @PostMapping("/invite/{id}/accept")
    public String acceptInvite(@PathVariable Long id) {

        JobInvite invite = inviteRepository.findById(id).orElseThrow();

        invite.setStatus(InviteStatus.ACCEPTED);
        inviteRepository.save(invite);

        applicationService.apply(invite.getJob(), invite.getJobseeker(), "invite");

        return "redirect:/jobseeker/dashboard";
    }

    @PostMapping("/invite/{id}/decline")
    public String declineInvite(@PathVariable Long id) {
        JobInvite invite = inviteRepository.findById(id).orElseThrow();
        invite.setStatus(InviteStatus.DECLINED);
        inviteRepository.save(invite);
        return "redirect:/jobseeker/dashboard";
    }

    // ─── Private helpers ───

    /**
     * Resolves the email for the onboarding flow securely.
     * Priority: authenticated principal > session token.
     * Never trusts a URL parameter.
     */
    private String resolveOnboardingEmail(HttpSession session, Principal principal) {
        if (principal != null) {
            return principal.getName();
        }
        return (String) session.getAttribute("pending_onboarding_email");
    }
}
