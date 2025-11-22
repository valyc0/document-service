package com.example.extraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionCompletedMessage implements Serializable {
    private String fileId;
    private String status; // SUCCESS, FAILED
    private Integer chunksCount;
    private String errorMessage;
    private LocalDateTime timestamp;
    
    public ExtractionCompletedMessage(String fileId, String status, Integer chunksCount) {
        this.fileId = fileId;
        this.status = status;
        this.chunksCount = chunksCount;
        this.timestamp = LocalDateTime.now();
    }
    
    public ExtractionCompletedMessage(String fileId, String status, Integer chunksCount, String errorMessage) {
        this(fileId, status, chunksCount);
        this.errorMessage = errorMessage;
    }
}
