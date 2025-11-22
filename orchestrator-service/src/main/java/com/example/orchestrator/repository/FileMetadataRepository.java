package com.example.orchestrator.repository;

import com.example.orchestrator.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {
    
    List<FileMetadata> findByUploadStatus(String uploadStatus);
    
    List<FileMetadata> findByExtractionStatus(String extractionStatus);
    
    List<FileMetadata> findByIndexingStatus(String indexingStatus);
    
    Optional<FileMetadata> findByChecksum(String checksum);
    
    List<FileMetadata> findByOriginalFilenameContaining(String filename);
    
    long countByUploadStatus(String uploadStatus);
}
