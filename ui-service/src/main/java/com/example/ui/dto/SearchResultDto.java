package com.example.ui.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchResultDto {
    private String documentId;
    private String filename;
    private Integer chunkIndex;
    private String content;
    private List<String> highlights;
    private Double score;
    
    // Metadata
    private String author;
    private String title;
    private String contentType;
    private String creationDate;
    private String lastModified;
    private String creator;
    private String keywords;
    private String subject;
    private Integer pageCount;
}
