package com.example.controller;

import com.example.repositary.JobseekerProfileRepository;
import com.example.repositary.JobseekerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private JobseekerProfileRepository profileRepository;

    @GetMapping("/jobseekers")
    public String listJobseekers(Model model) {
        model.addAttribute("jobseekers", jobseekerRepository.findAll());
        return "admin/jobseekers";
    }
}