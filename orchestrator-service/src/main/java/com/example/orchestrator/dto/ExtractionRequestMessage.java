package com.example.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionRequestMessage implements Serializable {
    private String fileId;
    private String originalFilename;
    private LocalDateTime timestamp;
    
    public ExtractionRequestMessage(String fileId, String originalFilename) {
        this.fileId = fileId;
        this.originalFilename = originalFilename;
        this.timestamp = LocalDateTime.now();
    }
}
