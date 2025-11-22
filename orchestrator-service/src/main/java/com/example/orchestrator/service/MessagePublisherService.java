package com.example.orchestrator.service;

import com.example.orchestrator.dto.ExtractionRequestMessage;
import com.example.orchestrator.dto.IndexingRequestMessage;
import com.example.orchestrator.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePublisherService {
    
    private final RabbitTemplate rabbitTemplate;
    private final FileMetadataRepository repository;
    
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    
    @Value("${rabbitmq.routing-key.extraction-request}")
    private String extractionRequestKey;
    
    @Value("${rabbitmq.routing-key.indexing-request}")
    private String indexingRequestKey;
    
    /**
     * Pubblica richiesta di estrazione
     */
    public void publishExtractionRequest(String fileId) {
        repository.findById(fileId).ifPresent(metadata -> {
            metadata.setExtractionStartedAt(LocalDateTime.now());
            repository.save(metadata);
            
            // Passa anche il filename al servizio di estrazione
            String filename = metadata.getOriginalFilename();
            ExtractionRequestMessage message = new ExtractionRequestMessage(fileId, filename, LocalDateTime.now());
            rabbitTemplate.convertAndSend(exchangeName, extractionRequestKey, message);
            log.info("📤 Published extraction request for fileId: {} (filename: {})", fileId, filename);
        });
    }
    
    /**
     * Pubblica richiesta di indicizzazione
     */
    public void publishIndexingRequest(String fileId) {
        repository.findById(fileId).ifPresent(metadata -> {
            metadata.setIndexingStartedAt(LocalDateTime.now());
            repository.save(metadata);
        });
        
        IndexingRequestMessage message = new IndexingRequestMessage(fileId);
        rabbitTemplate.convertAndSend(exchangeName, indexingRequestKey, message);
        log.info("📤 Published indexing request for fileId: {}", fileId);
    }
}
