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
            WHERE j.active = true AND j.status = 'PUBLISHED'
            AND (:q IS NULL OR :q = '' OR
                LOWER(j.title)          LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(j.requiredSkills) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(j.location)       LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(j.description)    LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (:workMode IS NULL OR :workMode = '' OR j.workMode = :workMode)
            AND (:expMax  IS NULL OR j.experienceMin <= :expMax)
            AND (:expMin  IS NULL OR j.experienceMax >= :expMin)
            """)
    Page<Job> filterJobs(@Param("q")       String q,
                         @Param("workMode") String workMode,
                         @Param("expMin")   Integer expMin,
                         @Param("expMax")   Integer expMax,
                         Pageable pageable);

    Page<Job> findByRecruiter(Recruiter recruiter, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.active = true AND j.status = 'PUBLISHED'")
    Page<Job> listActiveJobs(Pageable pageable);
}