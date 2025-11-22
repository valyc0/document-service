package com.example.orchestrator.consumer;

import com.example.orchestrator.dto.ExtractionCompletedMessage;
import com.example.orchestrator.entity.FileMetadata;
import com.example.orchestrator.repository.FileMetadataRepository;
import com.example.orchestrator.service.MessagePublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractionCompletedConsumer {
    
    private final FileMetadataRepository repository;
    private final MessagePublisherService messagePublisher;
    
    @RabbitListener(queues = "${rabbitmq.queue.extraction-completed}")
    public void handleExtractionCompleted(ExtractionCompletedMessage message) {
        String fileId = message.getFileId();
        log.info("📥 Received extraction completed message for fileId: {} - Status: {}", 
                fileId, message.getStatus());
        
        try {
            FileMetadata metadata = repository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
            
            if ("SUCCESS".equals(message.getStatus())) {
                // Update status: extraction completed
                metadata.setExtractionStatus("COMPLETED");
                metadata.setExtractionCompletedAt(LocalDateTime.now());
                metadata.setMinioPathExtracted("files/" + fileId + "/extracted-text.json");
                metadata.setUploadStatus("EXTRACTED");
                
                // Trigger indexing
                metadata.setIndexingStatus("PENDING");
                repository.save(metadata);
                
                log.info("✅ Extraction completed, triggering indexing for fileId: {}", fileId);
                messagePublisher.publishIndexingRequest(fileId);
                
            } else {
                // Extraction failed
                metadata.setExtractionStatus("FAILED");
                metadata.setExtractionError(message.getErrorMessage());
                metadata.setUploadStatus("FAILED");
                metadata.setExtractionCompletedAt(LocalDateTime.now());
                repository.save(metadata);
                
                log.error("❌ Extraction failed for fileId: {} - Error: {}", 
                        fileId, message.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ Error handling extraction completed message for fileId: {}", fileId, e);
        }
    }
}
