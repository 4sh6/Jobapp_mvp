package com.example.controller;
import com.example.model.Job;
import com.example.model.Jobseeker;
import com.example.model.JobseekerProfile;
import com.example.model.Resume; // Add this import
import com.example.repositary.JobseekerProfileRepository;
import com.example.repositary.JobseekerRepository;
import com.example.repositary.ResumeRepository;
import com.example.service.EmailService;
import com.example.service.JobRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File; // Add this import
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import jakarta.servlet.http.HttpSession;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


@Controller
@RequestMapping("/jobseeker")
public class JobseekerController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private JobseekerProfileRepository profileRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JobRecommendationService jobRecommendationService;

    // File storage location
    private final String uploadDir = "./uploads/resumes";


    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("jobseeker", new Jobseeker());
        return "jobseeker/register";
    }

    @PostMapping("/register")
    public String registerJobseeker(Jobseeker jobseeker, HttpSession session) {

        // Save the jobseeker to the database
        jobseekerRepository.save(jobseeker);

        // Store jobseeker ID in session for later use
        session.setAttribute("jobseekerId", jobseeker.getId());

        return "redirect:/jobseeker/profile-details";
    }

    @GetMapping("/profile-details")
    public String showProfileDetailsForm(HttpSession session, Model model) {
        Long jobseekerId = (Long) session.getAttribute("jobseekerId");
        if (jobseekerId == null) {
            return "redirect:/jobseeker/register";
        }

        model.addAttribute("jobseekerId", jobseekerId);
        return "jobseeker/profiledetails";
    }

    @PostMapping("/profile-details")
    public String saveProfileDetails(@RequestParam Map<String, String> formData, HttpSession session) {
        // First try to get jobseekerId from session
        Long jobseekerId = (Long) session.getAttribute("jobseekerId");

        // If not in session, try to get from form data
        if (jobseekerId == null && formData.containsKey("jobseekerId")) {
            try {
                jobseekerId = Long.parseLong(formData.get("jobseekerId"));
            } catch (NumberFormatException e) {
                // Handle parsing error properly
                return "redirect:/jobseeker/register";
            }
        }

        if (jobseekerId == null) {
            return "redirect:/jobseeker/register";
        }

        // Get the jobseeker
        Jobseeker jobseeker = jobseekerRepository.findById(jobseekerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create and populate profile
        JobseekerProfile profile = new JobseekerProfile();
        profile.setJobseeker(jobseeker);

        // Parse and set form data
        profile.setExperienceYears(Integer.parseInt(formData.getOrDefault("experienceYears", "0")));
        profile.setExperienceMonths(Integer.parseInt(formData.getOrDefault("experienceMonths", "0")));
        profile.setCurrentCompany(formData.get("currentCompany"));
        profile.setCurrentRole(formData.get("currentRole"));

        // Parse numeric values safely
        try {
            if (formData.get("currentCtc") != null && !formData.get("currentCtc").isEmpty()) {
                profile.setCurrentCtc(Double.parseDouble(formData.get("currentCtc")));
            }
            if (formData.get("expectedCtc") != null && !formData.get("expectedCtc").isEmpty()) {
                profile.setExpectedCtc(Double.parseDouble(formData.get("expectedCtc")));
            }
        } catch (NumberFormatException e) {
            // Handle parsing error
        }

        profile.setSkills(formData.get("primarySkills"));
        profile.setHighestEducation(formData.get("highestEducation"));
        profile.setInstitution(formData.get("institution"));
        profile.setFieldOfStudy(formData.get("fieldOfStudy"));

        try {
            if (formData.get("graduationYear") != null && !formData.get("graduationYear").isEmpty()) {
                profile.setGraduationYear(Integer.parseInt(formData.get("graduationYear")));
            }
        } catch (NumberFormatException e) {
            // Handle parsing error
        }

        // Handle multi-select and checkbox fields
        String[] locations = formData.get("preferredLocations") != null ?
                formData.get("preferredLocations").split(",") : new String[0];
        profile.setPreferredLocations(String.join(",", locations));

        profile.setWorkMode(formData.get("workMode"));

        // Save the profile
        profileRepository.save(profile);

        return "redirect:/jobseeker/resume-upload";
    }

//    @GetMapping("/login")
//    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
//                                Model model) {
//        if (error != null) {
//            model.addAttribute("errorMessage", "Email or password is not correct.");
//        }
//        return "jobseeker/login";
//    }

    // Updated OTP endpoints with HttpSession
    @PostMapping("/send-otp")
    @ResponseBody
    public Map<String, Object> sendOtp(@RequestBody Map<String, String> requestBody, HttpSession session) {
        String email = requestBody.get("email");

        // Generate OTP (6-digit number)
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Store OTP in session
        session.setAttribute("otp_for_" + email, otp);

        // Send email with OTP
        try {
            emailService.sendOtpEmail(email, otp);
            System.out.println("Generated OTP for " + email + ": " + otp);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return response;
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send verification email");
            return response;
        }
    }

    @PostMapping("/verify-otp")
    @ResponseBody
    public Map<String, Object> verifyOtp(HttpSession session, @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String submittedOtp = request.get("otp");

        // Retrieve stored OTP from session
        String storedOtp = (String) session.getAttribute("otp_for_" + email);

        boolean verified = storedOtp != null && storedOtp.equals(submittedOtp);

        Map<String, Object> response = new HashMap<>();
        response.put("success", verified);

        if (verified) {
            // Remove the OTP from session after successful verification
            session.removeAttribute("otp_for_" + email);
        }

        return response;
    }

    @GetMapping("/resume-upload")
    public String showResumeUploadForm(HttpSession session, Model model) {
        Long jobseekerId = (Long) session.getAttribute("jobseekerId");
        if (jobseekerId == null) {
            return "redirect:/jobseeker/login";
        }

        model.addAttribute("jobseekerId", jobseekerId);
        return "jobseeker/resume-upload";
    }

    @PostMapping("/resume-upload")
    public String handleResumeUpload(
            @RequestParam("resumeFile") MultipartFile file,
            @RequestParam Map<String, String> formData,
            @RequestParam(value = "jobTypes", required = false) String[] jobTypes,
            @RequestParam(value = "industries", required = false) String[] industries,
            HttpSession session) throws IOException {

        Long jobseekerId = (Long) session.getAttribute("jobseekerId");
        if (jobseekerId == null) {
            return "redirect:/jobseeker/login";
        }

        // Get the jobseeker
        Jobseeker jobseeker = jobseekerRepository.findById(jobseekerId)
                .orElseThrow(() -> new RuntimeException("Jobseeker not found"));

        // Create directory if it doesn't exist
        Path directoryPath = Paths.get(uploadDir);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        // Create a unique file name
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        String fileName = jobseekerId + "_" + System.currentTimeMillis() + fileExtension;
        String filePath = uploadDir + "/" + fileName;

        // Save the file using nio
        Path destinationPath = Paths.get(filePath);
        Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

        // Create and save resume entity
        Resume resume = new Resume();
        resume.setJobseeker(jobseeker);
        resume.setFileName(originalFileName);
        resume.setFileType(file.getContentType());
        resume.setFilePath(filePath);
        resume.setNoticePeriod(formData.get("noticePeriod"));

        // Handle multi-select fields
        if (jobTypes != null) {
            resume.setJobTypes(String.join(",", jobTypes));
        }

        if (industries != null) {
            resume.setIndustries(String.join(",", industries));
        }

        resumeRepository.save(resume);

        return "redirect:/jobseeker/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        // Get the current logged-in jobseeker
        Jobseeker jobseeker = jobseekerRepository.findByEmail(principal.getName());
        model.addAttribute("jobseeker", jobseeker);

        // Get the jobseeker's profile
        JobseekerProfile profile = profileRepository.findById(jobseeker.getId()).orElse(null);

        // Get recommended jobs based on skills
        List<Job> recommendedJobs = jobRecommendationService.getRecommendedJobs(profile);
        model.addAttribute("recommendedJobs", recommendedJobs);

        return "jobseeker/dashboard";
    }
}