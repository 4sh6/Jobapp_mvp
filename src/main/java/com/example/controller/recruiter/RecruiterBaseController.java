package com.example.controller.recruiter;

import com.example.model.recruiter.Recruiter;
import com.example.repository.recruiter.RecruiterRepository;
import com.example.constant.ActivationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Shared helpers for recruiter controllers.
 */
abstract class RecruiterBaseController {

    @Autowired
    protected RecruiterRepository recruiterRepository;

    /** Loads recruiter and enforces activation + suspension checks. */
    protected Recruiter getActiveRecruiter(UserDetails userDetails) {
        Recruiter recruiter = recruiterRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (recruiter.isSuspended()) {
            throw new IllegalStateException("Your account has been suspended. Contact support.");
        }
        if (!ActivationStatus.ACTIVE.equals(recruiter.getActivationStatus())) {
            throw new IllegalStateException("Your account is pending admin approval.");
        }
        return recruiter;
    }

    /** Returns recruiter regardless of status — for read-only/profile pages. */
    protected Recruiter getRecruiter(UserDetails userDetails) {
        return recruiterRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }
}
