package com.example.repository;

import com.example.model.InviteStatus;
import com.example.model.Job;
import com.example.model.JobInvite;
import com.example.model.Jobseeker;
import com.example.model.recruiter.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface JobInviteRepository extends JpaRepository<JobInvite, Long> {

    List<JobInvite> findByJobseeker(Jobseeker jobseeker);

    boolean existsByJobAndJobseeker(Job job, Jobseeker jobseeker);

    List<JobInvite> findByJob_RecruiterOrderByCreatedAtDesc(Recruiter recruiter);

    long countByJob_Recruiter(Recruiter recruiter);

    /** Decline all PENDING invites for a jobseeker — called when they pause their profile. */
    @Modifying
    @Transactional
    @Query("UPDATE JobInvite i SET i.status = com.example.model.InviteStatus.DECLINED WHERE i.jobseeker = :jobseeker AND i.status = com.example.model.InviteStatus.PENDING")
    int declineAllPendingForJobseeker(@Param("jobseeker") Jobseeker jobseeker);

    /** True if this recruiter has at least one ACCEPTED invite with the given jobseeker (across any job). */
    @Query("""
        SELECT COUNT(i) > 0 FROM JobInvite i
        WHERE i.jobseeker.id = :jobseekerId
        AND i.job.recruiter = :recruiter
        AND i.status = :status
        """)
    boolean existsByJobseekerIdAndJob_RecruiterAndStatus(
            @Param("jobseekerId") Long jobseekerId,
            @Param("recruiter") Recruiter recruiter,
            @Param("status") InviteStatus status);

    void deleteByJobseeker(Jobseeker jobseeker);

    void deleteByJob(Job job);
}