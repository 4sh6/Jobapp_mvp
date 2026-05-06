package com.example.repository;

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
            LEFT JOIN j.recruiter r
            LEFT JOIN r.company c
            WHERE j.active = true AND j.status = 'PUBLISHED'
            AND (:q IS NULL OR :q = '' OR
                LOWER(j.title)          LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(j.requiredSkills) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(j.location)       LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(j.description)    LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(c.name)           LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (:workMode  IS NULL OR :workMode  = '' OR j.workMode = :workMode)
            AND (:jobType   IS NULL OR :jobType   = '' OR j.jobType  = :jobType)
            AND (:expMin    IS NULL OR j.experienceMax >= :expMin)
            AND (:expMax    IS NULL OR j.experienceMin <= :expMax)
            AND (:salaryMin IS NULL OR j.salaryMax     >= :salaryMin)
            AND (:salaryMax IS NULL OR j.salaryMin     <= :salaryMax)
            """)
    Page<Job> filterJobs(@Param("q")         String q,
                         @Param("workMode")   String workMode,
                         @Param("jobType")    String jobType,
                         @Param("expMin")     Integer expMin,
                         @Param("expMax")     Integer expMax,
                         @Param("salaryMin")  Integer salaryMin,
                         @Param("salaryMax")  Integer salaryMax,
                         Pageable pageable);

    Page<Job> findByRecruiter(Recruiter recruiter, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.active = true AND j.status = 'PUBLISHED'")
    Page<Job> listActiveJobs(Pageable pageable);

    /** Admin list — eagerly fetches recruiter + company to avoid LazyInitializationException */
    @Query(value = "SELECT j FROM Job j LEFT JOIN FETCH j.recruiter r LEFT JOIN FETCH r.company",
           countQuery = "SELECT COUNT(j) FROM Job j")
    Page<Job> findAllWithRecruiter(Pageable pageable);
}