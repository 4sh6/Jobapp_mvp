
package com.example.repository;

import com.example.model.Jobseeker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface JobseekerRepository extends JpaRepository<Jobseeker, Long> {
    Optional<Jobseeker> findByEmail(String email);

    Optional<Jobseeker> findByReferralCode(String referralCode);

    @Query("SELECT j FROM Jobseeker j WHERE j.approvalStatus = :status " +
           "OR (:status = 'PENDING' AND j.approvalStatus IS NULL) " +
           "ORDER BY j.createdAt DESC")
    List<Jobseeker> findByApprovalStatusOrderByCreatedAtDesc(@Param("status") String approvalStatus);

    /**
     * Same status filtering as findByApprovalStatusOrderByCreatedAtDesc, plus optional skill and
     * experience-range filters against the candidate's profile (LEFT JOIN so candidates who
     * haven't filled out a profile yet still show up when no filter is applied).
     * Skill terms use ANY-match semantics; unused slots must be passed as ''.
     */
    @Query("""
SELECT DISTINCT j FROM Jobseeker j
LEFT JOIN JobseekerProfile p ON p.jobseeker = j
WHERE (j.approvalStatus = :status OR (:status = 'PENDING' AND j.approvalStatus IS NULL))
AND (
    (:s1 = '' AND :s2 = '' AND :s3 = '' AND :s4 = '')
    OR (:s1 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s1, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s1, '%'))))
    OR (:s2 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s2, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s2, '%'))))
    OR (:s3 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s3, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s3, '%'))))
    OR (:s4 <> '' AND (LOWER(p.skills) LIKE LOWER(CONCAT('%', :s4, '%')) OR LOWER(p.primarySkills) LIKE LOWER(CONCAT('%', :s4, '%'))))
)
AND (p.experienceYears IS NULL
     OR ((:minExp = 0 OR p.experienceYears >= :minExp) AND (:maxExp = 100 OR p.experienceYears <= :maxExp)))
ORDER BY j.createdAt DESC
""")
    List<Jobseeker> findByApprovalStatusWithFilters(@Param("status") String status,
                                                     @Param("s1") String s1,
                                                     @Param("s2") String s2,
                                                     @Param("s3") String s3,
                                                     @Param("s4") String s4,
                                                     @Param("minExp") Integer minExp,
                                                     @Param("maxExp") Integer maxExp);

    @Query("SELECT COUNT(j) FROM Jobseeker j WHERE j.approvalStatus = :status " +
           "OR (:status = 'PENDING' AND j.approvalStatus IS NULL)")
    long countByApprovalStatus(@Param("status") String approvalStatus);

    @Modifying
    @Transactional
    @Query("UPDATE Jobseeker j SET j.approvalStatus = 'PENDING' WHERE j.approvalStatus IS NULL")
    void backfillNullApprovalStatus();
}
