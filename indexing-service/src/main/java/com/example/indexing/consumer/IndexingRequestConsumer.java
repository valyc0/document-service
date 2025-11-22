package com.example.indexing.consumer;

import com.example.indexing.dto.ExtractionResult;
import com.example.indexing.dto.IndexingCompletedMessage;
import com.example.indexing.dto.IndexingRequestMessage;
import com.example.indexing.service.ElasticsearchIndexingService;
import com.example.indexing.service.MinioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumer che ascolta le richieste di indicizzazione
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingRequestConsumer {
    
    private final MinioService minioService;
    private final ElasticsearchIndexingService indexingService;
    private final RabbitTemplate rabbitTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    
    @Value("${rabbitmq.routing-key.indexing-completed}")
    private String completedRoutingKey;
    
    @RabbitListener(queues = "${rabbitmq.queue.indexing-requests}")
    public void handleIndexingRequest(IndexingRequestMessage message) {
        String fileId = message.getFileId();
        log.info("📥 Received indexing request for fileId: {}", fileId);
        
        try {
            // 1. Download extracted text JSON from MinIO
            String extractedPath = "files/" + fileId + "/extracted-text.json";
            String extractedJson = minioService.downloadFileAsString(extractedPath);
            log.info("Downloaded extracted text from MinIO");
            
            // 2. Parse JSON
            ExtractionResult extractionResult = objectMapper.readValue(
                    extractedJson,
                    ExtractionResult.class
            );
            log.info("Parsed extraction result: {} chunks", extractionResult.getChunks().size());
            
            // 3. Index to Elasticsearch
            List<String> indexedIds = indexingService.indexDocument(extractionResult);
            log.info("Indexed {} chunks to Elasticsearch", indexedIds.size());
            
            // 4. Publish completion event
            IndexingCompletedMessage completedMsg = new IndexingCompletedMessage(
                    fileId,
                    "SUCCESS",
                    indexedIds.size()
            );
            rabbitTemplate.convertAndSend(exchangeName, completedRoutingKey, completedMsg);
            
            log.info("✅ Indexing completed for fileId: {} ({} chunks)", 
                    fileId, indexedIds.size());
            
        } catch (Exception e) {
            log.error("❌ Indexing failed for fileId: {}", fileId, e);
            
            // Publish failure event
            IndexingCompletedMessage failureMsg = new IndexingCompletedMessage(
                    fileId,
                    "FAILED",
                    0,
                    e.getMessage()
            );
            rabbitTemplate.convertAndSend(exchangeName, completedRoutingKey, failureMsg);
        }
    }
}
