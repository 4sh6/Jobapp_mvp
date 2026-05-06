package com.example.controller.recruiter;

import com.example.model.recruiter.Recruiter;
import com.example.repository.CompanyRepository;
import com.example.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recruiter")
public class RecruiterCompanyController extends RecruiterBaseController {

    @Autowired private CompanyRepository companyRepository;

    @GetMapping("/company")
    public String viewCompanyProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Recruiter recruiter = getRecruiter(userDetails);
        model.addAttribute("recruiter", recruiter);
        model.addAttribute("company", recruiter.getCompany());
        return "recruiter/company-profile";
    }

    @PostMapping("/company")
    public String updateCompanyProfile(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestParam String companyName,
                                       @RequestParam(required = false) String website,
                                       @RequestParam(required = false) String industry,
                                       @RequestParam(required = false) String sizeBand,
                                       @RequestParam(required = false) String description,
                                       @RequestParam(required = false) String jobTitle,
                                       @RequestParam(required = false) String phoneNumber,
                                       RedirectAttributes ra) {
        Recruiter recruiter = getRecruiter(userDetails);
        Company company = recruiter.getCompany();
        if (company != null) {
            if (companyName != null && !companyName.isBlank()) company.setName(companyName.trim());
            company.setWebsite(website);
            company.setIndustry(industry);
            company.setSizeBand(sizeBand);
            company.setDescription(description);
            companyRepository.save(company);
        }
        if (jobTitle != null && !jobTitle.isBlank()) recruiter.setJobTitle(jobTitle.trim());
        if (phoneNumber != null && !phoneNumber.isBlank()) recruiter.setPhoneNumber(phoneNumber.trim());
        recruiterRepository.save(recruiter);
        ra.addFlashAttribute("success", "Company profile updated.");
        return "redirect:/recruiter/company";
    }
}
