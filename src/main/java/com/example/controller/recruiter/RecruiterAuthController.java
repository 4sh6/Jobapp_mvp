package com.example.controller.recruiter;

import com.example.dto.RecruiterRegistrationDto;
import com.example.service.recruiter.RecruiterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/recruiter")
public class RecruiterAuthController extends RecruiterBaseController {

    @Autowired private RecruiterService recruiterService;

    @GetMapping("/home")
    public String recruiterHome() {
        return "recruiter/home";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("recruiterRegistrationDto", new RecruiterRegistrationDto());
        return "recruiter/register";
    }

    @PostMapping("/register")
    public String registerRecruiter(@ModelAttribute("recruiterRegistrationDto") @Valid RecruiterRegistrationDto dto,
                                    BindingResult result, Model model) {
        if (result.hasErrors()) return "recruiter/register";
        try {
            recruiterService.registerRecruiter(dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "recruiter/register";
        }
        return "recruiter/signup-success";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "recruiter/login";
    }
}
