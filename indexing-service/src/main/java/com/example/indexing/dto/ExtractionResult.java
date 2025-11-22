package com.example.indexing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO per rappresentare il risultato dell'estrazione (letto da MinIO)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {
    private String fileId;
    private String fullText;
    private List<String> chunks;
    private Map<String, String> metadata;
    private LocalDateTime extractedAt;
}
