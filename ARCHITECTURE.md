# Document Processing System - Architecture

## 📋 Overview

Sistema distribuito a 3 microservizi per processamento, indicizzazione e ricerca di documenti multi-formato, basato su architettura **Event-Driven** con **Storage Condiviso (MinIO)**.

---

## 🏗️ Architettura

```
                           ┌─────────────────────────────────┐
                           │   CLIENT (REST API)              │
                           └───────────────┬─────────────────┘
                                           │
                                           ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                         ORCHESTRATOR SERVICE                              │
│  - Port: 8080                                                             │
│  - H2 Database (file metadata & status tracking)                         │
│  - MinIO Client (upload/download files)                                  │
│  - RabbitMQ Publisher (send file IDs to queues)                          │
│  - RabbitMQ Consumer (receive completion events)                         │
│                                                                           │
│  Endpoints:                                                               │
│  - POST /api/documents/upload          → Upload file                     │
│  - GET  /api/documents/{id}/status     → Check processing status         │
│  - GET  /api/documents/{id}            → Get file metadata               │
│  - GET  /api/documents/{id}/download   → Download original file          │
│  - GET  /api/documents                 → List all files                  │
│  - GET  /api/search?q=query            → Search documents                │
└─────────────┬────────────────────────────────┬───────────────────────────┘
              │                                │
              │ Publish: extraction.request    │ Publish: indexing.request
              │ (fileId)                       │ (fileId)
              ▼                                ▼
    ┌──────────────────┐            ┌──────────────────┐
    │  RabbitMQ Queue  │            │  RabbitMQ Queue  │
    │ extraction-      │            │ indexing-        │
    │ requests         │            │ requests         │
    └────────┬─────────┘            └────────┬─────────┘
             │                               │
             ▼                               ▼
┌─────────────────────────┐      ┌─────────────────────────┐
│  EXTRACTION SERVICE     │      │  INDEXING SERVICE       │
│  - Port: 8081           │      │  - Port: 8082           │
│  - RabbitMQ Consumer    │      │  - RabbitMQ Consumer    │
│  - MinIO Client         │      │  - MinIO Client         │
│  - Apache Tika          │      │  - Elasticsearch Client │
│  - Text Chunking        │      │  - Search API           │
│  - RabbitMQ Publisher   │      │  - RabbitMQ Publisher   │
│                         │      │                         │
│  Workflow:              │      │  Workflow:              │
│  1. Consume fileId      │      │  1. Consume fileId      │
│  2. Download from MinIO │      │  2. Download JSON from  │
│  3. Extract text (Tika) │      │     MinIO               │
│  4. Create chunks       │      │  3. Index chunks to     │
│  5. Upload JSON to MinIO│      │     Elasticsearch       │
│  6. Publish completion  │      │  4. Publish completion  │
└──────────┬──────────────┘      └──────────┬──────────────┘
           │                                │
           │ Publish: extraction.completed  │ Publish: indexing.completed
           │ (fileId, status)               │ (fileId, status)
           ▼                                ▼
    ┌──────────────────┐            ┌──────────────────┐
    │  RabbitMQ Queue  │            │  RabbitMQ Queue  │
    │ extraction-      │            │ indexing-        │
    │ completed        │            │ completed        │
    └────────┬─────────┘            └────────┬─────────┘
             │                               │
             └───────────────┬───────────────┘
                             ▼
                ┌─────────────────────────┐
                │  ORCHESTRATOR SERVICE   │
                │  (Update DB status)     │
                └─────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                         SHARED STORAGE (MinIO)                            │
│  Bucket: document-processing                                              │
│                                                                           │
│  /files/{fileId}/                                                         │
│    ├── original.{ext}           ← File originale caricato                │
│    ├── extracted-text.json      ← Risultato extraction service           │
│    │   {                                                                  │
│    │     "fileId": "uuid",                                               │
│    │     "fullText": "...",                                              │
│    │     "chunks": ["chunk1", "chunk2", ...],                           │
│    │     "metadata": {author, title, ...},                              │
│    │     "extractedAt": "2025-11-22T10:00:00Z"                          │
│    │   }                                                                  │
│    └── metadata.json            ← Metadati Tika completi                │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                         ELASTICSEARCH                                     │
│  Index: documents                                                         │
│                                                                           │
│  Each chunk as separate document:                                         │
│  {                                                                        │
│    "id": "chunk-uuid",                                                   │
│    "documentId": "file-uuid",                                            │
│    "content": "chunk text...",                                           │
│    "chunkIndex": 0,                                                      │
│    "totalChunks": 10,                                                    │
│    "filename": "document.pdf",                                           │
│    "metadata": {...}                                                     │
│  }                                                                        │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Database Schema (H2 - Orchestrator)

```sql
CREATE TABLE file_metadata (
    -- Primary Key
    id VARCHAR(36) PRIMARY KEY,                    -- UUID del file
    
    -- File Info
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    checksum VARCHAR(64),                          -- SHA-256 per deduplication
    
    -- MinIO Storage Paths
    minio_bucket VARCHAR(100) NOT NULL,
    minio_path_original VARCHAR(500) NOT NULL,     -- files/{fileId}/original.ext
    minio_path_extracted VARCHAR(500),             -- files/{fileId}/extracted-text.json
    
    -- Status Tracking (State Machine)
    upload_status VARCHAR(50) NOT NULL,            -- UPLOADED | EXTRACTING | EXTRACTED | INDEXING | INDEXED | FAILED
    extraction_status VARCHAR(50),                 -- PENDING | IN_PROGRESS | COMPLETED | FAILED
    indexing_status VARCHAR(50),                   -- PENDING | IN_PROGRESS | COMPLETED | FAILED
    
    -- Timestamps (Audit Trail)
    uploaded_at TIMESTAMP NOT NULL,
    extraction_started_at TIMESTAMP,
    extraction_completed_at TIMESTAMP,
    indexing_started_at TIMESTAMP,
    indexing_completed_at TIMESTAMP,
    
    -- Metadata from Tika (JSON)
    extracted_metadata TEXT,                       -- JSON: {author, title, pageCount, ...}
    
    -- Error Handling
    extraction_error TEXT,
    indexing_error TEXT,
    retry_count INT DEFAULT 0,
    
    -- System Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Performance
CREATE INDEX idx_upload_status ON file_metadata(upload_status);
CREATE INDEX idx_extraction_status ON file_metadata(extraction_status);
CREATE INDEX idx_indexing_status ON file_metadata(indexing_status);
CREATE INDEX idx_uploaded_at ON file_metadata(uploaded_at);
CREATE INDEX idx_checksum ON file_metadata(checksum);
```

---

## 🔄 Complete Workflow

### **Phase 1: File Upload (Client → Orchestrator)**

```
1. Client → POST /api/documents/upload
   Content-Type: multipart/form-data
   Body: file=@document.pdf

2. Orchestrator:
   a. Generate fileId = UUID.randomUUID()
   b. Calculate checksum (SHA-256)
   c. Upload to MinIO: files/{fileId}/original.pdf
   d. Save to H2:
      - id = fileId
      - original_filename = "document.pdf"
      - minio_path_original = "files/{fileId}/original.pdf"
      - upload_status = "UPLOADED"
      - extraction_status = "PENDING"
   e. Publish to RabbitMQ:
      Exchange: document-processing
      Routing Key: extraction.request
      Message: {"fileId": "...", "timestamp": "..."}

3. Response to Client:
   {
     "fileId": "uuid",
     "status": "UPLOADED",
     "message": "File queued for extraction"
   }
```

### **Phase 2: Text Extraction (Extraction Service)**

```
1. Extraction Service consumes from queue: extraction-requests

2. Receive message: {"fileId": "uuid"}

3. Download file from MinIO:
   Path: files/{fileId}/original.*

4. Extract text with Apache Tika:
   - Detect format automatically
   - Parse to String
   - Extract metadata (author, title, dates, etc.)

5. Create chunks:
   - Split text into chunks of ~5000 characters
   - Break on word boundaries

6. Create result JSON:
   {
     "fileId": "uuid",
     "fullText": "complete extracted text...",
     "chunks": ["chunk1", "chunk2", ...],
     "metadata": {
       "author": "John Doe",
       "title": "Document Title",
       "contentType": "application/pdf",
       "pageCount": 10,
       ...
     },
     "extractedAt": "2025-11-22T10:00:00Z"
   }

7. Upload to MinIO:
   Path: files/{fileId}/extracted-text.json

8. Publish completion event:
   Exchange: document-processing
   Routing Key: extraction.completed
   Message: {
     "fileId": "uuid",
     "status": "SUCCESS",
     "chunksCount": 10,
     "timestamp": "..."
   }

9. On Error:
   Publish: {"fileId": "uuid", "status": "FAILED", "error": "..."}
```

### **Phase 3: Update Status (Orchestrator)**

```
1. Orchestrator consumes from: extraction-completed

2. Receive message: {"fileId": "uuid", "status": "SUCCESS"}

3. Update H2:
   UPDATE file_metadata SET
     extraction_status = 'COMPLETED',
     extraction_completed_at = NOW(),
     minio_path_extracted = 'files/{fileId}/extracted-text.json',
     upload_status = 'EXTRACTED',
     indexing_status = 'PENDING'
   WHERE id = fileId

4. Publish to indexing queue:
   Exchange: document-processing
   Routing Key: indexing.request
   Message: {"fileId": "uuid", "timestamp": "..."}
```

### **Phase 4: Indexing (Indexing Service)**

```
1. Indexing Service consumes from: indexing-requests

2. Receive message: {"fileId": "uuid"}

3. Download extracted text from MinIO:
   Path: files/{fileId}/extracted-text.json

4. Parse JSON:
   - Extract chunks array
   - Extract metadata

5. Index to Elasticsearch:
   For each chunk (i = 0 to N):
     {
       "id": UUID.randomUUID(),
       "documentId": fileId,
       "content": chunks[i],
       "chunkIndex": i,
       "totalChunks": N,
       "filename": "document.pdf",
       "author": metadata.author,
       "title": metadata.title,
       "contentType": metadata.contentType,
       ...
     }

6. Publish completion event:
   Exchange: document-processing
   Routing Key: indexing.completed
   Message: {
     "fileId": "uuid",
     "status": "SUCCESS",
     "indexedChunks": 10,
     "timestamp": "..."
   }
```

### **Phase 5: Final Update (Orchestrator)**

```
1. Orchestrator consumes from: indexing-completed

2. Receive message: {"fileId": "uuid", "status": "SUCCESS"}

3. Update H2:
   UPDATE file_metadata SET
     indexing_status = 'COMPLETED',
     indexing_completed_at = NOW(),
     upload_status = 'INDEXED'
   WHERE id = fileId

4. Pipeline completed! 🎉
```

### **Phase 6: Search (Client → Orchestrator → Indexing Service)**

```
1. Client → GET /api/search?q=query

2. Orchestrator proxies to Indexing Service:
   GET http://indexing-service:8082/api/search?q=query

3. Indexing Service queries Elasticsearch:
   - Match on content field
   - Group by documentId
   - Return highlights

4. Response:
   [
     {
       "documentId": "uuid",
       "filename": "document.pdf",
       "score": 1.5,
       "highlights": ["...query..."],
       "metadata": {...}
     }
   ]
```

---

## 📡 RabbitMQ Configuration

### **Exchanges**

```yaml
document-processing:
  type: topic
  durable: true
  auto-delete: false
```

### **Queues**

```yaml
extraction-requests:
  durable: true
  arguments:
    x-dead-letter-exchange: document-processing-dlx
    x-dead-letter-routing-key: extraction.dlq
    x-message-ttl: 300000  # 5 minutes

extraction-completed:
  durable: true

indexing-requests:
  durable: true
  arguments:
    x-dead-letter-exchange: document-processing-dlx
    x-dead-letter-routing-key: indexing.dlq
    x-message-ttl: 300000

indexing-completed:
  durable: true

# Dead Letter Queues
extraction-requests-dlq:
  durable: true

indexing-requests-dlq:
  durable: true
```

### **Bindings**

```yaml
Exchange: document-processing → Queue: extraction-requests
  Routing Key: extraction.request

Exchange: document-processing → Queue: extraction-completed
  Routing Key: extraction.completed

Exchange: document-processing → Queue: indexing-requests
  Routing Key: indexing.request

Exchange: document-processing → Queue: indexing-completed
  Routing Key: indexing.completed
```

---

## 🐳 Infrastructure Stack

```yaml
Services:
  - MinIO (Storage)       → Port 9000 (API), 9001 (Console)
  - RabbitMQ (Queue)      → Port 5672 (AMQP), 15672 (Management)
  - Elasticsearch (Index) → Port 9200
  - H2 Database (File)    → Embedded in Orchestrator

Applications:
  - Orchestrator Service  → Port 8080
  - Extraction Service    → Port 8081 (scalable: 2+ instances)
  - Indexing Service      → Port 8082
```

---

## 📦 Technology Stack

### **Orchestrator Service**

```xml
Dependencies:
- spring-boot-starter-web          (REST API)
- spring-boot-starter-data-jpa     (H2 database)
- h2                                (Embedded DB)
- spring-boot-starter-amqp         (RabbitMQ)
- minio (io.minio:minio:8.5.7)     (Object storage client)
- lombok                            (Boilerplate reduction)
```

### **Extraction Service**

```xml
Dependencies:
- spring-boot-starter-web          (Health checks)
- spring-boot-starter-amqp         (RabbitMQ consumer/publisher)
- minio (io.minio:minio:8.5.7)     (Download/upload files)
- tika-core (2.9.1)                (Text extraction)
- tika-parsers-standard-package    (PDF, DOC, XLS parsers)
- lombok
```

### **Indexing Service**

```xml
Dependencies:
- spring-boot-starter-web                    (REST API for search)
- spring-boot-starter-data-elasticsearch     (Elasticsearch)
- spring-boot-starter-amqp                   (RabbitMQ)
- minio (io.minio:minio:8.5.7)              (Download extracted text)
- lombok
```

---

## 📁 Project Structure

```
/document-server/
├── orchestrator-service/
│   ├── src/main/java/com/example/orchestrator/
│   │   ├── OrchestratorApplication.java
│   │   ├── controller/
│   │   │   ├── DocumentController.java        (Upload, status, download)
│   │   │   └── SearchController.java          (Proxy to indexing service)
│   │   ├── service/
│   │   │   ├── DocumentUploadService.java     (Handle uploads)
│   │   │   ├── MinioService.java              (MinIO operations)
│   │   │   └── MessagePublisherService.java   (RabbitMQ publish)
│   │   ├── consumer/
│   │   │   ├── ExtractionCompletedConsumer.java
│   │   │   └── IndexingCompletedConsumer.java
│   │   ├── entity/
│   │   │   └── FileMetadata.java              (JPA entity)
│   │   ├── repository/
│   │   │   └── FileMetadataRepository.java
│   │   ├── dto/
│   │   │   ├── ExtractionRequestMessage.java
│   │   │   ├── ExtractionCompletedMessage.java
│   │   │   ├── IndexingRequestMessage.java
│   │   │   └── IndexingCompletedMessage.java
│   │   └── config/
│   │       ├── RabbitMQConfig.java
│   │       ├── MinioConfig.java
│   │       └── H2Config.java
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── schema.sql
│   ├── pom.xml
│   └── Dockerfile
│
├── extraction-service/
│   ├── src/main/java/com/example/extraction/
│   │   ├── ExtractionApplication.java
│   │   ├── consumer/
│   │   │   └── ExtractionRequestConsumer.java  (Main worker)
│   │   ├── service/
│   │   │   ├── TikaExtractionService.java      (Text extraction - RIUSO DA google-like-search)
│   │   │   ├── ChunkingService.java            (Text chunking - RIUSO)
│   │   │   └── MinioService.java               (Download/upload)
│   │   ├── dto/
│   │   │   ├── ExtractionResult.java
│   │   │   └── ExtractionCompletedMessage.java
│   │   └── config/
│   │       ├── RabbitMQConfig.java
│   │       └── MinioConfig.java
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── pom.xml
│   └── Dockerfile
│
├── indexing-service/
│   ├── src/main/java/com/example/indexing/
│   │   ├── IndexingApplication.java
│   │   ├── consumer/
│   │   │   └── IndexingRequestConsumer.java    (Main worker)
│   │   ├── service/
│   │   │   ├── ElasticsearchIndexingService.java  (Index chunks - RIUSO DA google-like-search)
│   │   │   ├── SearchService.java                 (Search logic - RIUSO)
│   │   │   └── MinioService.java
│   │   ├── model/
│   │   │   └── SearchDocument.java                (RIUSO DA google-like-search)
│   │   ├── dto/
│   │   │   ├── SearchResultDto.java               (RIUSO)
│   │   │   └── IndexingCompletedMessage.java
│   │   ├── controller/
│   │   │   └── SearchController.java              (REST API for search)
│   │   └── config/
│   │       ├── RabbitMQConfig.java
│   │       ├── MinioConfig.java
│   │       └── ElasticsearchConfig.java           (RIUSO)
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── pom.xml
│   └── Dockerfile
│
├── docker-compose.yml
├── ARCHITECTURE.md (questo file)
└── README.md
```

---

## 🔒 Error Handling & Resilience

### **Retry Strategy**

```
1. RabbitMQ Message TTL: 5 minutes
2. Dead Letter Queues for failed messages
3. Max retry count: 3 (tracked in H2)
4. Exponential backoff: 1s, 2s, 4s
```

### **Failure Scenarios**

| Scenario | Handling |
|----------|----------|
| MinIO upload fails | Retry 3x, then mark as FAILED in DB |
| Tika extraction fails | Send to DLQ, mark FAILED in DB |
| Elasticsearch indexing fails | Retry 3x, send to DLQ |
| RabbitMQ down | Messages persist in queue (durable) |
| Service crash | Consumer auto-reconnects, reprocess message |
| Duplicate upload (same checksum) | Skip processing, return existing fileId |

### **Idempotency**

- fileId è UUID univoco
- Elasticsearch document ID è generato dal servizio
- Consumer può riprocessare stesso messaggio safely (idempotent)

---

## 📊 Monitoring & Observability

### **Health Checks**

```
GET /actuator/health

Orchestrator:
- H2 database connection
- MinIO connectivity
- RabbitMQ connectivity

Extraction Service:
- RabbitMQ connectivity
- MinIO connectivity

Indexing Service:
- Elasticsearch connectivity
- RabbitMQ connectivity
- MinIO connectivity
```

### **Metrics**

```
- Files uploaded (counter)
- Extraction duration (histogram)
- Indexing duration (histogram)
- Queue depth (gauge)
- Failed extractions (counter)
- Failed indexing (counter)
```

### **Logging**

```
Format: JSON structured logs
Level: INFO (production), DEBUG (development)

Key events:
- File uploaded: {fileId, filename, size}
- Extraction started: {fileId}
- Extraction completed: {fileId, duration, chunksCount}
- Indexing started: {fileId}
- Indexing completed: {fileId, duration, indexedDocs}
- Errors: {fileId, service, error, stacktrace}
```

---

## 🚀 Scalability

### **Horizontal Scaling**

```yaml
Extraction Service:
  Replicas: 2-10
  Reason: CPU-intensive (Tika parsing)
  Load balancing: RabbitMQ round-robin

Indexing Service:
  Replicas: 1-3
  Reason: I/O-intensive (Elasticsearch)
  
Orchestrator Service:
  Replicas: 1 (stateful - H2 file-based)
  Future: Migrate to PostgreSQL for multi-instance
```

### **Performance Targets**

```
- Upload throughput: 100 files/minute
- Extraction time: < 30s for PDF < 10MB
- Indexing time: < 10s for 1000 chunks
- Search latency: < 200ms (p95)
```

---

## 🔐 Security Considerations

1. **File Validation**: Check MIME type, max size (10GB)
2. **MinIO Access**: Pre-signed URLs per accesso temporaneo
3. **API Authentication**: JWT tokens (future)
4. **Virus Scanning**: ClamAV integration (future)
5. **Rate Limiting**: Per IP/user (future)

---

## 🧪 Testing Strategy

### **Unit Tests**
- Service layer logic
- Chunking algorithm
- DTO serialization

### **Integration Tests**
- RabbitMQ message flow
- MinIO upload/download
- Elasticsearch indexing
- H2 database operations

### **E2E Tests**
- Complete upload → extract → index → search workflow
- Error scenarios (service down, invalid file)

---

## 📝 API Examples

### **Upload File**

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@document.pdf"

Response:
{
  "fileId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "UPLOADED",
  "message": "File queued for extraction"
}
```

### **Check Status**

```bash
curl http://localhost:8080/api/documents/123e4567-e89b-12d3-a456-426614174000/status

Response:
{
  "fileId": "123e4567-e89b-12d3-a456-426614174000",
  "filename": "document.pdf",
  "uploadStatus": "INDEXED",
  "extractionStatus": "COMPLETED",
  "indexingStatus": "COMPLETED",
  "uploadedAt": "2025-11-22T10:00:00",
  "extractionCompletedAt": "2025-11-22T10:00:30",
  "indexingCompletedAt": "2025-11-22T10:00:45"
}
```

### **Search**

```bash
curl "http://localhost:8080/api/search?q=machine%20learning"

Response:
[
  {
    "documentId": "123e4567-e89b-12d3-a456-426614174000",
    "filename": "document.pdf",
    "score": 2.45,
    "highlights": [
      "...introduction to <mark>machine learning</mark>...",
      "...<mark>machine learning</mark> algorithms..."
    ],
    "metadata": {
      "author": "John Doe",
      "title": "ML Guide",
      "pageCount": 50
    }
  }
]
```

### **Download Original**

```bash
curl http://localhost:8080/api/documents/123e4567-e89b-12d3-a456-426614174000/download \
  --output document.pdf
```

---

## ✅ Advantages of This Architecture

1. **Loose Coupling**: Services communicate only via queues
2. **Fault Tolerance**: DLQ + retry mechanisms
3. **Scalability**: Each service scales independently
4. **Persistence**: MinIO + H2 ensure no data loss
5. **Auditability**: H2 tracks complete pipeline history
6. **Idempotency**: Safe message reprocessing
7. **Storage Efficiency**: Binary files on MinIO, not DB
8. **Reusability**: Extraction service is generic (any format)
9. **Async Processing**: Non-blocking uploads
10. **Technology Reuse**: Leverages existing Tika & Elasticsearch code

---

## 🔄 Future Enhancements

1. **Orchestrator**: Migrate H2 → PostgreSQL for HA
2. **Authentication**: Add JWT-based API security
3. **Webhooks**: Notify external systems on completion
4. **Batch Processing**: Accept ZIP files with multiple documents
5. **OCR**: Add Tesseract for image-based PDFs
6. **Real-time Status**: WebSocket for live progress updates
7. **Document Versioning**: Track file updates
8. **Multi-tenancy**: Isolate data per organization
9. **Caching**: Redis for frequently accessed documents
10. **Analytics**: Dashboard for processing metrics

---

## 🎯 Code Reuse from `google-like-search`

### **Files to Reuse As-Is**

```
FROM: /google-like-search/src/main/java/com/example/documentsearch/

Extraction Service ← 
  - service/DocumentService.java (extractMetadata, calculateChecksum, splitIntoChunks, applyMetadata)
  - config/AsyncConfig.java

Indexing Service ←
  - model/SearchDocument.java (complete)
  - dto/SearchResultDto.java (complete)
  - service/SearchService.java (search, searchRaw, getIndexedFilenames)
  - config/ElasticsearchConfig.java

Both Services ←
  - model/UploadStatus.java (for status tracking)
```

### **Adaptations Needed**

1. **DocumentService**: 
   - Remove direct Elasticsearch write → save to MinIO instead
   - Keep Tika extraction logic intact
   
2. **SearchService**:
   - Keep as-is, works perfectly
   
3. **SearchDocument**:
   - No changes needed

---

## 📌 Implementation Priority

1. ✅ Setup infrastructure (docker-compose.yml)
2. ✅ Create Orchestrator with H2 + MinIO + RabbitMQ
3. ✅ Create Extraction Service (reuse Tika code)
4. ✅ Create Indexing Service (reuse Elasticsearch code)
5. ✅ Test complete pipeline
6. ✅ Add error handling + DLQ
7. ✅ Add monitoring + health checks

---

**End of Architecture Document**
