package com.example.orchestrator.controller;

import com.example.orchestrator.entity.FileMetadata;
import com.example.orchestrator.service.DocumentUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentUploadService uploadService;
    private final com.example.orchestrator.repository.FileMetadataRepository repository;
    private final com.example.orchestrator.client.IndexingServiceClient indexingServiceClient;
    
    /**
     * Upload document
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            log.info("📥 Received upload request for file: {} ({} bytes)", 
                    file.getOriginalFilename(), file.getSize());
            
            FileMetadata metadata = uploadService.uploadDocument(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileId", metadata.getId());
            response.put("filename", metadata.getOriginalFilename());
            response.put("status", metadata.getUploadStatus());
            response.put("message", "File uploaded and queued for extraction");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error uploading document", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Get file status
     */
    @GetMapping("/{fileId}/status")
    public ResponseEntity<?> getStatus(@PathVariable String fileId) {
        try {
            FileMetadata metadata = uploadService.getFileMetadata(fileId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileId", metadata.getId());
            response.put("filename", metadata.getOriginalFilename());
            response.put("uploadStatus", metadata.getUploadStatus());
            response.put("extractionStatus", metadata.getExtractionStatus());
            response.put("indexingStatus", metadata.getIndexingStatus());
            response.put("uploadedAt", metadata.getUploadedAt());
            response.put("extractionStartedAt", metadata.getExtractionStartedAt());
            response.put("extractionCompletedAt", metadata.getExtractionCompletedAt());
            response.put("indexingStartedAt", metadata.getIndexingStartedAt());
            response.put("indexingCompletedAt", metadata.getIndexingCompletedAt());
            
            if (metadata.getExtractionError() != null) {
                response.put("extractionError", metadata.getExtractionError());
            }
            if (metadata.getIndexingError() != null) {
                response.put("indexingError", metadata.getIndexingError());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "File not found: " + fileId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get file metadata
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<FileMetadata> getFileMetadata(@PathVariable String fileId) {
        try {
            FileMetadata metadata = uploadService.getFileMetadata(fileId);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Download original file
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileId) {
        try {
            FileMetadata metadata = uploadService.getFileMetadata(fileId);
            InputStream inputStream = uploadService.downloadFile(fileId);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                    .contentType(MediaType.parseMediaType(
                            metadata.getContentType() != null ? 
                            metadata.getContentType() : "application/octet-stream"))
                    .body(new InputStreamResource(inputStream));
                    
        } catch (Exception e) {
            log.error("❌ Error downloading file: {}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Download extracted text (transcription)
     */
    @GetMapping("/{fileId}/download-text")
    public ResponseEntity<InputStreamResource> downloadExtractedText(@PathVariable String fileId) {
        try {
            FileMetadata metadata = uploadService.getFileMetadata(fileId);
            
            if (metadata.getMinioPathExtracted() == null) {
                log.warn("⚠️ No extracted text available for file: {}", fileId);
                return ResponseEntity.notFound().build();
            }
            
            InputStream inputStream = uploadService.downloadExtractedText(fileId);
            String filename = metadata.getOriginalFilename().replaceFirst("[.][^.]+$", "") + "_transcription.txt";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new InputStreamResource(inputStream));
                    
        } catch (Exception e) {
            log.error("❌ Error downloading extracted text: {}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * List all documents
     */
    @GetMapping
    public ResponseEntity<List<FileMetadata>> listDocuments(
            @RequestParam(required = false) String status) {
        try {
            List<FileMetadata> documents;
            if (status != null) {
                documents = repository.findByUploadStatus(status);
            } else {
                documents = repository.findAll();
            }
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("❌ Error listing documents", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", repository.count());
            stats.put("uploaded", repository.countByUploadStatus("UPLOADED"));
            stats.put("extracted", repository.countByUploadStatus("EXTRACTED"));
            stats.put("indexed", repository.countByUploadStatus("INDEXED"));
            stats.put("failed", repository.countByUploadStatus("FAILED"));
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete document by ID (DB + MinIO + Elasticsearch)
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String fileId) {
        try {
            FileMetadata metadata = uploadService.getFileMetadata(fileId);
            
            // 1. Delete from MinIO (original file + extracted text)
            uploadService.deleteFromMinIO(fileId, metadata.getMinioPathOriginal(), metadata.getMinioPathExtracted());
            
            // 2. Delete from Elasticsearch
            try {
                indexingServiceClient.deleteDocument(fileId);
                log.info("🗑️ Deleted from Elasticsearch: {}", fileId);
            } catch (Exception e) {
                log.warn("⚠️ Could not delete from Elasticsearch (might not be indexed yet): {}", fileId);
            }
            
            // 3. Delete from DB
            repository.deleteById(fileId);
            
            Map<String, Object> response = Map.of(
                "message", "Document deleted completely",
                "fileId", fileId,
                "filename", metadata.getOriginalFilename()
            );
            
            log.info("✅ Document deleted completely: {} ({})", fileId, metadata.getOriginalFilename());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error deleting document: {}", fileId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Delete all failed documents
     */
    @DeleteMapping("/failed")
    public ResponseEntity<Map<String, Object>> deleteFailedDocuments() {
        try {
            List<FileMetadata> failed = repository.findByUploadStatus("FAILED");
            failed.forEach(repository::delete);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed documents deleted");
            response.put("count", failed.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error deleting failed documents", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
