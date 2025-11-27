package com.example.controller;

import com.example.model.Jobseeker;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import java.util.Map;

@Controller
public class AuthController {

    @GetMapping("/")
    public String home() {
        return "homepage";
    }

    @GetMapping("/register-recruiter")
    public String showRecruiterLoginForm(@RequestParam(value = "error", required = false) String error,
                                         Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Email or password is not correct.");
        }
        return "recruiter/login";
    }

    @PostMapping("/recruiter/login")
    public String recruiterLogin(@RequestParam String email, @RequestParam String password) {
        // Add authentication logic here
        return "redirect:/recruiter/dashboard";
    }

//    @GetMapping("/recruiter/dashboard")
//    public String recruiterDashboard() {
//        return "recruiter/dashboard";
//    }

    @GetMapping("/jobseeker/login")
    public String showJobseekerLoginForm(@RequestParam(value = "error", required = false) String error,
                                         Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Email or password is not correct.");
        }
        return "jobseeker/login";
    }


    @PostMapping("/profile-details")
    public String saveProfileDetails(@RequestParam Map<String, String> formData) {
        // Save the profile details to the user's account
        return "redirect:/jobseeker/dashboard";
    }

//    @GetMapping("/jobseeker/dashboard")
//    public String seekerDashboard() {
//        return "jobseeker/dashboard";
//    }

    @GetMapping("/recruiter/register")
    public String recruiterRegisterForm() {
        return "recruiter/register";
    }


    // Custom error page
    @GetMapping("/error")
    public String handleError() {
        return "error";
    }
}