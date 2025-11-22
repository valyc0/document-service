package com.example.extraction.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    /**
     * Download file from MinIO
     */
    public InputStream downloadFile(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("❌ Error downloading file from MinIO: {}", objectName, e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }
    
    /**
     * Upload JSON result to MinIO
     */
    public void uploadJson(String objectName, String jsonContent) {
        try {
            byte[] bytes = jsonContent.getBytes();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                            .contentType("application/json")
                            .build()
            );
            log.info("✅ Uploaded JSON to MinIO: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("❌ Error uploading JSON to MinIO: {}", objectName, e);
            throw new RuntimeException("Failed to upload JSON to MinIO", e);
        }
    }
}
