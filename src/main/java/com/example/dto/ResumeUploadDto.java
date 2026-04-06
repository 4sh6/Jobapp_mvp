
package com.example.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class ResumeUploadDto {

    @NotNull
    private MultipartFile file;

    private String jobTypes;

    private String industries;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
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
