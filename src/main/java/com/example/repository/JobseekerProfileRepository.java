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

    @Query(value = """
SELECT p FROM JobseekerProfile p
WHERE p.jobseeker.approvalStatus = 'APPROVED'
AND p.jobseeker.profilePaused = false
AND (:skills = '' OR LOWER(p.skills) LIKE LOWER(CONCAT('%', :skills, '%'))
                  OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :skills, '%')))
AND p.experienceYears BETWEEN :minExp AND :maxExp
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
AND p.jobseeker.profilePaused = false
AND (:skills = '' OR LOWER(p.skills) LIKE LOWER(CONCAT('%', :skills, '%'))
                  OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :skills, '%')))
AND p.experienceYears BETWEEN :minExp AND :maxExp
AND (
    p.jobseeker.hideFromCurrentEmployer = false
    OR p.currentCompany IS NULL
    OR :recruiterCompany IS NULL
    OR LOWER(p.currentCompany) != LOWER(:recruiterCompany)
)
""")
    Page<JobseekerProfile> findMatchingCandidates(@Param("skills") String skills,
                                                  @Param("minExp") Integer minExp,
                                                  @Param("maxExp") Integer maxExp,
                                                  @Param("recruiterCompany") String recruiterCompany,
                                                  Pageable pageable);

    @Query(value = """
SELECT p FROM JobseekerProfile p
WHERE p.jobseeker.approvalStatus = 'APPROVED'
AND p.jobseeker.profilePaused = false
AND (:skills = '' OR LOWER(p.skills) LIKE LOWER(CONCAT('%', :skills, '%'))
                  OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :skills, '%')))
AND (:minExp = 0 OR p.experienceYears >= :minExp)
AND (:maxExp = 100 OR p.experienceYears <= :maxExp)
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
AND p.jobseeker.profilePaused = false
AND (:skills = '' OR LOWER(p.skills) LIKE LOWER(CONCAT('%', :skills, '%'))
                  OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :skills, '%')))
AND (:minExp = 0 OR p.experienceYears >= :minExp)
AND (:maxExp = 100 OR p.experienceYears <= :maxExp)
AND (
    p.jobseeker.hideFromCurrentEmployer = false
    OR p.currentCompany IS NULL
    OR :recruiterCompany IS NULL
    OR LOWER(p.currentCompany) != LOWER(:recruiterCompany)
)
""")
    Page<JobseekerProfile> browseWithFilters(@Param("skills") String skills,
                                             @Param("minExp") Integer minExp,
                                             @Param("maxExp") Integer maxExp,
                                             @Param("recruiterCompany") String recruiterCompany,
                                             Pageable pageable);
}
