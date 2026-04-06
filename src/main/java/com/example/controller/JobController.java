package com.example.controller;

import java.security.Principal;

import com.example.model.Job;
import com.example.model.Jobseeker;
import com.example.service.ApplicationService;
import com.example.service.EventLogService;
import com.example.service.JobService;
import com.example.repositary.JobseekerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private EventLogService eventLogService;

    @GetMapping
    public String listJobs(Model model,
                           @PageableDefault(size = 10, sort = "postedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Job> jobsPage = jobService.listActiveJobs(pageable);
        model.addAttribute("jobsPage", jobsPage);
        return "jobs/list";
    }

    @GetMapping("/{id}")
    public String viewJob(@PathVariable Long id,
                          Principal principal,
                          Model model) {

        Job job = jobService.findById(id).orElseThrow(() -> new RuntimeException("Job not found"));
        Long jobseekerId = null;

        if (principal != null) {
            String email = principal.getName();
            Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElse(null);
            if (jobseeker != null) {
                jobseekerId = jobseeker.getId();
            }
        }

        eventLogService.log("job_view", job.getId(), jobseekerId, null, null);
        model.addAttribute("job", job);
        return "jobs/detail";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @PageableDefault(size = 10, sort = "postedDate", direction = Sort.Direction.DESC) Pageable pageable,
                         Model model) {
        Page<Job> jobsPage = jobService.search(q, pageable);
        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("q", q);
        return "jobs/list";
    }

    @PostMapping("/{id}/apply")
    public String apply(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return "redirect:/jobseeker/login";
        }
        String email = principal.getName();
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElseThrow();
        Job job = jobService.findById(id).orElseThrow();
        applicationService.apply(job, jobseeker, "website");
        return "redirect:/jobseeker/applications";
    }
}