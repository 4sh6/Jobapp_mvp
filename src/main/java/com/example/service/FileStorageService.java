package com.example.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get(System.getProperty("user.dir"), "uploads", "resumes")
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file, Long jobseekerId) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Failed to store empty file.");
            }

            // SECURITY: use a random UUID filename to prevent path traversal.
            // The original filename is stored in the Resume entity for display/download purposes.
            String fileName = "resume_" + jobseekerId + "_" + UUID.randomUUID() + ".pdf";
            Path targetLocation = this.fileStorageLocation.resolve(fileName).normalize();

            // SECURITY: ensure the resolved path stays within the upload directory
            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new IllegalArgumentException("Invalid file path detected.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    /**
     * Load a resume file as a Spring Resource.
     * @param fileName the stored file name (from Resume.fileName)
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

            // SECURITY: prevent path traversal — reject any path that escapes the upload directory
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new RuntimeException("Access denied: invalid file path.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("File not found.");
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found.", ex);
        }
    }
}
