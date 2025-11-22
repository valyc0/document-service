package com.example.orchestrator.service;

import com.example.orchestrator.entity.FileMetadata;
import com.example.orchestrator.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadService {
    
    private final FileMetadataRepository repository;
    private final MinioService minioService;
    private final MessagePublisherService messagePublisher;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    @PostConstruct
    public void init() {
        // Ensure MinIO bucket exists
        minioService.ensureBucketExists();
    }
    
    /**
     * Upload document and trigger processing pipeline
     */
    public FileMetadata uploadDocument(MultipartFile file) {
        try {
            // Generate unique file ID
            String fileId = UUID.randomUUID().toString();
            
            // Calculate checksum
            String checksum = calculateChecksum(file.getBytes());
            log.info("Calculated checksum: {} for file: {}", checksum, file.getOriginalFilename());
            
            // Check if file already exists (deduplication)
            var existing = repository.findByChecksum(checksum);
            if (existing.isPresent()) {
                log.info("⚠️ File with same checksum already exists: {}", checksum);
                return existing.get();
            }
            
            // Determine file extension
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // Upload to MinIO
            String minioPath = "files/" + fileId + "/original" + extension;
            minioService.uploadFile(
                    minioPath,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
            
            // Save metadata to H2
            FileMetadata metadata = new FileMetadata();
            metadata.setId(fileId);
            metadata.setOriginalFilename(originalFilename);
            metadata.setFileSize(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setChecksum(checksum);
            metadata.setMinioBucket(bucketName);
            metadata.setMinioPathOriginal(minioPath);
            metadata.setUploadStatus("UPLOADED");
            metadata.setExtractionStatus("PENDING");
            metadata.setUploadedAt(LocalDateTime.now());
            
            repository.save(metadata);
            log.info("✅ Saved file metadata to H2: {}", fileId);
            
            // Publish extraction request to RabbitMQ
            messagePublisher.publishExtractionRequest(fileId);
            
            return metadata;
            
        } catch (Exception e) {
            log.error("❌ Error uploading document", e);
            throw new RuntimeException("Failed to upload document", e);
        }
    }
    
    /**
     * Get file metadata by ID
     */
    public FileMetadata getFileMetadata(String fileId) {
        return repository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
    }
    
    /**
     * Download original file
     */
    public InputStream downloadFile(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        return minioService.downloadFile(metadata.getMinioPathOriginal());
    }
    
    /**
     * Download extracted text
     */
    public InputStream downloadExtractedText(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (metadata.getMinioPathExtracted() == null) {
            throw new RuntimeException("Extracted text not available for file: " + fileId);
        }
        return minioService.downloadFile(metadata.getMinioPathExtracted());
    }
    
    /**
     * Calculate SHA-256 checksum
     */
    private String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error calculating checksum", e);
            return null;
        }
    }
    
    /**
     * Delete file from MinIO (original + extracted text)
     */
    public void deleteFromMinIO(String fileId, String originalPath, String extractedTextPath) {
        try {
            // Delete original file
            if (originalPath != null) {
                minioService.deleteFile(originalPath);
                log.info("🗑️ Deleted original from MinIO: {}", originalPath);
            }
            
            // Delete extracted text
            if (extractedTextPath != null) {
                minioService.deleteFile(extractedTextPath);
                log.info("🗑️ Deleted extracted text from MinIO: {}", extractedTextPath);
            }
        } catch (Exception e) {
            log.error("❌ Error deleting from MinIO: {}", fileId, e);
        }
    }
}
