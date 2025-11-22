package com.example.extraction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service per estrarre testo da documenti usando Apache Tika
 * RIUSO ESATTO DA google-like-search/DocumentService (metodi extractMetadata, applyMetadata)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TikaExtractionService {
    
    private final Tika tika = new Tika();
    private final ChunkingService chunkingService;
    
    /**
     * Estrai testo e metadati da file
     */
    public ExtractionResultData extractTextAndMetadata(InputStream inputStream) throws Exception {
        // Leggi i byte
        byte[] fileBytes = inputStream.readAllBytes();
        
        // Estrai metadati (CODICE DA google-like-search)
        Metadata metadata = extractMetadata(fileBytes);
        
        // Estrai testo completo usando Tika
        String fullText = tika.parseToString(new ByteArrayInputStream(fileBytes));
        log.info("Testo estratto: {} caratteri", fullText.length());
        
        // Crea chunk
        List<String> chunks = chunkingService.splitIntoChunks(fullText);
        log.info("Creati {} chunk", chunks.size());
        
        // Converti metadati Tika in Map
        Map<String, String> metadataMap = convertMetadataToMap(metadata);
        
        return new ExtractionResultData(fullText, chunks, metadataMap);
    }
    
    /**
     * Estrae metadati dal file usando Tika
     * (CODICE IDENTICO DA google-like-search/DocumentService.extractMetadata())
     */
    private Metadata extractMetadata(byte[] fileBytes) {
        try {
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // -1 = no limit
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            
            parser.parse(new ByteArrayInputStream(fileBytes), handler, metadata, context);
            return metadata;
        } catch (Exception e) {
            log.warn("Errore nell'estrazione metadati: {}", e.getMessage());
            return new Metadata(); // Ritorna metadata vuoti
        }
    }
    
    /**
     * Converte Tika Metadata in Map<String, String>
     * (LOGICA DA google-like-search/DocumentService.applyMetadata())
     */
    private Map<String, String> convertMetadataToMap(Metadata metadata) {
        Map<String, String> map = new HashMap<>();
        
        try {
            // Autore
            String author = metadata.get(TikaCoreProperties.CREATOR);
            if (author == null) author = metadata.get("Author");
            if (author != null) map.put("author", author);
            
            // Titolo
            String title = metadata.get(TikaCoreProperties.TITLE);
            if (title != null) map.put("title", title);
            
            // Content Type
            String contentType = metadata.get("Content-Type");
            if (contentType != null) map.put("contentType", contentType);
            
            // Data creazione
            String created = metadata.get(TikaCoreProperties.CREATED);
            if (created != null) map.put("creationDate", created);
            
            // Data modifica
            String modified = metadata.get(TikaCoreProperties.MODIFIED);
            if (modified != null) map.put("lastModified", modified);
            
            // Creator (software)
            String creator = metadata.get("producer"); // PDF producer
            if (creator == null) creator = metadata.get("Application-Name");
            if (creator != null) map.put("creator", creator);
            
            // Keywords
            String keywords = metadata.get("Keywords");
            if (keywords == null) keywords = metadata.get("meta:keyword");
            if (keywords != null) map.put("keywords", keywords);
            
            // Subject
            String subject = metadata.get(TikaCoreProperties.SUBJECT);
            if (subject != null) map.put("subject", subject);
            
            // Page count
            String pages = metadata.get("xmpTPg:NPages");
            if (pages == null) pages = metadata.get("Page-Count");
            if (pages != null) map.put("pageCount", pages);
            
            log.debug("Metadati estratti - Autore: {}, Titolo: {}, Tipo: {}, Pagine: {}",
                    map.get("author"), map.get("title"), map.get("contentType"), map.get("pageCount"));
                    
        } catch (Exception e) {
            log.warn("Errore nell'applicazione metadati: {}", e.getMessage());
        }
        
        return map;
    }
    
    /**
     * Inner class per risultato estrazione
     */
    public static class ExtractionResultData {
        public final String fullText;
        public final List<String> chunks;
        public final Map<String, String> metadata;
        
        public ExtractionResultData(String fullText, List<String> chunks, Map<String, String> metadata) {
            this.fullText = fullText;
            this.chunks = chunks;
            this.metadata = metadata;
        }
    }
}
