package com.example.orchestrator.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class IndexingServiceClient {

    private final RestTemplate restTemplate;
    private final String indexingServiceUrl;

    public IndexingServiceClient(
            RestTemplate restTemplate,
            @Value("${indexing.service.url:http://indexing-service:8082}") String indexingServiceUrl) {
        this.restTemplate = restTemplate;
        this.indexingServiceUrl = indexingServiceUrl;
    }

    public List<Map<String, Object>> search(String query, Integer maxResults) {
        String url = indexingServiceUrl + "/api/search?q={query}&maxResults={maxResults}";
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                query,
                maxResults
        );
        return response.getBody();
    }

    public List<Map<String, Object>> searchPost(Map<String, Object> request) {
        String url = indexingServiceUrl + "/api/search/query";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        return response.getBody();
    }

    public List<Map<String, Object>> searchRaw(String query) {
        String url = indexingServiceUrl + "/api/search/raw?q={query}";
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                query
        );
        return response.getBody();
    }

    public List<String> getIndexedFiles() {
        String url = indexingServiceUrl + "/api/search/files";
        ResponseEntity<List<String>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {}
        );
        return response.getBody();
    }

    public void deleteDocument(String documentId) {
        String url = indexingServiceUrl + "/api/search/documents/" + documentId;
        restTemplate.delete(url);
    }
}
