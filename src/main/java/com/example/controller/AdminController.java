
package com.example.controller;

import com.example.model.Jobseeker;
import com.example.model.recruiter.Recruiter;
import com.example.repositary.EventLogRepository;
import com.example.repositary.JobseekerRepository;
import com.example.repositary.recruiter.RecruiterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private EventLogRepository eventLogRepository;

    @GetMapping("/login")
    public String adminLogin() {
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long jobseekerCount = jobseekerRepository.count();
        long recruiterCount = recruiterRepository.count();
        long views = eventLogRepository.count(); // simple metric; could filter by type

        model.addAttribute("jobseekerCount", jobseekerCount);
        model.addAttribute("recruiterCount", recruiterCount);
        model.addAttribute("eventCount", views);
        return "admin/dashboard";
    }

    @GetMapping("/jobseekers")
    public String listJobseekers(Model model) {
        List<Jobseeker> jobseekers = jobseekerRepository.findAll();
        model.addAttribute("jobseekers", jobseekers);
        return "admin/jobseekers";
    }

    @GetMapping("/recruiters")
    public String listRecruiters(Model model) {
        List<Recruiter> recruiters = recruiterRepository.findAll();
        model.addAttribute("recruiters", recruiters);
        return "admin/recruiters";
    }

    @PostMapping("/recruiters/{id}/approve")
    public String approveRecruiter(@PathVariable Long id) {
        Recruiter recruiter = recruiterRepository.findById(id).orElseThrow();
        recruiter.setActivationStatus("Active");
        recruiterRepository.save(recruiter);
        return "redirect:/admin/recruiters";
    }

    @PostMapping("/recruiters/{id}/reject")
    public String rejectRecruiter(@PathVariable Long id) {
        Recruiter recruiter = recruiterRepository.findById(id).orElseThrow();
        recruiter.setActivationStatus("Rejected");
        recruiterRepository.save(recruiter);
        return "redirect:/admin/recruiters";
    }
}
