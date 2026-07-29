package com.example.controller.jobseeker;

import com.example.dto.JobseekerProfileDto;
import com.example.dto.ResumeUploadDto;
import com.example.model.*;
import com.example.repository.JobInviteRepository;
import com.example.repository.JobseekerProfileRepository;
import com.example.repository.JobseekerRepository;
import com.example.repository.ResumeRepository;
import com.example.service.*;
import com.example.service.ReferralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/jobseeker")
public class JobseekerDashboardController {

    @Autowired private JobseekerRepository jobseekerRepository;
    @Autowired private JobseekerProfileRepository profileRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private JobInviteRepository inviteRepository;
    @Autowired private JobseekerService jobseekerService;
    @Autowired private JobRecommendationService recommendationService;
    @Autowired private ApplicationService applicationService;
    @Autowired private OnboardingService onboardingService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ReferralService referralService;

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        Boolean showWelcomeBanner = (Boolean) model.getAttribute("showWelcomeBanner");
        if (principal == null) return "redirect:/jobseeker/login";

        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));

        List<JobInvite> allInvites = inviteRepository.findByJobseeker(jobseeker);
        List<JobInvite> pendingInvites = allInvites.stream()
                .filter(inv -> inv.getStatus() == InviteStatus.PENDING)
                .filter(inv -> inv.getJob().isActive()) // hide invites whose job was closed
                .collect(Collectors.toList());

        if (!jobseeker.isProfileCompleted()) {
            return "redirect:/jobseeker/onboarding?email=" + email;
        }
        if (!jobseeker.isResumeUploaded()) {
            return "redirect:/jobseeker/resume-onboarding?email=" + email;
        }

        JobseekerProfile profile = profileRepository.findByJobseeker(jobseeker).orElse(null);
        List<Job> recommended = recommendationService.recommendJobs(profile);
        List<Application> applications = applicationService.findByJobseeker(jobseeker);

        int completionPercent = onboardingService.getCompletionPercentage(jobseeker, profile);
        boolean needsSave = false;

        // Show "Profile Approved" banner for first 3 dashboard visits after approval
        boolean showApprovalBanner = false;
        if ("APPROVED".equals(jobseeker.getApprovalStatus())
                && jobseeker.getApprovalBannerShownCount() < 3) {
            showApprovalBanner = true;
            jobseeker.setApprovalBannerShownCount(jobseeker.getApprovalBannerShownCount() + 1);
            needsSave = true;
        }

        // Show "Profile Complete!" banner exactly once when profile first reaches 100%
        boolean showProfileCompleteBanner = false;
        if (completionPercent >= 100 && !jobseeker.isProfileCompleteBannerShown()) {
            showProfileCompleteBanner = true;
            jobseeker.setProfileCompleteBannerShown(true);
            needsSave = true;
        }

        if (needsSave) jobseekerRepository.save(jobseeker);

        // Rank pending invites: preference-matching ones first, mismatches last
        List<JobInvite> rankedInvites = rankInvitesByPreference(pendingInvites, profile);
        // Build a set of invite IDs that are outside candidate preferences
        List<Long> outsidePrefIds = pendingInvites.stream()
                .filter(inv -> !isPreferenceMatch(inv, profile))
                .map(JobInvite::getId)
                .collect(Collectors.toList());

        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("profile", profile);
        model.addAttribute("invites", rankedInvites);
        model.addAttribute("outsidePrefIds", outsidePrefIds);
        model.addAttribute("pendingInviteCount", pendingInvites.size());
        model.addAttribute("recommendedJobs", recommended);
        model.addAttribute("applications", applications);
        model.addAttribute("completionPercent", completionPercent);
        model.addAttribute("showApprovalBanner", showApprovalBanner);
        model.addAttribute("showProfileCompleteBanner", showProfileCompleteBanner);
        model.addAttribute("showWelcomeBanner", Boolean.TRUE.equals(showWelcomeBanner));
        return "jobseeker/dashboard";
    }

    @GetMapping("/profile")
    public String showProfile(Principal principal, Model model) {
        if (principal == null) return "redirect:/jobseeker/login";

        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));
        JobseekerProfile profile = profileRepository.findByJobseeker(jobseeker)
                .orElseGet(() -> {
                    JobseekerProfile p = new JobseekerProfile();
                    p.setJobseeker(jobseeker);
                    return p;
                });

        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("profile", profile);
        return "jobseeker/profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Principal principal, Model model) {
        if (principal == null) return "redirect:/jobseeker/login";

        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName())
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
        if (profile.getWorkMode() != null && !profile.getWorkMode().isBlank()) {
            dto.setWorkMode(Arrays.stream(profile.getWorkMode().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList()));
        }
        dto.setNoticePeriodDays(profile.getNoticePeriodDays());
        dto.setServingNotice(profile.getServingNotice());
        dto.setNoticeStartDate(profile.getNoticeStartDate());

        model.addAttribute("profileDto", dto);
        return "jobseeker/profile-edit";
    }

    @PostMapping("/profile/edit")
    public String handleEditProfile(Principal principal,
                                    @ModelAttribute("profileDto") @Valid JobseekerProfileDto dto,
                                    BindingResult result,
                                    Model model) {
        if (principal == null) return "redirect:/jobseeker/login";
        if (result.hasErrors()) return "jobseeker/profile-edit";

        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));
        jobseekerService.updateProfile(jobseeker, dto);

        // If editing causes profile to drop below 100%, reset banner so it shows again when re-completed
        JobseekerProfile updatedProfile = profileRepository.findByJobseeker(jobseeker).orElse(null);
        if (onboardingService.getCompletionPercentage(jobseeker, updatedProfile) < 100) {
            jobseeker.setProfileCompleteBannerShown(false);
            jobseekerRepository.save(jobseeker);
        }

        return "redirect:/jobseeker/profile";
    }

    @GetMapping("/profile-details")
    public String showProfileDetails(Principal principal, Model model) {
        if (principal == null) return "redirect:/jobseeker/login";
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("jobseekerId", jobseeker.getId());
        return "jobseeker/profiledetails";
    }

    @PostMapping("/profile-details")
    public String saveProfileDetails(Principal principal,
                                     @RequestParam(required = false) String experienceYears,
                                     @RequestParam(required = false) String experienceMonths,
                                     @RequestParam(required = false) String currentCompany,
                                     @RequestParam(required = false) String currentRole,
                                     @RequestParam(required = false) String currentCtc,
                                     @RequestParam(required = false) String expectedCtc,
                                     @RequestParam(required = false) String primarySkills,
                                     @RequestParam(required = false) String highestEducation,
                                     @RequestParam(required = false) String institution,
                                     @RequestParam(required = false) String fieldOfStudy,
                                     @RequestParam(required = false) String graduationYear,
                                     @RequestParam(required = false) List<String> preferredLocations,
                                     @RequestParam(required = false) List<String> workMode,
                                     @RequestParam(required = false) String noticePeriodDays,
                                     @RequestParam(required = false) Boolean servingNotice,
                                     @RequestParam(required = false) String noticeStartDate,
                                     RedirectAttributes ra) {
        if (principal == null) return "redirect:/jobseeker/login";
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();

        JobseekerProfile profile = profileRepository.findByJobseeker(jobseeker)
                .orElseGet(() -> { JobseekerProfile p = new JobseekerProfile(); p.setJobseeker(jobseeker); return p; });

        if (experienceYears != null && !experienceYears.isBlank()) {
            try { profile.setExperienceYears(Integer.parseInt(experienceYears.replace("+", "").trim())); }
            catch (NumberFormatException ignored) {}
        }
        if (experienceMonths != null && !experienceMonths.isBlank()) {
            try { profile.setExperienceMonths(Integer.parseInt(experienceMonths.trim())); }
            catch (NumberFormatException ignored) {}
        }
        if (currentCompany != null && !currentCompany.isBlank()) profile.setCurrentCompany(currentCompany.trim());
        if (currentRole    != null && !currentRole.isBlank())    profile.setCurrentRole(currentRole.trim());
        if (currentCtc     != null && !currentCtc.isBlank()) {
            try { profile.setCurrentCtc(Double.parseDouble(currentCtc.trim())); } catch (NumberFormatException ignored) {}
        }
        if (expectedCtc != null && !expectedCtc.isBlank()) {
            try { profile.setExpectedCtc(Double.parseDouble(expectedCtc.trim())); } catch (NumberFormatException ignored) {}
        }
        if (primarySkills    != null && !primarySkills.isBlank())    profile.setPrimarySkills(primarySkills.trim());
        if (highestEducation != null && !highestEducation.isBlank()) profile.setHighestEducation(highestEducation.trim());
        if (institution      != null && !institution.isBlank())      profile.setInstitution(institution.trim());
        if (fieldOfStudy     != null && !fieldOfStudy.isBlank())     profile.setFieldOfStudy(fieldOfStudy.trim());
        if (graduationYear   != null && !graduationYear.isBlank()) {
            try { profile.setGraduationYear(Integer.parseInt(graduationYear.trim())); } catch (NumberFormatException ignored) {}
        }
        if (preferredLocations != null && !preferredLocations.isEmpty()) {
            profile.setPreferredLocations(String.join(", ", preferredLocations));
        }
        if (workMode != null && !workMode.isEmpty()) profile.setWorkMode(String.join(", ", workMode));
        if (noticePeriodDays != null && !noticePeriodDays.isBlank()) {
            try { profile.setNoticePeriodDays(Integer.parseInt(noticePeriodDays.trim())); } catch (NumberFormatException ignored) {}
        }
        profile.setServingNotice(servingNotice != null && servingNotice);
        if (noticeStartDate != null && !noticeStartDate.isBlank()) {
            try { profile.setNoticeStartDate(java.time.LocalDate.parse(noticeStartDate.trim())); }
            catch (java.time.format.DateTimeParseException ignored) {}
        } else {
            profile.setNoticeStartDate(null);
        }

        profileRepository.save(profile);
        ra.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/jobseeker/dashboard";
    }

    @GetMapping("/resume")
    public String showResumeUpload(Principal principal, Model model) {
        if (principal == null) return "redirect:/jobseeker/login";
        model.addAttribute("resumeUploadDto", new ResumeUploadDto());
        return "jobseeker/resume-upload";
    }

    @PostMapping("/resume")
    public String handleResumeUpload(Principal principal,
                                     @ModelAttribute("resumeUploadDto") @Valid ResumeUploadDto dto,
                                     BindingResult result,
                                     Model model) throws IOException {
        if (principal == null) return "redirect:/jobseeker/login";
        if (result.hasErrors()) return "jobseeker/resume-upload";

        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName())
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
                    r.setJobseeker(jobseeker); // @MapsId derives ID automatically — do NOT set manually
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

    @GetMapping("/resume/download")
    @ResponseBody
    public ResponseEntity<Resource> downloadOwnResume(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();
        Resume resume = resumeRepository.findById(jobseeker.getId()).orElse(null);
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

    @GetMapping("/account")
    public String viewAccountSettings(Principal principal,
                                      @RequestParam(required = false) String success,
                                      @RequestParam(required = false) String error,
                                      Model model) {
        if (principal == null) return "redirect:/jobseeker/login";
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("jobseeker", jobseeker);
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
        // privacySuccess comes via flash attribute — already in model from RedirectAttributes
        return "jobseeker/account";
    }

    @PostMapping("/account/change-password")
    public String changePassword(Principal principal,
                                  @RequestParam String currentPassword,
                                  @RequestParam String newPassword,
                                  @RequestParam String confirmPassword) {
        if (principal == null) return "redirect:/jobseeker/login";
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();

        if ("GOOGLE".equals(jobseeker.getAuthProvider())) {
            return "redirect:/jobseeker/account?error=Google+accounts+cannot+change+password+here";
        }
        if (!passwordEncoder.matches(currentPassword, jobseeker.getPassword())) {
            return "redirect:/jobseeker/account?error=Current+password+is+incorrect";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/jobseeker/account?error=New+passwords+do+not+match";
        }
        if (newPassword.length() < 8) {
            return "redirect:/jobseeker/account?error=Password+must+be+at+least+8+characters";
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
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();
        jobseeker.setFullName(fullName.trim());
        jobseekerRepository.save(jobseeker);
        return "redirect:/jobseeker/account?success=Name+updated+successfully";
    }

    /** Toggle "Private from current employer" — hides candidate from their employer's recruiter. */
    @PostMapping("/profile/employer-privacy")
    public String toggleEmployerPrivacy(Principal principal,
                                        @RequestParam boolean hide,
                                        RedirectAttributes ra) {
        if (principal == null) return "redirect:/jobseeker/login";
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();
        jobseeker.setHideFromCurrentEmployer(hide);
        jobseekerRepository.save(jobseeker);
        String msg = hide
                ? "Your profile is now hidden from recruiters at your current company."
                : "Your profile is now visible to recruiters at your current company.";
        ra.addFlashAttribute("privacySuccess", msg);
        return "redirect:/jobseeker/account";
    }

    /** Pause the candidate's profile — hides them from recruiter browse and declines all pending invites. */
    @PostMapping("/profile/pause")
    public String pauseProfile(Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/jobseeker/login";
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();
        jobseeker.setProfilePaused(true);
        jobseeker.setPausedAt(java.time.LocalDateTime.now());
        jobseekerRepository.save(jobseeker);
        int declined = inviteRepository.declineAllPendingForJobseeker(jobseeker);
        String msg = "Your profile is now paused. Recruiters cannot see you until you resume.";
        if (declined > 0) msg += " " + declined + " pending invite(s) were automatically declined.";
        ra.addFlashAttribute("pauseSuccess", msg);
        return "redirect:/jobseeker/dashboard";
    }

    /** Resume a paused profile — makes the candidate visible in recruiter browse again. */
    @PostMapping("/profile/resume")
    public String resumeProfile(Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/jobseeker/login";
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();
        jobseeker.setProfilePaused(false);
        jobseekerRepository.save(jobseeker);
        ra.addFlashAttribute("pauseSuccess", "Your profile is active again. Recruiters can now find you.");
        return "redirect:/jobseeker/dashboard";
    }

    // ── Preference-ranking helpers ──────────────────────────────────────────

    /** Returns true if the invite's job matches all the candidate's declared preferences. */
    private boolean isPreferenceMatch(JobInvite invite, JobseekerProfile profile) {
        if (profile == null) return true; // no preferences set — treat as match
        var job = invite.getJob();
        // Work mode match — candidate may have selected multiple work modes (comma-separated)
        if (profile.getWorkMode() != null && job.getWorkMode() != null) {
            boolean matchesAny = Arrays.stream(profile.getWorkMode().split(","))
                    .map(String::trim)
                    .anyMatch(mode -> mode.equalsIgnoreCase(job.getWorkMode()));
            if (!matchesAny) return false;
        }
        // Min CTC match: job's max salary should be at least candidate's expected CTC (in LPA)
        if (profile.getExpectedCtc() != null && job.getSalaryMax() != null
                && job.getSalaryMax() < profile.getExpectedCtc()) {
            return false;
        }
        return true;
    }

    /** Sorts invites so preference-matching ones appear first. */
    private List<JobInvite> rankInvitesByPreference(List<JobInvite> invites, JobseekerProfile profile) {
        if (profile == null) return invites;
        List<JobInvite> matching    = new ArrayList<>();
        List<JobInvite> nonMatching = new ArrayList<>();
        for (JobInvite inv : invites) {
            if (isPreferenceMatch(inv, profile)) matching.add(inv);
            else nonMatching.add(inv);
        }
        List<JobInvite> ranked = new ArrayList<>(matching);
        ranked.addAll(nonMatching);
        return ranked;
    }

    @GetMapping("/refer-and-earn")
    public String viewReferAndEarn(Principal principal, Model model) {
        if (principal == null) return "redirect:/jobseeker/login";
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();
        // Accounts created before referral codes existed (or via Google) may not have one yet
        jobseekerService.ensureReferralCode(jobseeker);

        var referrals = referralService.getReferralsByReferrer(jobseeker);
        long signedUp       = referrals.stream().filter(r -> !r.getStatus().equals("")).count();
        long approved       = referrals.stream().filter(r -> "PROFILE_APPROVED".equals(r.getStatus()) || "HIRED".equals(r.getStatus()) || "REWARD_PAID".equals(r.getStatus())).count();
        long hired          = referrals.stream().filter(r -> "HIRED".equals(r.getStatus()) || "REWARD_PAID".equals(r.getStatus())).count();
        long rewardsPaid    = referrals.stream().filter(r -> "REWARD_PAID".equals(r.getStatus())).count();
        long pendingRewards = hired - rewardsPaid;

        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("referrals", referrals);
        model.addAttribute("totalSignedUp", signedUp);
        model.addAttribute("totalApproved", approved);
        model.addAttribute("totalHired", hired);
        model.addAttribute("rewardsPaid", rewardsPaid);
        model.addAttribute("pendingRewards", pendingRewards);
        return "jobseeker/refer-and-earn";
    }
}
