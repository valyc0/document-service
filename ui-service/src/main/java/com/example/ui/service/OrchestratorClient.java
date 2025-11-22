package com.example.ui.service;

import com.example.ui.dto.FileMetadataDto;
import com.example.ui.dto.SearchResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrchestratorClient {

    private final RestClient restClient;

    public OrchestratorClient(
            RestClient.Builder restClientBuilder,
            @Value("${orchestrator.url}") String orchestratorUrl) {
        this.restClient = restClientBuilder.baseUrl(orchestratorUrl).build();
    }

    /**
     * Upload document
     */
    public Map<String, Object> uploadDocument(MultipartFile file) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            return restClient.post()
                    .uri("/api/documents/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Error uploading document", e);
            throw new RuntimeException("Failed to upload document: " + e.getMessage());
        }
    }

    /**
     * List all documents
     */
    public List<FileMetadataDto> listDocuments() {
        return restClient.get()
                .uri("/api/documents")
                .retrieve()
                .body(new ParameterizedTypeReference<List<FileMetadataDto>>() {});
    }

    /**
     * Get document by ID
     */
    public FileMetadataDto getDocument(String fileId) {
        return restClient.get()
                .uri("/api/documents/{fileId}", fileId)
                .retrieve()
                .body(FileMetadataDto.class);
    }

    /**
     * Download original file
     */
    public byte[] downloadFile(String fileId) {
        try {
            log.info("Downloading file with ID: {}", fileId);
            byte[] response = restClient.get()
                    .uri("/api/documents/{fileId}/download", fileId)
                    .retrieve()
                    .body(byte[].class);
            log.info("Download successful, received {} bytes", response != null ? response.length : 0);
            return response;
        } catch (Exception e) {
            log.error("Error downloading file {}: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download extracted text (transcription)
     */
    public byte[] downloadExtractedText(String fileId) {
        try {
            log.info("Downloading extracted text with ID: {}", fileId);
            byte[] response = restClient.get()
                    .uri("/api/documents/{fileId}/download-text", fileId)
                    .retrieve()
                    .body(byte[].class);
            log.info("Download text successful, received {} bytes", response != null ? response.length : 0);
            return response;
        } catch (Exception e) {
            log.error("Error downloading extracted text {}: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Failed to download extracted text: " + e.getMessage(), e);
        }
    }

    /**
     * Delete document
     */
    public void deleteDocument(String fileId) {
        restClient.delete()
                .uri("/api/documents/{fileId}", fileId)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Search documents
     */
    public List<SearchResultDto> search(String query, Integer maxResults) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/search")
                        .queryParam("q", query)
                        .queryParam("maxResults", maxResults != null ? maxResults : 10)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<SearchResultDto>>() {});
    }

    /**
     * Search documents (POST)
     */
    public List<SearchResultDto> searchPost(String query, Integer maxResults) {
        Map<String, Object> request = Map.of(
                "question", query,
                "maxResults", maxResults != null ? maxResults : 10
        );

        return restClient.post()
                .uri("/api/search/query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<SearchResultDto>>() {});
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        return restClient.get()
                .uri("/api/documents/stats")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
