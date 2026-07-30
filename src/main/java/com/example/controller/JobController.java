package com.example.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.model.Job;
import com.example.model.Jobseeker;
import com.example.service.ApplicationService;
import com.example.service.EventLogService;
import com.example.service.JobService;
import com.example.repository.ApplicationRepository;
import com.example.repository.JobseekerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    @Autowired private ObjectMapper objectMapper;

    @GetMapping
    public String listJobs(Model model,
                           @PageableDefault(size = 10, sort = "postedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Job> jobsPage = jobService.search(null, null, null, null, null, null, null, pageable);
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
        model.addAttribute("jobPostingJsonLd", buildJobPostingJsonLd(job));
        return "jobs/detail";
    }

    /** Builds schema.org JobPosting JSON-LD so listings qualify for Google for Jobs. */
    private String buildJobPostingJsonLd(Job job) {
        try {
            Map<String, Object> jsonLd = new LinkedHashMap<>();
            jsonLd.put("@context", "https://schema.org/");
            jsonLd.put("@type", "JobPosting");
            jsonLd.put("title", job.getTitle());
            jsonLd.put("description", job.getDescription());

            LocalDateTime posted = job.getPostedDate() != null ? job.getPostedDate() : LocalDateTime.now();
            jsonLd.put("datePosted", posted.toLocalDate().toString());
            // Job has no expiry field yet, so default listings to a 90-day validity window — Google requires validThrough.
            jsonLd.put("validThrough", posted.toLocalDate().plusDays(90).toString());

            jsonLd.put("employmentType", mapEmploymentType(job.getJobType()));

            Map<String, Object> hiringOrg = new LinkedHashMap<>();
            hiringOrg.put("@type", "Organization");
            String companyName = job.getRecruiter() != null && job.getRecruiter().getCompany() != null
                    ? job.getRecruiter().getCompany().getName() : "KoderHyre";
            hiringOrg.put("name", companyName);
            if (job.getRecruiter() != null && job.getRecruiter().getCompany() != null
                    && job.getRecruiter().getCompany().getWebsite() != null
                    && !job.getRecruiter().getCompany().getWebsite().isBlank()) {
                hiringOrg.put("sameAs", job.getRecruiter().getCompany().getWebsite());
            }
            jsonLd.put("hiringOrganization", hiringOrg);

            Map<String, Object> address = new LinkedHashMap<>();
            address.put("@type", "PostalAddress");
            address.put("addressLocality", job.getLocation());
            address.put("addressCountry", "IN");
            Map<String, Object> jobLocation = new LinkedHashMap<>();
            jobLocation.put("@type", "Place");
            jobLocation.put("address", address);
            jsonLd.put("jobLocation", jobLocation);

            if ("REMOTE".equals(job.getWorkMode())) {
                jsonLd.put("jobLocationType", "TELECOMMUTE");
                Map<String, Object> applicantLocation = new LinkedHashMap<>();
                applicantLocation.put("@type", "Country");
                applicantLocation.put("name", "IN");
                jsonLd.put("applicantLocationRequirements", applicantLocation);
            }

            if (job.getSalaryMin() != null && job.getSalaryMax() != null) {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("@type", "QuantitativeValue");
                // salaryMin/Max are stored in LPA (lakhs per annum) — convert to actual INR for schema.org.
                value.put("minValue", job.getSalaryMin() * 100000);
                value.put("maxValue", job.getSalaryMax() * 100000);
                value.put("unitText", "YEAR");
                Map<String, Object> baseSalary = new LinkedHashMap<>();
                baseSalary.put("@type", "MonetaryAmount");
                baseSalary.put("currency", "INR");
                baseSalary.put("value", value);
                jsonLd.put("baseSalary", baseSalary);
            }

            String json = objectMapper.writeValueAsString(jsonLd);
            return json.replace("</", "<\\/"); // prevent the JSON from prematurely closing the <script> tag
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String mapEmploymentType(String jobType) {
        if (jobType == null) return "OTHER";
        return switch (jobType) {
            case "FULL_TIME" -> "FULL_TIME";
            case "PART_TIME" -> "PART_TIME";
            case "CONTRACT" -> "CONTRACTOR";
            default -> "OTHER";
        };
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String workMode,
                         @RequestParam(required = false) String jobType,
                         @RequestParam(required = false) Integer expMin,
                         @RequestParam(required = false) Integer expMax,
                         @RequestParam(required = false) Integer salaryMin,
                         @RequestParam(required = false) Integer salaryMax,
                         @PageableDefault(size = 10, sort = "postedDate", direction = Sort.Direction.DESC) Pageable pageable,
                         Model model) {
        Page<Job> jobsPage = jobService.search(q, workMode, jobType, expMin, expMax, salaryMin, salaryMax, pageable);
        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("q", q);
        model.addAttribute("workMode", workMode);
        model.addAttribute("jobType", jobType);
        model.addAttribute("expMin", expMin);
        model.addAttribute("expMax", expMax);
        model.addAttribute("salaryMin", salaryMin);
        model.addAttribute("salaryMax", salaryMax);
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