package com.example.indexing.service;

import com.example.indexing.dto.ExtractionResult;
import com.example.indexing.model.SearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service per indicizzare documenti su Elasticsearch
 * RIUSA LOGICA DA google-like-search/DocumentService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchIndexingService {
    
    private final ElasticsearchOperations elasticsearchOperations;
    
    /**
     * Indicizza tutti i chunk di un documento su Elasticsearch
     */
    public List<String> indexDocument(ExtractionResult extractionResult) {
        List<String> indexedIds = new ArrayList<>();
        
        try {
            String fileId = extractionResult.getFileId();
            List<String> chunks = extractionResult.getChunks();
            Map<String, String> metadata = extractionResult.getMetadata();
            
            log.info("Indexing {} chunks for fileId: {}", chunks.size(), fileId);
            
            // Indicizza ogni chunk come documento separato
            for (int i = 0; i < chunks.size(); i++) {
                SearchDocument doc = new SearchDocument();
                doc.setId(UUID.randomUUID().toString());
                doc.setDocumentId(fileId);
                doc.setContent(chunks.get(i));
                doc.setChunkIndex(i);
                doc.setTotalChunks(chunks.size());
                doc.setUploadedAt(LocalDateTime.now());
                doc.setStatus("COMPLETED");
                
                // Applica metadati da Tika
                applyMetadataToDocument(doc, metadata);
                
                // Salva su Elasticsearch
                elasticsearchOperations.save(doc);
                indexedIds.add(doc.getId());
                
                log.debug("Indexed chunk {}/{} for fileId: {}", i + 1, chunks.size(), fileId);
            }
            
            log.info("✅ Successfully indexed {} chunks for fileId: {}", chunks.size(), fileId);
            
            return indexedIds;
            
        } catch (Exception e) {
            log.error("❌ Error indexing document", e);
            throw new RuntimeException("Failed to index document", e);
        }
    }
    
    /**
     * Applica metadati dal Map al SearchDocument
     * (LOGICA DA google-like-search/DocumentService.applyMetadata())
     */
    private void applyMetadataToDocument(SearchDocument doc, Map<String, String> metadata) {
        if (metadata == null) return;
        
        try {
            // Filename
            String filename = metadata.get("filename");
            if (filename != null) doc.setFilename(filename);
            
            // Autore
            String author = metadata.get("author");
            if (author != null) doc.setAuthor(author);
            
            // Titolo
            String title = metadata.get("title");
            if (title != null) doc.setTitle(title);
            
            // Content Type
            String contentType = metadata.get("contentType");
            if (contentType != null) doc.setContentType(contentType);
            
            // Data creazione
            String created = metadata.get("creationDate");
            if (created != null) {
                try {
                    doc.setCreationDate(parseDate(created));
                } catch (Exception e) {
                    log.debug("Cannot parse creation date: {}", created);
                }
            }
            
            // Data modifica
            String modified = metadata.get("lastModified");
            if (modified != null) {
                try {
                    doc.setLastModified(parseDate(modified));
                } catch (Exception e) {
                    log.debug("Cannot parse modified date: {}", modified);
                }
            }
            
            // Creator
            String creator = metadata.get("creator");
            if (creator != null) doc.setCreator(creator);
            
            // Keywords
            String keywords = metadata.get("keywords");
            if (keywords != null) doc.setKeywords(keywords);
            
            // Subject
            String subject = metadata.get("subject");
            if (subject != null) doc.setSubject(subject);
            
            // Page count
            String pages = metadata.get("pageCount");
            if (pages != null) {
                try {
                    doc.setPageCount(Integer.parseInt(pages));
                } catch (NumberFormatException e) {
                    log.debug("Cannot parse page count: {}", pages);
                }
            }
            
            log.debug("Applied metadata - Filename: {}, Author: {}, Title: {}, Type: {}, Pages: {}",
                    doc.getFilename(), doc.getAuthor(), doc.getTitle(), doc.getContentType(), doc.getPageCount());
                    
        } catch (Exception e) {
            log.warn("Error applying metadata: {}", e.getMessage());
        }
    }
    
    /**
     * Parse date string to LocalDateTime
     */
    private LocalDateTime parseDate(String dateStr) {
        try {
            // Try ISO format first
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            // Try other formats...
            return null;
        }
    }
}
