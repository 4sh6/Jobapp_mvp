package com.example.controller.recruiter;

import com.example.constant.ActivationStatus;
import com.example.model.Job;
import com.example.model.recruiter.Recruiter;
import com.example.repository.ApplicationRepository;
import com.example.repository.JobInviteRepository;
import com.example.service.ApplicationService;
import com.example.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/recruiter")
public class RecruiterDashboardController extends RecruiterBaseController {

    @Autowired private JobService jobService;
    @Autowired private ApplicationService applicationService;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private JobInviteRepository inviteRepository;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
                            @PageableDefault(size = 10, sort = "postedDate", direction = Sort.Direction.DESC) Pageable pageable,
                            Model model) {
        if (userDetails == null) return "redirect:/recruiter/login";

        Recruiter recruiter = getRecruiter(userDetails);

        if (!ActivationStatus.ACTIVE.equals(recruiter.getActivationStatus())) {
            model.addAttribute("recruiter", recruiter);
            model.addAttribute("status", recruiter.getActivationStatus());
            return "recruiter/pending-approval";
        }

        Page<Job> jobsPage = jobService.listJobsByRecruiter(recruiter, pageable);

        Map<Long, Integer> applicationCounts = new HashMap<>();
        for (Job job : jobsPage.getContent()) {
            applicationCounts.put(job.getId(), applicationService.findByJob(job).size());
        }

        model.addAttribute("recruiter", recruiter);
        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("applicationCounts", applicationCounts);
        model.addAttribute("totalJobs", jobsPage.getTotalElements());
        model.addAttribute("totalApplicants", applicationRepository.countByJobRecruiter(recruiter));
        model.addAttribute("totalInvitesSent", inviteRepository.countByJob_Recruiter(recruiter));
        model.addAttribute("totalHired", applicationRepository.countHiredByJobRecruiter(recruiter));
        return "recruiter/dashboard";
    }
}
