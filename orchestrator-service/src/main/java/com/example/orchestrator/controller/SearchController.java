package com.example.orchestrator.controller;

import com.example.orchestrator.client.IndexingServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final IndexingServiceClient indexingServiceClient;

    public SearchController(IndexingServiceClient indexingServiceClient) {
        this.indexingServiceClient = indexingServiceClient;
    }

    /**
     * GET /api/search?q=query&maxResults=10
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") Integer maxResults) {
        List<Map<String, Object>> results = indexingServiceClient.search(q, maxResults);
        return ResponseEntity.ok(results);
    }

    /**
     * POST /api/search/query
     * Body: {"question": "query", "maxResults": 10}
     * Recommended method for complex queries
     */
    @PostMapping("/query")
    public ResponseEntity<List<Map<String, Object>>> searchPost(@RequestBody Map<String, Object> request) {
        List<Map<String, Object>> results = indexingServiceClient.searchPost(request);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/search/raw?q=query
     * Returns raw Elasticsearch results (for debugging)
     */
    @GetMapping("/raw")
    public ResponseEntity<List<Map<String, Object>>> searchRaw(@RequestParam String q) {
        List<Map<String, Object>> results = indexingServiceClient.searchRaw(q);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/search/files
     * Returns list of all indexed filenames
     */
    @GetMapping("/files")
    public ResponseEntity<List<String>> getIndexedFiles() {
        List<String> files = indexingServiceClient.getIndexedFiles();
        return ResponseEntity.ok(files);
    }
}
