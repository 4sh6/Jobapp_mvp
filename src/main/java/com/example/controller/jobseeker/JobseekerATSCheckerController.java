package com.example.controller.jobseeker;

import com.example.model.Jobseeker;
import com.example.model.JobseekerProfile;
import com.example.repository.JobseekerProfileRepository;
import com.example.repository.JobseekerRepository;
import com.example.service.ATSCheckerService;
import com.example.service.OnboardingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/jobseeker/ats-checker")
public class JobseekerATSCheckerController {

    @Autowired private JobseekerRepository jobseekerRepository;
    @Autowired private JobseekerProfileRepository profileRepository;
    @Autowired private ATSCheckerService atsCheckerService;
    @Autowired private OnboardingService onboardingService;

    @GetMapping
    public String showATSChecker(Principal principal, HttpSession session, Model model) {
        // Not logged in — redirect to login (clean redirect, no session error)
        if (principal == null) {
            return "redirect:/jobseeker/login";
        }

        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElse(null);
        if (jobseeker == null) {
            return "redirect:/jobseeker/login";
        }

        // Profile not completed — send through onboarding, then return here
        if (!jobseeker.isProfileCompleted()) {
            session.setAttribute("onboarding_next", "ats-checker");
            return "redirect:/jobseeker/onboarding";
        }

        // Resume not uploaded — send through resume upload, then return here
        if (!jobseeker.isResumeUploaded()) {
            session.setAttribute("onboarding_next", "ats-checker");
            return "redirect:/jobseeker/resume-onboarding";
        }

        JobseekerProfile profile = profileRepository.findByJobseeker(jobseeker).orElse(null);
        int completionPercent = onboardingService.getCompletionPercentage(jobseeker, profile);

        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("profile", profile);
        model.addAttribute("completionPercent", completionPercent);
        return "jobseeker/ats-checker";
    }

    @PostMapping("/check")
    public String checkMatch(
            Principal principal,
            HttpSession session,
            @RequestParam String jobDescription,
            @RequestParam(required = false) String jobTitle,
            Model model) {

        if (principal == null) {
            return "redirect:/jobseeker/login";
        }

        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();
        JobseekerProfile profile = profileRepository.findByJobseeker(jobseeker).orElse(null);

        if (profile == null || jobDescription == null || jobDescription.isBlank()) {
            model.addAttribute("error", "Please enter a valid job description.");
            return showATSChecker(principal, session, model);
        }

        if (jobDescription.length() < 50) {
            model.addAttribute("error", "Job description is too short. Please paste a complete job posting.");
            return showATSChecker(principal, session, model);
        }

        ATSCheckerService.ATSMatchResult result = atsCheckerService.checkMatch(profile, jobDescription, jobTitle);

        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("profile", profile);
        model.addAttribute("completionPercent", onboardingService.getCompletionPercentage(jobseeker, profile));
        model.addAttribute("matchScore", result.getScore());
        model.addAttribute("feedback", result.getFeedback());
        model.addAttribute("breakdown", result.getBreakdown());
        model.addAttribute("jobDescription", jobDescription);
        model.addAttribute("jobTitle", jobTitle != null ? jobTitle : "Untitled Job");
        model.addAttribute("showResult", true);

        return "jobseeker/ats-checker";
    }
}
