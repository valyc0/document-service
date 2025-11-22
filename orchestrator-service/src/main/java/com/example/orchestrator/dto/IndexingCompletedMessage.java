package com.example.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexingCompletedMessage implements Serializable {
    private String fileId;
    private String status; // SUCCESS, FAILED
    private Integer indexedChunks;
    private String errorMessage;
    private LocalDateTime timestamp;
    
    public IndexingCompletedMessage(String fileId, String status, Integer indexedChunks) {
        this.fileId = fileId;
        this.status = status;
        this.indexedChunks = indexedChunks;
        this.timestamp = LocalDateTime.now();
    }
    
    public IndexingCompletedMessage(String fileId, String status, Integer indexedChunks, String errorMessage) {
        this(fileId, status, indexedChunks);
        this.errorMessage = errorMessage;
    }
}
