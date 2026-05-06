package com.example.service.recruiter;

import com.example.dto.RecruiterRegistrationDto;
import com.example.model.Company;
import com.example.model.recruiter.Recruiter;
import com.example.repository.CompanyRepository;
import com.example.repository.recruiter.RecruiterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RecruiterService {

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Recruiter registerRecruiter(RecruiterRegistrationDto dto) {

        // Prevent duplicate recruiter email
        if (recruiterRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Reuse existing company if present
        Company company = companyRepository.findByName(dto.getCompanyName())
                .orElseGet(() -> {
                    Company newCompany = new Company();
                    newCompany.setName(dto.getCompanyName());
                    newCompany.setWebsite(dto.getWebsite());
                    return companyRepository.save(newCompany);
                });

        Recruiter recruiter = new Recruiter();
        recruiter.setName(dto.getName());
        recruiter.setEmail(dto.getEmail());
        recruiter.setPassword(passwordEncoder.encode(dto.getPassword()));
        recruiter.setCompany(company);
        recruiter.setJobTitle(dto.getJobTitle());
        recruiter.setPhoneNumber(dto.getPhoneNumber());

        // Default status: Pending until admin approval
        recruiter.setActivationStatus("Pending");

        return recruiterRepository.save(recruiter);
    }
}