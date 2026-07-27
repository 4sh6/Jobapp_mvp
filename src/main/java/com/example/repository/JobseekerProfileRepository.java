package com.example.repository;

import com.example.model.Jobseeker;
import com.example.model.JobseekerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JobseekerProfileRepository extends JpaRepository<JobseekerProfile, Long> {
    Optional<JobseekerProfile> findByJobseeker(Jobseeker jobseeker);

    /**
     * Skill terms are matched individually (candidate matches if ANY term matches),
     * because both job requiredSkills and candidate skills are comma-separated lists —
     * a single LIKE on the whole CSV would almost never match.
     * Up to 4 terms; unused slots must be passed as ''.
     */
    @Query(value = """
SELECT p FROM JobseekerProfile p
WHERE p.jobseeker.approvalStatus = 'APPROVED'
AND p.jobseeker.active = true
AND p.jobseeker.profilePaused = false
AND (
    (:s1 = '' AND :s2 = '' AND :s3 = '' AND :s4 = '')
    OR (:s1 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s1, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s1, '%'))))
    OR (:s2 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s2, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s2, '%'))))
    OR (:s3 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s3, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s3, '%'))))
    OR (:s4 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s4, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s4, '%'))))
)
AND (p.experienceYears IS NULL OR p.experienceYears BETWEEN :minExp AND :maxExp)
AND (
    p.jobseeker.hideFromCurrentEmployer = false
    OR p.currentCompany IS NULL
    OR :recruiterCompany IS NULL
    OR LOWER(p.currentCompany) != LOWER(:recruiterCompany)
)
ORDER BY
CASE WHEN p.jobseeker.approvedAt IS NULL THEN 1 ELSE 0 END ASC,
p.jobseeker.approvedAt DESC,
(CASE WHEN p.primarySkills IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.skills IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.about IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.currentRole IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.currentCompany IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.expectedCtc IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.experienceYears IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.highestEducation IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.preferredLocations IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.workMode IS NOT NULL THEN 1 ELSE 0 END) DESC
""",
           countQuery = """
SELECT COUNT(p) FROM JobseekerProfile p
WHERE p.jobseeker.approvalStatus = 'APPROVED'
AND p.jobseeker.active = true
AND p.jobseeker.profilePaused = false
AND (
    (:s1 = '' AND :s2 = '' AND :s3 = '' AND :s4 = '')
    OR (:s1 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s1, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s1, '%'))))
    OR (:s2 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s2, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s2, '%'))))
    OR (:s3 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s3, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s3, '%'))))
    OR (:s4 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s4, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s4, '%'))))
)
AND (p.experienceYears IS NULL OR p.experienceYears BETWEEN :minExp AND :maxExp)
AND (
    p.jobseeker.hideFromCurrentEmployer = false
    OR p.currentCompany IS NULL
    OR :recruiterCompany IS NULL
    OR LOWER(p.currentCompany) != LOWER(:recruiterCompany)
)
""")
    Page<JobseekerProfile> findMatchingCandidates(@Param("s1") String s1,
                                                  @Param("s2") String s2,
                                                  @Param("s3") String s3,
                                                  @Param("s4") String s4,
                                                  @Param("minExp") Integer minExp,
                                                  @Param("maxExp") Integer maxExp,
                                                  @Param("recruiterCompany") String recruiterCompany,
                                                  Pageable pageable);

    /** General browse — same ANY-skill-term semantics as findMatchingCandidates. */
    @Query(value = """
SELECT p FROM JobseekerProfile p
WHERE p.jobseeker.approvalStatus = 'APPROVED'
AND p.jobseeker.active = true
AND p.jobseeker.profilePaused = false
AND (
    (:s1 = '' AND :s2 = '' AND :s3 = '' AND :s4 = '')
    OR (:s1 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s1, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s1, '%'))))
    OR (:s2 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s2, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s2, '%'))))
    OR (:s3 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s3, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s3, '%'))))
    OR (:s4 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s4, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s4, '%'))))
)
AND (p.experienceYears IS NULL
     OR ((:minExp = 0 OR p.experienceYears >= :minExp) AND (:maxExp = 100 OR p.experienceYears <= :maxExp)))
AND (
    p.jobseeker.hideFromCurrentEmployer = false
    OR p.currentCompany IS NULL
    OR :recruiterCompany IS NULL
    OR LOWER(p.currentCompany) != LOWER(:recruiterCompany)
)
ORDER BY
CASE WHEN p.jobseeker.approvedAt IS NULL THEN 1 ELSE 0 END ASC,
p.jobseeker.approvedAt DESC,
(CASE WHEN p.primarySkills IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.skills IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.about IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.currentRole IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.currentCompany IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.expectedCtc IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.experienceYears IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.highestEducation IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.preferredLocations IS NOT NULL THEN 1 ELSE 0 END +
 CASE WHEN p.workMode IS NOT NULL THEN 1 ELSE 0 END) DESC
""",
           countQuery = """
SELECT COUNT(p) FROM JobseekerProfile p
WHERE p.jobseeker.approvalStatus = 'APPROVED'
AND p.jobseeker.active = true
AND p.jobseeker.profilePaused = false
AND (
    (:s1 = '' AND :s2 = '' AND :s3 = '' AND :s4 = '')
    OR (:s1 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s1, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s1, '%'))))
    OR (:s2 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s2, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s2, '%'))))
    OR (:s3 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s3, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s3, '%'))))
    OR (:s4 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s4, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s4, '%'))))
)
AND (p.experienceYears IS NULL
     OR ((:minExp = 0 OR p.experienceYears >= :minExp) AND (:maxExp = 100 OR p.experienceYears <= :maxExp)))
AND (
    p.jobseeker.hideFromCurrentEmployer = false
    OR p.currentCompany IS NULL
    OR :recruiterCompany IS NULL
    OR LOWER(p.currentCompany) != LOWER(:recruiterCompany)
)
""")
    Page<JobseekerProfile> browseWithFilters(@Param("s1") String s1,
                                             @Param("s2") String s2,
                                             @Param("s3") String s3,
                                             @Param("s4") String s4,
                                             @Param("minExp") Integer minExp,
                                             @Param("maxExp") Integer maxExp,
                                             @Param("recruiterCompany") String recruiterCompany,
                                             Pageable pageable);
}
