package com.example.orchestrator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_upload_status", columnList = "uploadStatus"),
    @Index(name = "idx_extraction_status", columnList = "extractionStatus"),
    @Index(name = "idx_indexing_status", columnList = "indexingStatus"),
    @Index(name = "idx_uploaded_at", columnList = "uploadedAt"),
    @Index(name = "idx_checksum", columnList = "checksum")
})
public class FileMetadata {
    
    @Id
    @Column(length = 36)
    private String id; // UUID
    
    // File Info
    @Column(nullable = false)
    private String originalFilename;
    
    @Column(nullable = false)
    private Long fileSize;
    
    private String contentType;
    
    @Column(length = 64)
    private String checksum; // SHA-256
    
    // MinIO Storage Paths
    @Column(nullable = false)
    private String minioBucket;
    
    @Column(nullable = false, length = 500)
    private String minioPathOriginal;
    
    @Column(length = 500)
    private String minioPathExtracted;
    
    // Status Tracking
    @Column(nullable = false, length = 50)
    private String uploadStatus; // UPLOADED, EXTRACTING, EXTRACTED, INDEXING, INDEXED, FAILED
    
    @Column(length = 50)
    private String extractionStatus; // PENDING, IN_PROGRESS, COMPLETED, FAILED
    
    @Column(length = 50)
    private String indexingStatus; // PENDING, IN_PROGRESS, COMPLETED, FAILED
    
    // Timestamps
    @Column(nullable = false)
    private LocalDateTime uploadedAt;
    
    private LocalDateTime extractionStartedAt;
    private LocalDateTime extractionCompletedAt;
    private LocalDateTime indexingStartedAt;
    private LocalDateTime indexingCompletedAt;
    
    // Metadata from Tika (JSON)
    @Column(columnDefinition = "TEXT")
    private String extractedMetadata;
    
    // Error Handling
    @Column(columnDefinition = "TEXT")
    private String extractionError;
    
    @Column(columnDefinition = "TEXT")
    private String indexingError;
    
    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer retryCount = 0;
    
    // System Fields
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
