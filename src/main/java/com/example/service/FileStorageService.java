package com.example.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Résumé storage. Uses Cloudflare R2 (S3-compatible) when R2 credentials are configured;
 * falls back to local disk under ./uploads/resumes otherwise (dev default — no R2 account needed).
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${r2.account-id:}")
    private String accountId;

    @Value("${r2.access-key-id:}")
    private String accessKeyId;

    @Value("${r2.secret-access-key:}")
    private String secretAccessKey;

    @Value("${r2.bucket-name:}")
    private String bucketName;

    private S3Client s3Client;
    private boolean r2Enabled;
    private Path localStorageLocation;

    @PostConstruct
    private void init() {
        r2Enabled = !accountId.isBlank() && !accessKeyId.isBlank() && !secretAccessKey.isBlank();

        if (r2Enabled) {
            s3Client = S3Client.builder()
                    .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                    // R2 has no real AWS regions; "auto" is Cloudflare's documented placeholder.
                    .region(Region.of("auto"))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    // R2 requires path-style access (bucket in the URL path, not the hostname).
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
            log.info("Résumé storage: using Cloudflare R2 bucket '{}'", bucketName);
        } else {
            this.localStorageLocation = Paths.get(System.getProperty("user.dir"), "uploads", "resumes")
                    .toAbsolutePath().normalize();
            try {
                Files.createDirectories(this.localStorageLocation);
            } catch (IOException ex) {
                throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
            }
            log.info("Résumé storage: R2 not configured — using local disk at {} (dev fallback)", localStorageLocation);
        }
    }

    public String storeFile(MultipartFile file, Long jobseekerId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file.");
        }

        // SECURITY: use a random UUID filename to prevent path traversal.
        // The original filename is stored in the Resume entity for display/download purposes.
        String fileName = "resume_" + jobseekerId + "_" + UUID.randomUUID() + ".pdf";

        if (r2Enabled) {
            storeToR2(file, fileName);
        } else {
            storeToLocalDisk(file, fileName);
        }
        return fileName;
    }

    /**
     * Load a resume file as a Spring Resource.
     * @param fileName the stored file name (from Resume.fileName)
     */
    public Resource loadFileAsResource(String fileName) {
        return r2Enabled ? loadFromR2(fileName) : loadFromLocalDisk(fileName);
    }

    // ── Cloudflare R2 ──────────────────────────────────────────────────

    private void storeToR2(MultipartFile file, String fileName) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        } catch (S3Exception ex) {
            throw new RuntimeException("Could not upload file to storage. Please try again!", ex);
        }
    }

    private Resource loadFromR2(String fileName) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            return new InputStreamResource(s3Client.getObject(request));
        } catch (NoSuchKeyException ex) {
            throw new RuntimeException("File not found.", ex);
        } catch (S3Exception ex) {
            throw new RuntimeException("Could not retrieve file. Please try again!", ex);
        }
    }

    // ── Local disk (dev fallback) ──────────────────────────────────────

    private void storeToLocalDisk(MultipartFile file, String fileName) {
        try {
            Path targetLocation = this.localStorageLocation.resolve(fileName).normalize();

            // SECURITY: ensure the resolved path stays within the upload directory
            if (!targetLocation.startsWith(this.localStorageLocation)) {
                throw new IllegalArgumentException("Invalid file path detected.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    private Resource loadFromLocalDisk(String fileName) {
        try {
            Path filePath = this.localStorageLocation.resolve(fileName).normalize();

            // SECURITY: prevent path traversal — reject any path that escapes the upload directory
            if (!filePath.startsWith(this.localStorageLocation)) {
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
