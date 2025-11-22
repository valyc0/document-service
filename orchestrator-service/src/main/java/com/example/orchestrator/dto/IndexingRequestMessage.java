package com.example.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexingRequestMessage implements Serializable {
    private String fileId;
    private LocalDateTime timestamp;
    
    public IndexingRequestMessage(String fileId) {
        this.fileId = fileId;
        this.timestamp = LocalDateTime.now();
    }
}
