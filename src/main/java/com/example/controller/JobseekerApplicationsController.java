package com.example.controller;

import com.example.model.Application;
import com.example.model.Jobseeker;
import com.example.repositary.ApplicationRepository;
import com.example.repositary.JobseekerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
public class JobseekerApplicationsController {

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    /**
     * Dedicated page for jobseeker to view all applications
     *
     * URL: GET /jobseeker/applications
     */
    @GetMapping("/jobseeker/applications")
    public String viewMyApplications(Principal principal, Model model) {

        // Not logged in → redirect to login
        if (principal == null) {
            return "redirect:/jobseeker/login";
        }

        String email = principal.getName();

        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));

        List<Application> applications =
                applicationRepository.findByJobseekerOrderByAppliedDateDesc(jobseeker);

        model.addAttribute("applications", applications);
        model.addAttribute("jobseeker", jobseeker);

        return "jobseeker/applications";
    }
}