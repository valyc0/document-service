package com.example.orchestrator.camel;

import com.example.orchestrator.service.DocumentUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Bean Spring che processa i documenti intercettati da Apache Camel.
 * Converte il File in MultipartFile e lo invia al servizio di upload.
 */
@Component("documentProcessorBean")
@Slf4j
@RequiredArgsConstructor
public class DocumentProcessorBean {

    private final DocumentUploadService uploadService;

    /**
     * Processa un documento dal file system e lo invia all'orchestrator
     * 
     * @param exchange Camel Exchange contenente il file
     * @throws Exception se il processamento fallisce
     */
    public void processDocument(Exchange exchange) throws Exception {
        File file = exchange.getIn().getBody(File.class);
        String filename = exchange.getIn().getHeader("CamelFileName", String.class);
        
        log.info("🔄 Inizio processamento documento: {} ({} bytes)", 
                filename, file.length());

        try {
            // Crea un MultipartFile wrapper per il File
            MultipartFile multipartFile = new MultipartFile() {
                @Override
                public String getName() {
                    return "file";
                }

                @Override
                public String getOriginalFilename() {
                    return filename;
                }

                @Override
                public String getContentType() {
                    // Determina il content type dal nome file
                    if (filename.endsWith(".pdf")) return "application/pdf";
                    if (filename.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    if (filename.endsWith(".doc")) return "application/msword";
                    if (filename.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    if (filename.endsWith(".xls")) return "application/vnd.ms-excel";
                    if (filename.endsWith(".txt")) return "text/plain";
                    if (filename.endsWith(".html") || filename.endsWith(".htm")) return "text/html";
                    return "application/octet-stream";
                }

                @Override
                public boolean isEmpty() {
                    return file.length() == 0;
                }

                @Override
                public long getSize() {
                    return file.length();
                }

                @Override
                public byte[] getBytes() throws java.io.IOException {
                    try (InputStream is = new FileInputStream(file)) {
                        return is.readAllBytes();
                    }
                }

                @Override
                public InputStream getInputStream() throws java.io.IOException {
                    return new FileInputStream(file);
                }

                @Override
                public void transferTo(File dest) throws java.io.IOException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public org.springframework.core.io.Resource getResource() {
                    try {
                        return new org.springframework.core.io.InputStreamResource(getInputStream()) {
                            @Override
                            public String getFilename() {
                                return filename;
                            }
                            
                            @Override
                            public long contentLength() {
                                return file.length();
                            }
                        };
                    } catch (java.io.IOException e) {
                        throw new RuntimeException("Cannot create resource", e);
                    }
                }
            };

            // Upload il documento
            var result = uploadService.uploadDocument(multipartFile);
            String fileId = result.getId();
            
            log.info("✅ Documento caricato con successo: {} - FileID: {}", filename, fileId);
            exchange.getIn().setHeader("FileId", fileId);
            
        } catch (Exception e) {
            log.error("❌ Errore nel processamento di {}: {}", filename, e.getMessage(), e);
            throw e; // Rilancia per gestione errori della route
        }
    }
}
