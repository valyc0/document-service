package com.example.ui.controller;

import com.example.ui.service.OrchestratorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/download")
@RequiredArgsConstructor
public class DownloadController {

    private final OrchestratorClient orchestratorClient;

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        try {
            log.info("Download request for fileId: {}", fileId);
            
            // Get file metadata
            var metadata = orchestratorClient.getDocument(fileId);
            
            // Download file bytes
            byte[] fileData = orchestratorClient.downloadFile(fileId);
            
            log.info("Downloaded {} bytes for file: {}", fileData.length, metadata.getOriginalFilename());
            
            ByteArrayResource resource = new ByteArrayResource(fileData);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                    .contentType(MediaType.parseMediaType(
                            metadata.getContentType() != null ? 
                            metadata.getContentType() : "application/octet-stream"))
                    .contentLength(fileData.length)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error downloading file: {}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{fileId}/text")
    public ResponseEntity<Resource> downloadExtractedText(@PathVariable String fileId) {
        try {
            log.info("Download text request for fileId: {}", fileId);
            
            // Get file metadata
            var metadata = orchestratorClient.getDocument(fileId);
            
            // Download extracted text
            byte[] textData = orchestratorClient.downloadExtractedText(fileId);
            
            log.info("Downloaded {} bytes of extracted text for file: {}", textData.length, metadata.getOriginalFilename());
            
            ByteArrayResource resource = new ByteArrayResource(textData);
            
            String filename = metadata.getOriginalFilename().replaceFirst("[.][^.]+$", "") + "_transcription.txt";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(textData.length)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error downloading extracted text: {}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }
}
