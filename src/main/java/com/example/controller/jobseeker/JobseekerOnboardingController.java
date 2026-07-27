package com.example.controller.jobseeker;

import com.example.dto.JobseekerProfileDto;
import com.example.dto.ResumeUploadDto;
import com.example.model.Jobseeker;
import com.example.model.JobseekerProfile;
import com.example.model.Resume;
import com.example.repository.JobseekerProfileRepository;
import com.example.repository.JobseekerRepository;
import com.example.repository.ResumeRepository;
import com.example.service.FileStorageService;
import com.example.service.JobseekerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.Principal;

@Controller
@RequestMapping("/jobseeker")
public class JobseekerOnboardingController {

    @Autowired private JobseekerService jobseekerService;
    @Autowired private JobseekerRepository jobseekerRepository;
    @Autowired private JobseekerProfileRepository profileRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private FileStorageService fileStorageService;

    @GetMapping("/onboarding")
    public String showOnboarding(HttpSession session, Principal principal, Model model) {
        String email = resolveOnboardingEmail(session, principal);
        if (email == null) return "redirect:/jobseeker/register";

        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));
        JobseekerProfile profile = profileRepository.findByJobseeker(jobseeker)
                .orElseGet(() -> {
                    JobseekerProfile p = new JobseekerProfile();
                    p.setJobseeker(jobseeker);
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
                                         Model model,
                                         RedirectAttributes redirectAttrs) throws IOException {
        String email = resolveOnboardingEmail(session, principal);
        if (email == null) return "redirect:/jobseeker/register";

        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));

        if (result.hasErrors()) {
            model.addAttribute("email", email);
            model.addAttribute("jobseeker", jobseeker);
            return "jobseeker/resume-onboarding";
        }

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

        session.removeAttribute("pending_onboarding_email");
        redirectAttrs.addFlashAttribute("showWelcomeBanner", true);

        // If ATS Checker was the trigger, redirect there after completing onboarding
        String next = (String) session.getAttribute("onboarding_next");
        if ("ats-checker".equals(next)) {
            session.removeAttribute("onboarding_next");
            return "redirect:/jobseeker/ats-checker";
        }

        return "redirect:/jobseeker/dashboard";
    }

    /**
     * Resolves the email for the onboarding flow securely.
     * Priority: authenticated principal > session token.
     * Never trusts a URL parameter.
     */
    static String resolveOnboardingEmail(HttpSession session, Principal principal) {
        if (principal != null) return principal.getName();
        return (String) session.getAttribute("pending_onboarding_email");
    }
}
