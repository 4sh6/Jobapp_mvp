
package com.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "jobseeker_resumes")
public class Resume {

    @Id
    private Long id; // same as Jobseeker id

    @OneToOne
    @MapsId
    @JoinColumn(name = "jobseeker_id")
    private Jobseeker jobseeker;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "job_types")
    private String jobTypes;

    @Column(name = "industries")
    private String industries;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Jobseeker getJobseeker() {
        return jobseeker;
    }

    public void setJobseeker(Jobseeker jobseeker) {
        this.jobseeker = jobseeker;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getJobTypes() {
        return jobTypes;
    }

    public void setJobTypes(String jobTypes) {
        this.jobTypes = jobTypes;
    }

    public String getIndustries() {
        return industries;
    }

    public void setIndustries(String industries) {
        this.industries = industries;
    }
}
