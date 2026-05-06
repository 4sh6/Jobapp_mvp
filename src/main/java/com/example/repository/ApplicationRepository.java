
package com.example.repository;

import com.example.model.Application;
import com.example.model.Job;
import com.example.model.Jobseeker;
import com.example.model.recruiter.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByJobAndJobseeker(Job job, Jobseeker jobseeker);

    List<Application> findByJobseeker(Jobseeker jobseeker);

    List<Application> findByJob(Job job);

    List<Application> findByJobseekerOrderByAppliedDateDesc(Jobseeker jobseeker);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.recruiter = :recruiter")
    long countByJobRecruiter(@Param("recruiter") Recruiter recruiter);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.recruiter = :recruiter AND a.status = 'HIRED'")
    long countHiredByJobRecruiter(@Param("recruiter") Recruiter recruiter);
}
