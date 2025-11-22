package com.example.orchestrator.service;

import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    /**
     * Inizializza il bucket se non esiste
     */
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("✅ Created MinIO bucket: {}", bucketName);
            } else {
                log.info("✅ MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("❌ Error ensuring bucket exists", e);
            throw new RuntimeException("Failed to create MinIO bucket", e);
        }
    }
    
    /**
     * Upload file to MinIO
     */
    public String uploadFile(String objectName, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            
            log.info("✅ Uploaded file to MinIO: {}/{}", bucketName, objectName);
            return objectName;
            
        } catch (Exception e) {
            log.error("❌ Error uploading file to MinIO: {}", objectName, e);
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }
    
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
     * Delete file from MinIO
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("✅ Deleted file from MinIO: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("❌ Error deleting file from MinIO: {}", objectName, e);
            throw new RuntimeException("Failed to delete file from MinIO", e);
        }
    }
    
    /**
     * Check if file exists
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get presigned URL for download (valid for 7 days)
     */
    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(io.minio.http.Method.GET)
                            .expiry(7 * 24 * 60 * 60) // 7 days
                            .build()
            );
        } catch (Exception e) {
            log.error("❌ Error generating presigned URL: {}", objectName, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
}
