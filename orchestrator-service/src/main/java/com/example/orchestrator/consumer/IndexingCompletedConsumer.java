package com.example.orchestrator.consumer;

import com.example.orchestrator.dto.IndexingCompletedMessage;
import com.example.orchestrator.entity.FileMetadata;
import com.example.orchestrator.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingCompletedConsumer {
    
    private final FileMetadataRepository repository;
    
    @RabbitListener(queues = "${rabbitmq.queue.indexing-completed}")
    public void handleIndexingCompleted(IndexingCompletedMessage message) {
        String fileId = message.getFileId();
        log.info("📥 Received indexing completed message for fileId: {} - Status: {}", 
                fileId, message.getStatus());
        
        try {
            FileMetadata metadata = repository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
            
            if ("SUCCESS".equals(message.getStatus())) {
                // Update status: indexing completed
                metadata.setIndexingStatus("COMPLETED");
                metadata.setIndexingCompletedAt(LocalDateTime.now());
                metadata.setUploadStatus("INDEXED");
                repository.save(metadata);
                
                log.info("🎉 Pipeline completed successfully for fileId: {}", fileId);
                
            } else {
                // Indexing failed
                metadata.setIndexingStatus("FAILED");
                metadata.setIndexingError(message.getErrorMessage());
                metadata.setUploadStatus("FAILED");
                metadata.setIndexingCompletedAt(LocalDateTime.now());
                repository.save(metadata);
                
                log.error("❌ Indexing failed for fileId: {} - Error: {}", 
                        fileId, message.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ Error handling indexing completed message for fileId: {}", fileId, e);
        }
    }
}
