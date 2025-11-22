package com.example.indexing.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    /**
     * Download file as string (for JSON)
     */
    public String downloadFileAsString(String objectName) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            stream.close();
            
            log.info("✅ Downloaded file from MinIO: {}/{}", bucketName, objectName);
            return content;
            
        } catch (Exception e) {
            log.error("❌ Error downloading file from MinIO: {}", objectName, e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }
}
