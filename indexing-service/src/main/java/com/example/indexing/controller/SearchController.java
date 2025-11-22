package com.example.indexing.controller;

import com.example.indexing.dto.SearchResultDto;
import com.example.indexing.model.SearchDocument;
import com.example.indexing.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller per ricerca documenti
 * RIUSA ESATTAMENTE API DA google-like-search/SearchController
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    
    private final SearchService searchService;
    
    /**
     * Search documents (POST method - recommended)
     */
    @PostMapping("/query")
    public ResponseEntity<List<SearchResultDto>> searchPost(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        Integer maxResults = request.containsKey("maxResults") ? 
                (Integer) request.get("maxResults") : 10;
        
        log.info("🔍 Search request: q={}, maxResults={}", question, maxResults);
        
        List<SearchResultDto> results = searchService.search(question, maxResults);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Search documents (GET method)
     */
    @GetMapping
    public ResponseEntity<List<SearchResultDto>> searchGet(
            @RequestParam("q") String query,
            @RequestParam(value = "maxResults", defaultValue = "10") Integer maxResults) {
        
        log.info("🔍 Search request: q={}, maxResults={}", query, maxResults);
        
        List<SearchResultDto> results = searchService.search(query, maxResults);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Raw search (returns all chunks - for debugging)
     */
    @GetMapping("/raw")
    public ResponseEntity<List<SearchHit<SearchDocument>>> rawSearch(@RequestParam("q") String query) {
        log.info("🔍 Raw search request: q={}", query);
        
        List<SearchHit<SearchDocument>> results = searchService.searchRaw(query);
        return ResponseEntity.ok(results);
    }
    
    /**
     * List indexed filenames
     */
    @GetMapping("/files")
    public ResponseEntity<List<String>> listFiles() {
        List<String> filenames = searchService.getIndexedFilenames();
        return ResponseEntity.ok(filenames);
    }
    
    /**
     * Delete document from Elasticsearch by documentId
     */
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String documentId) {
        log.info("🗑️ Delete request for documentId: {}", documentId);
        
        long deletedCount = searchService.deleteByDocumentId(documentId);
        
        Map<String, Object> response = Map.of(
            "message", "Document deleted from Elasticsearch",
            "documentId", documentId,
            "chunksDeleted", deletedCount
        );
        
        return ResponseEntity.ok(response);
    }
}
