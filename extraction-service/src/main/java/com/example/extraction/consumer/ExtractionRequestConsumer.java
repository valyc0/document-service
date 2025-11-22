package com.example.extraction.consumer;

import com.example.extraction.dto.ExtractionCompletedMessage;
import com.example.extraction.dto.ExtractionRequestMessage;
import com.example.extraction.dto.ExtractionResult;
import com.example.extraction.service.MinioService;
import com.example.extraction.service.TikaExtractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * Consumer che ascolta le richieste di estrazione e processa i file
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractionRequestConsumer {
    
    private final MinioService minioService;
    private final TikaExtractionService tikaService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    
    @Value("${rabbitmq.routing-key.extraction-completed}")
    private String completedRoutingKey;
    
    @RabbitListener(queues = "${rabbitmq.queue.extraction-requests}")
    public void handleExtractionRequest(ExtractionRequestMessage message) {
        String fileId = message.getFileId();
        String originalFilename = message.getOriginalFilename();
        log.info("📥 Received extraction request for fileId: {} (filename: {})", fileId, originalFilename);
        
        try {
            // 1. Trova il file su MinIO (cerca in files/{fileId}/original.*)
            String minioPath = findOriginalFile(fileId);
            log.info("Found file on MinIO: {}", minioPath);
            
            // 2. Download file da MinIO
            InputStream fileStream = minioService.downloadFile(minioPath);
            log.info("Downloaded file from MinIO");
            
            // 3. Estrai testo e metadati con Tika
            TikaExtractionService.ExtractionResultData resultData = 
                    tikaService.extractTextAndMetadata(fileStream);
            
            // 4. Aggiungi il filename originale ai metadati
            if (originalFilename != null) {
                resultData.metadata.put("filename", originalFilename);
                log.info("Added original filename to metadata: {}", originalFilename);
            }
            
            // 5. Crea ExtractionResult
            ExtractionResult result = new ExtractionResult();
            result.setFileId(fileId);
            result.setFullText(resultData.fullText);
            result.setChunks(resultData.chunks);
            result.setMetadata(resultData.metadata);
            result.setExtractedAt(LocalDateTime.now());
            
            // 5. Converti in JSON
            String resultJson = objectMapper.writeValueAsString(result);
            
            // 6. Upload JSON su MinIO
            String extractedPath = "files/" + fileId + "/extracted-text.json";
            minioService.uploadJson(extractedPath, resultJson);
            
            // 7. Pubblica evento di completamento
            ExtractionCompletedMessage completedMsg = new ExtractionCompletedMessage(
                    fileId,
                    "SUCCESS",
                    resultData.chunks.size()
            );
            rabbitTemplate.convertAndSend(exchangeName, completedRoutingKey, completedMsg);
            
            log.info("✅ Extraction completed for fileId: {} ({} chunks)", 
                    fileId, resultData.chunks.size());
            
        } catch (Exception e) {
            log.error("❌ Extraction failed for fileId: {}", fileId, e);
            
            // Pubblica evento di fallimento
            ExtractionCompletedMessage failureMsg = new ExtractionCompletedMessage(
                    fileId,
                    "FAILED",
                    0,
                    e.getMessage()
            );
            rabbitTemplate.convertAndSend(exchangeName, completedRoutingKey, failureMsg);
        }
    }
    
    /**
     * Trova il file originale su MinIO (potrebbe avere estensioni diverse)
     */
    private String findOriginalFile(String fileId) {
        // Possibili estensioni
        String[] extensions = {"pdf", "doc", "docx", "xls", "xlsx", "txt", "html", "rtf", 
                               "odt", "ods", "csv", "xml", "json", "md"};
        
        String basePath = "files/" + fileId + "/original.";
        
        // Prova diverse estensioni (in produzione si potrebbe salvare l'estensione nel messaggio)
        for (String ext : extensions) {
            String path = basePath + ext;
            try {
                minioService.downloadFile(path).close();
                return path; // Trovato!
            } catch (Exception e) {
                // Non trovato con questa estensione, prova la prossima
            }
        }
        
        // Se non trova con estensioni specifiche, prova senza estensione
        // (MinIO potrebbe avere il file salvato diversamente)
        throw new RuntimeException("Original file not found on MinIO for fileId: " + fileId);
    }
}
