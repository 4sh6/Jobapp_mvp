
package com.example.service;

import com.example.model.Application;
import com.example.model.Job;
import com.example.model.Jobseeker;
import com.example.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    public Application apply(Job job, Jobseeker jobseeker, String source) {
        if (!job.isActive() || !"PUBLISHED".equals(job.getStatus())) {
            throw new IllegalStateException("This job is no longer accepting applications.");
        }
        Optional<Application> existing = applicationRepository.findByJobAndJobseeker(job, jobseeker);
        if (existing.isPresent()) {
            throw new IllegalStateException("You have already applied to this job.");
        }
        Application application = new Application();
        application.setJob(job);
        application.setJobseeker(jobseeker);
        application.setSource(source);
        return applicationRepository.save(application);
    }

    public boolean hasApplied(Job job, Jobseeker jobseeker) {
        return applicationRepository.findByJobAndJobseeker(job, jobseeker).isPresent();
    }

    public List<Application> findByJobseeker(Jobseeker jobseeker) {
        return applicationRepository.findByJobseeker(jobseeker);
    }

    public List<Application> findByJob(Job job) {
        return applicationRepository.findByJob(job);
    }

    public Optional<Application> findById(Long id) {
        return applicationRepository.findById(id);
    }

    public Application save(Application application) {
        return applicationRepository.save(application);
    }
}
