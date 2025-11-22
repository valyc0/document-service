package com.example.extraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {
    private String fileId;
    private String fullText;
    private List<String> chunks;
    private Map<String, String> metadata; // Tika metadata
    private LocalDateTime extractedAt;
}
