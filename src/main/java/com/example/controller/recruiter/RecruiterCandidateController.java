package com.example.controller.recruiter;

import com.example.model.*;
import com.example.model.recruiter.Recruiter;
import com.example.repository.JobInviteRepository;
import com.example.repository.JobseekerProfileRepository;
import com.example.repository.JobseekerRepository;
import com.example.repository.ResumeRepository;
import com.example.service.EmailService;
import com.example.service.JobService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/recruiter")
public class RecruiterCandidateController extends RecruiterBaseController {

    @Autowired private JobService jobService;
    @Autowired private JobseekerRepository jobseekerRepository;
    @Autowired private JobseekerProfileRepository profileRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private JobInviteRepository inviteRepository;
    @Autowired private EmailService emailService;

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
        String recruiterCompany = (recruiter.getCompany() != null) ? recruiter.getCompany().getName() : null;
        String[] terms = splitSkillTerms(skills);
        Page<JobseekerProfile> candidatesPage = profileRepository.browseWithFilters(
                terms[0], terms[1], terms[2], terms[3], expMin, expMax, recruiterCompany, pageable);
        List<Job> myJobs = jobService.listJobsByRecruiter(recruiter, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream().filter(Job::isActive).toList();
        model.addAttribute("candidatesPage", candidatesPage);
        model.addAttribute("filterSkills", skills);
        model.addAttribute("filterExpMin", expMin);
        model.addAttribute("filterExpMax", expMax);
        model.addAttribute("myJobs", myJobs);
        model.addAttribute("recruiter", recruiter);
        return "recruiter/browse-candidates";
    }

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

        String recruiterCompany = (recruiter.getCompany() != null) ? recruiter.getCompany().getName() : null;
        String[] terms = splitSkillTerms(searchSkills);
        Page<JobseekerProfile> candidatesPage = profileRepository.findMatchingCandidates(
                terms[0], terms[1], terms[2], terms[3], searchExpMin, searchExpMax, recruiterCompany, pageable);

        model.addAttribute("job", job);
        model.addAttribute("candidatesPage", candidatesPage);
        model.addAttribute("filterSkills", searchSkills);
        model.addAttribute("filterExpMin", searchExpMin);
        model.addAttribute("filterExpMax", searchExpMax);
        model.addAttribute("recruiter", recruiter);
        return "recruiter/candidates";
    }

    @GetMapping("/candidate/{jobseekerId}")
    public String viewCandidateDetail(@PathVariable Long jobseekerId,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      Model model, RedirectAttributes ra) {
        Recruiter recruiter;
        try { recruiter = getActiveRecruiter(userDetails); }
        catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recruiter/dashboard";
        }
        Jobseeker js = jobseekerRepository.findById(jobseekerId).orElseThrow();
        JobseekerProfile profile = profileRepository.findByJobseeker(js).orElse(null);
        Resume resume = resumeRepository.findById(jobseekerId).orElse(null);
        List<Job> myJobs = jobService.listJobsByRecruiter(recruiter, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream().filter(Job::isActive).toList();

        boolean contactUnlocked = inviteRepository.existsByJobseekerIdAndJob_RecruiterAndStatus(
                jobseekerId, recruiter, InviteStatus.ACCEPTED);

        model.addAttribute("candidate", js);
        model.addAttribute("profile", profile);
        model.addAttribute("hasResume", resume != null && resume.getFileName() != null);
        model.addAttribute("contactUnlocked", contactUnlocked);
        model.addAttribute("myJobs", myJobs);
        model.addAttribute("recruiter", recruiter);
        return "recruiter/candidate";
    }

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

        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() != 80 && request.getServerPort() != 443
                       ? ":" + request.getServerPort() : "");
            String companyName = recruiter.getCompany() != null ? recruiter.getCompany().getName() : "A company";
            emailService.sendInviteNotification(
                    js.getEmail(), js.getFullName(),
                    job.getTitle(), companyName,
                    baseUrl + "/jobseeker/interview-requests");
        } catch (Exception emailEx) {
            // Email failure must not roll back a successful invite
        }

        ra.addFlashAttribute("success", "Interview invitation sent to " + js.getFullName() + "!");
        return "redirect:/recruiter/jobs/" + jobId + "/candidates";
    }

    /** Splits a comma-separated skills filter into up to 4 individual search terms (padded with ""). */
    private static String[] splitSkillTerms(String skillsCsv) {
        String[] slots = {"", "", "", ""};
        if (skillsCsv == null || skillsCsv.isBlank()) return slots;
        int i = 0;
        for (String term : skillsCsv.split(",")) {
            String t = term.trim();
            if (!t.isEmpty() && i < slots.length) slots[i++] = t;
        }
        return slots;
    }

    @GetMapping("/invites")
    public String viewSentInvites(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Recruiter recruiter = getRecruiter(userDetails);
        List<JobInvite> invites = inviteRepository.findByJob_RecruiterOrderByCreatedAtDesc(recruiter);
        model.addAttribute("invites", invites);
        model.addAttribute("recruiter", recruiter);
        return "recruiter/invites";
    }
}
