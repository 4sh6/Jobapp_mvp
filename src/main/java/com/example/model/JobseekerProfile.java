package com.example.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jobseeker_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobseekerProfile {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "jobseeker_id")
    private Jobseeker jobseeker;

    private Integer experienceYears;
    private Integer experienceMonths;
    private String currentCompany;

    @Column(name = "current_jobposition")
    private String currentRole;

    private Double currentCtc;
    private Double expectedCtc;

    @Column(length = 500)
    private String skills;

    private String highestEducation;
    private String institution;
    private String fieldOfStudy;
    private Integer graduationYear;

    @Column(length = 500)
    private String preferredLocations;

    private String workMode;
}