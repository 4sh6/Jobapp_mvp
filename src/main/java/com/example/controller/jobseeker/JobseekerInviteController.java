package com.example.controller.jobseeker;

import com.example.model.*;
import com.example.repository.JobInviteRepository;
import com.example.repository.JobseekerProfileRepository;
import com.example.repository.JobseekerRepository;
import com.example.service.ApplicationService;
import com.example.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/jobseeker")
public class JobseekerInviteController {

    @Autowired private JobseekerRepository jobseekerRepository;
    @Autowired private JobseekerProfileRepository profileRepository;
    @Autowired private JobInviteRepository inviteRepository;
    @Autowired private ApplicationService applicationService;
    @Autowired private EmailService emailService;

    @GetMapping("/interview-requests")
    public String viewInterviewRequests(Principal principal, Model model) {
        if (principal == null) return "redirect:/jobseeker/login";
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();

        List<JobInvite> pendingInvites = inviteRepository.findByJobseeker(jobseeker).stream()
                .filter(inv -> inv.getStatus() == InviteStatus.PENDING)
                .filter(inv -> inv.getJob().isActive()) // hide invites whose job was closed
                .collect(Collectors.toList());

        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("invites", pendingInvites);
        return "jobseeker/interview-requests";
    }

    @GetMapping("/active-interviews")
    public String viewActiveInterviews(Principal principal, Model model) {
        if (principal == null) return "redirect:/jobseeker/login";
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName()).orElseThrow();

        List<JobInvite> activeInvites = inviteRepository.findByJobseeker(jobseeker).stream()
                .filter(inv -> inv.getStatus() == InviteStatus.ACCEPTED)
                .collect(Collectors.toList());

        model.addAttribute("jobseeker", jobseeker);
        model.addAttribute("invites", activeInvites);
        return "jobseeker/active-interviews";
    }

    @PostMapping("/invite/{id}/accept")
    public String acceptInvite(@PathVariable Long id,
                               Principal principal,
                               HttpServletRequest request,
                               RedirectAttributes ra) {
        JobInvite invite = inviteRepository.findById(id).orElseThrow();

        if (principal == null || !invite.getJobseeker().getEmail().equals(principal.getName())) {
            return "redirect:/jobseeker/login";
        }

        if (!invite.getJob().isActive() || !"PUBLISHED".equals(invite.getJob().getStatus())) {
            ra.addFlashAttribute("error",
                    "This position has been closed by the company, so the invitation is no longer available.");
            return "redirect:/jobseeker/interview-requests";
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        inviteRepository.save(invite);

        // Create application — ignore if one already exists (duplicate invite/apply path)
        try {
            applicationService.apply(invite.getJob(), invite.getJobseeker(), "invite");
        } catch (IllegalStateException ignored) {
            // Already applied or job closed — invite acceptance still stands
        }

        Jobseeker js       = invite.getJobseeker();
        var job            = invite.getJob();
        var recruiter      = job.getRecruiter();
        var profile        = profileRepository.findByJobseeker(js).orElse(null);
        String companyName = recruiter.getCompany() != null ? recruiter.getCompany().getName() : "the company";

        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() != 80 && request.getServerPort() != 443
                       ? ":" + request.getServerPort() : "");

            emailService.sendAcceptanceConfirmationToCandidate(
                    js.getEmail(), js.getFullName(),
                    job.getTitle(), companyName,
                    job.getRequiredSkills() != null ? job.getRequiredSkills() : "your core skills",
                    baseUrl + "/jobseeker/interview-requests");

            String experienceSummary = profile != null && profile.getExperienceYears() != null
                    ? profile.getExperienceYears() + " yrs"
                      + (profile.getExperienceMonths() != null && profile.getExperienceMonths() > 0
                         ? " " + profile.getExperienceMonths() + " months" : "")
                    : "Not specified";
            String expectedCtc = profile != null && profile.getExpectedCtc() != null
                    ? "₹" + profile.getExpectedCtc() + " LPA" : "Not specified";
            String noticePeriod = profile != null && profile.getNoticePeriodDays() != null
                    ? profile.getNoticePeriodDays() + " days" : "Not specified";

            emailService.sendAcceptanceNotificationToRecruiter(
                    recruiter.getEmail(), recruiter.getName(),
                    js.getFullName(), js.getEmail(),
                    job.getTitle(),
                    experienceSummary, expectedCtc, noticePeriod,
                    baseUrl + "/recruiter/candidate/" + js.getId());
        } catch (Exception emailEx) {
            // Email failure must not break the acceptance flow
        }

        ra.addFlashAttribute("success",
                "You've accepted the invitation! " + companyName + " will be in touch with you shortly.");
        return "redirect:/jobseeker/interview-requests";
    }

    @PostMapping("/invite/{id}/decline")
    public String declineInvite(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        JobInvite invite = inviteRepository.findById(id).orElseThrow();
        if (principal == null || !invite.getJobseeker().getEmail().equals(principal.getName())) {
            return "redirect:/jobseeker/login";
        }
        invite.setStatus(InviteStatus.DECLINED);
        inviteRepository.save(invite);
        ra.addFlashAttribute("info", "Invitation declined.");
        return "redirect:/jobseeker/interview-requests";
    }
}
