package com.example.extraction.dto;

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
}
