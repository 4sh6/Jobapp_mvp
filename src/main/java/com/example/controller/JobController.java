package com.example.controller;

import java.security.Principal;

import com.example.model.Job;
import com.example.model.Jobseeker;
import com.example.service.ApplicationService;
import com.example.service.EventLogService;
import com.example.service.JobService;
import com.example.repositary.ApplicationRepository;
import com.example.repositary.JobseekerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/jobs")
public class JobController {

    @Autowired private JobService jobService;
    @Autowired private ApplicationService applicationService;
    @Autowired private JobseekerRepository jobseekerRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private EventLogService eventLogService;

    @GetMapping
    public String listJobs(Model model,
                           @PageableDefault(size = 10, sort = "postedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Job> jobsPage = jobService.search(null, null, null, null, pageable);
        model.addAttribute("jobsPage", jobsPage);
        return "jobs/list";
    }

    @GetMapping("/{id}")
    public String viewJob(@PathVariable Long id, Principal principal, Model model) {
        Job job = jobService.findById(id).orElseThrow(() -> new RuntimeException("Job not found"));
        Long jobseekerId = null;
        boolean alreadyApplied = false;

        if (principal != null) {
            String email = principal.getName();
            Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElse(null);
            if (jobseeker != null) {
                jobseekerId = jobseeker.getId();
                alreadyApplied = applicationService.hasApplied(job, jobseeker);
            }
        }

        eventLogService.log("job_view", job.getId(), jobseekerId, null, null);
        model.addAttribute("job", job);
        model.addAttribute("alreadyApplied", alreadyApplied);
        return "jobs/detail";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String workMode,
                         @RequestParam(required = false) Integer expMin,
                         @RequestParam(required = false) Integer expMax,
                         @PageableDefault(size = 10, sort = "postedDate", direction = Sort.Direction.DESC) Pageable pageable,
                         Model model) {
        Page<Job> jobsPage = jobService.search(q, workMode, expMin, expMax, pageable);
        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("q", q);
        model.addAttribute("workMode", workMode);
        model.addAttribute("expMin", expMin);
        model.addAttribute("expMax", expMax);
        return "jobs/list";
    }

    @PostMapping("/{id}/apply")
    public String apply(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/jobseeker/login";
        }
        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElseThrow();
        Job job = jobService.findById(id).orElseThrow();

        try {
            applicationService.apply(job, jobseeker, "website");
            ra.addFlashAttribute("success", "Application submitted successfully!");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/jobseeker/applications";
    }
}