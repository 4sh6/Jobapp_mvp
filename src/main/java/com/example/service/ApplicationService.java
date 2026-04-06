
package com.example.service;

import com.example.model.Application;
import com.example.model.Job;
import com.example.model.Jobseeker;
import com.example.repositary.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    public Application apply(Job job, Jobseeker jobseeker, String source) {
        Optional<Application> existing = applicationRepository.findByJobAndJobseeker(job, jobseeker);
        if (existing.isPresent()) {
            return existing.get();
        }
        Application application = new Application();
        application.setJob(job);
        application.setJobseeker(jobseeker);
        application.setSource(source);
        return applicationRepository.save(application);
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
