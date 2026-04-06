package com.example.repositary;

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

    @Query("""
SELECT p FROM JobseekerProfile p
WHERE (:skills = '' OR LOWER(p.skills) LIKE LOWER(CONCAT('%', :skills, '%'))
                    OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :skills, '%')))
AND p.experienceYears BETWEEN :minExp AND :maxExp
""")
    Page<JobseekerProfile> findMatchingCandidates(@Param("skills") String skills,
                                                  @Param("minExp") Integer minExp,
                                                  @Param("maxExp") Integer maxExp,
                                                  Pageable pageable);

    @Query("""
SELECT p FROM JobseekerProfile p
WHERE (:skills = '' OR LOWER(p.skills) LIKE LOWER(CONCAT('%', :skills, '%'))
                    OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :skills, '%')))
AND (:minExp = 0 OR p.experienceYears >= :minExp)
AND (:maxExp = 100 OR p.experienceYears <= :maxExp)
""")
    Page<JobseekerProfile> browseWithFilters(@Param("skills") String skills,
                                             @Param("minExp") Integer minExp,
                                             @Param("maxExp") Integer maxExp,
                                             Pageable pageable);
}