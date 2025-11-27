package com.example.controller.recruiter;

import com.example.model.recruiter.Recruiter;
import jakarta.servlet.http.HttpSession;
import com.example.repositary.recruiter.RecruiterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/recruiter")
public class RecruiterController {

    @Autowired
    private RecruiterRepository recruiterRepository;

    // This GET mapping likely exists already
    @GetMapping("/register-form")
    public String showRegisterForm(Model model) {
        model.addAttribute("recruiter", new Recruiter());
        return "recruiter/register";
    }

    // Add this POST mapping to handle form submission
    @PostMapping("/register")
    public String registerRecruiter(Recruiter recruiter, HttpSession session) {
        // Set initial activation status
        recruiter.setActivationStatus("Pending");

        // Save the recruiter to the database
        recruiterRepository.save(recruiter);

        // Store recruiter ID in session for later use
        session.setAttribute("recruiterId", recruiter.getId());

        return "redirect:/recruiter/signup-success";
    }

    @GetMapping("/signup-success")
    public String signupSuccess() {
        return "recruiter/signup-success";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Long recruiterId = (Long) session.getAttribute("recruiterId");

        if (recruiterId == null) {
            return "redirect:/recruiter/login";
        }

        Recruiter recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));

        model.addAttribute("recruiter", recruiter);
        return "recruiter/dashboard";
    }
}