package com.example.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jobseeker_resumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resume {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "jobseeker_id")
    private Jobseeker jobseeker;

    private String fileName;
    private String fileType;
    private String filePath;

    private String noticePeriod;

    @Column(length = 500)
    private String jobTypes;

    @Column(length = 500)
    private String industries;
}