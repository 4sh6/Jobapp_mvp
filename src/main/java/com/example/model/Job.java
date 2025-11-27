package com.example.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String company;
    private String location;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String requiredSkills;

    private Double minSalary;
    private Double maxSalary;

    private String jobType; // Full-time, Part-time, Contract, etc.
    private String workMode; // Remote, Onsite, Hybrid

    private LocalDateTime postedDate;

    private boolean active = true;
}