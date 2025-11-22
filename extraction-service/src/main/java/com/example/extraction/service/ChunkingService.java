package com.example.extraction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service per dividere il testo in chunk
 * RIUSO ESATTO DA google-like-search/DocumentService.splitIntoChunks()
 */
@Service
public class ChunkingService {
    
    @Value("${extraction.chunk-size:5000}")
    private int chunkSize;
    
    /**
     * Dividi il testo in chunk di dimensione specificata
     * (CODICE IDENTICO DA google-like-search)
     */
    public List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        int length = text.length();
        for (int i = 0; i < length; i += chunkSize) {
            int end = Math.min(i + chunkSize, length);
            
            // Cerca di spezzare su un confine di parola per evitare di tagliare a metà
            if (end < length) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > i) {
                    end = lastSpace;
                }
            }
            
            chunks.add(text.substring(i, end).trim());
            i = end - chunkSize; // Aggiusta l'indice dopo il trim
        }
        
        return chunks;
    }
}
