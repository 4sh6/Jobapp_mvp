package com.example.repositary;

import com.example.model.Job;
import com.example.model.recruiter.Recruiter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, Long> {

    Page<Job> findByActiveTrueAndStatus(String status, Pageable pageable);

    @Query("""
SELECT j FROM Job j
WHERE j.active = true
AND j.status = 'PUBLISHED'
AND (
    LOWER(j.title) LIKE LOWER(CONCAT('%', :q, '%'))
    OR LOWER(j.requiredSkills) LIKE LOWER(CONCAT('%', :q, '%'))
)
""")
    Page<Job> findMatchingJobs(@Param("q") String q, Pageable pageable);

    Page<Job> findByRecruiter(Recruiter recruiter, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.active = true AND j.status = 'PUBLISHED'")
    Page<Job> listActiveJobs(Pageable pageable);
}