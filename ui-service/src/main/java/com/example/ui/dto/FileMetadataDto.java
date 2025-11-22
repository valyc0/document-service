package com.example.ui.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileMetadataDto {
    private String id;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private String uploadStatus;
    private String extractionStatus;
    private String indexingStatus;
    private LocalDateTime uploadedAt;
    private LocalDateTime extractionStartedAt;
    private LocalDateTime extractionCompletedAt;
    private LocalDateTime indexingStartedAt;
    private LocalDateTime indexingCompletedAt;
    private String extractionError;
    private String indexingError;
}
