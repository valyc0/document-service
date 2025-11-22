# Document Processing System

Sistema distribuito a 3 microservizi per processamento, indicizzazione e ricerca di documenti multi-formato, basato su architettura **Event-Driven** con **Storage Condiviso (MinIO)**.

## 🏗️ Architettura

- **Orchestrator Service** (8080): Gestisce upload, salva metadata su H2, coordina il flusso via RabbitMQ
- **Extraction Service** (8081): Estrae testo con Apache Tika, crea chunk, salva risultato su MinIO
- **Indexing Service** (8082): Indicizza chunk su Elasticsearch, espone API di ricerca

## 🚀 Quick Start

### Prerequisiti

- Docker & Docker Compose
- (Opzionale) Java 17+ e Maven per sviluppo locale

### Avvio Completo con Docker

```bash
# Clone/naviga nella directory
cd /home/valyc-pc/lavoro/document-server

# Build e avvio di tutti i servizi
docker-compose up --build

# Oppure in background
docker-compose up -d --build
```

### Accesso ai Servizi

| Servizio | URL | Credenziali |
|----------|-----|-------------|
| **Orchestrator API** | http://localhost:8080 | - |
| **Extraction Service** | http://localhost:8081/actuator/health | - |
| **Indexing Service** | http://localhost:8082 | - |
| **MinIO Console** | http://localhost:9001 | minioadmin / minioadmin |
| **RabbitMQ Management** | http://localhost:15672 | admin / admin |
| **Elasticsearch** | http://localhost:9200 | - |
| **H2 Console** | http://localhost:8080/h2-console | JDBC URL: jdbc:h2:file:/data/orchestrator-db |

## 📡 API Usage

### 1. Upload Document

```bash
curl -F "file=@document.pdf" http://localhost:8080/api/documents/upload

# Response
{
  "fileId": "123e4567-e89b-12d3-a456-426614174000",
  "filename": "document.pdf",
  "status": "UPLOADED",
  "message": "File uploaded and queued for extraction"
}
```

### 2. Check Processing Status

```bash
curl http://localhost:8080/api/documents/{fileId}/status

# Response
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

### 3. Search Documents

```bash
# GET method
curl "http://localhost:8080/api/search?q=machine%20learning&maxResults=5"

# POST method (recommended)
curl -X POST http://localhost:8082/api/search/query \
  -H "Content-Type: application/json" \
  -d '{"question": "machine learning", "maxResults": 5}'

# Response
[
  {
    "documentId": "123e4567-e89b-12d3-a456-426614174000",
    "filename": "document.pdf",
    "score": 2.45,
    "chunkIndex": 3,
    "highlights": [
      "...introduction to <mark>machine learning</mark>...",
      "...<mark>machine learning</mark> algorithms..."
    ],
    "metadata": {
      "author": "John Doe",
      "title": "ML Guide",
      "contentType": "application/pdf",
      "pageCount": 50
    }
  }
]
```

### 4. Download Original File

```bash
curl http://localhost:8080/api/documents/{fileId}/download --output document.pdf
```

### 5. List All Documents

```bash
# All documents
curl http://localhost:8080/api/documents

# Filter by status
curl http://localhost:8080/api/documents?status=INDEXED
```

### 6. Get Statistics

```bash
curl http://localhost:8080/api/documents/stats

# Response
{
  "total": 100,
  "uploaded": 5,
  "extracted": 3,
  "indexed": 92,
  "failed": 0
}
```

## 🔄 Processing Pipeline

```
1. Client uploads file → Orchestrator
   ↓
2. Orchestrator:
   - Saves file to MinIO (files/{fileId}/original.{ext})
   - Saves metadata to H2
   - Publishes to RabbitMQ: extraction-requests
   ↓
3. Extraction Service:
   - Downloads file from MinIO
   - Extracts text with Tika
   - Creates chunks
   - Uploads JSON to MinIO (files/{fileId}/extracted-text.json)
   - Publishes to RabbitMQ: extraction-completed
   ↓
4. Orchestrator:
   - Updates H2: extraction status = COMPLETED
   - Publishes to RabbitMQ: indexing-requests
   ↓
5. Indexing Service:
   - Downloads extracted-text.json from MinIO
   - Indexes each chunk to Elasticsearch
   - Publishes to RabbitMQ: indexing-completed
   ↓
6. Orchestrator:
   - Updates H2: indexing status = COMPLETED
   - Pipeline complete! 🎉
```

## 📁 Project Structure

```
document-server/
├── orchestrator-service/       # Port 8080 - H2 + MinIO + RabbitMQ
├── extraction-service/         # Port 8081 - Tika + MinIO + RabbitMQ
├── indexing-service/           # Port 8082 - Elasticsearch + MinIO + RabbitMQ
├── docker-compose.yml
├── ARCHITECTURE.md
└── README.md
```

## 🔧 Development

### Build Singolo Servizio

```bash
cd orchestrator-service
mvn clean package
java -jar target/orchestrator-service-1.0.0.jar
```

### Run con Profili

```bash
# Development (usa localhost per infra)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production (usa docker hostnames)
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## 🐳 Docker Commands

```bash
# Build singolo servizio
docker-compose build orchestrator-service

# Start solo infrastruttura
docker-compose up minio rabbitmq elasticsearch

# Scale extraction service
docker-compose up --scale extraction-service=3

# Logs
docker-compose logs -f orchestrator-service
docker-compose logs -f extraction-service
docker-compose logs -f indexing-service

# Stop e cleanup
docker-compose down
docker-compose down -v  # Rimuove anche i volumi
```

## 📊 Monitoring

### Health Checks

```bash
# Orchestrator
curl http://localhost:8080/actuator/health

# Extraction
curl http://localhost:8081/actuator/health

# Indexing
curl http://localhost:8082/actuator/health

# Elasticsearch
curl http://localhost:9200/_cluster/health
```

### RabbitMQ Queues

Accedi a http://localhost:15672 e monitora:
- `extraction-requests`
- `extraction-completed`
- `indexing-requests`
- `indexing-completed`

### Elasticsearch Indices

```bash
# List indices
curl http://localhost:9200/_cat/indices?v

# Count documents
curl http://localhost:9200/documents/_count?pretty

# View mapping
curl http://localhost:9200/documents/_mapping?pretty
```

### MinIO Buckets

Accedi a http://localhost:9001 e verifica bucket `document-processing`:
```
files/
  ├── {fileId-1}/
  │   ├── original.pdf
  │   └── extracted-text.json
  ├── {fileId-2}/
  │   ├── original.docx
  │   └── extracted-text.json
```

## 🧪 Testing

### Test Upload & Search

```bash
# 1. Upload un file
FILE_ID=$(curl -s -F "file=@test.pdf" http://localhost:8080/api/documents/upload | jq -r '.fileId')
echo "File ID: $FILE_ID"

# 2. Attendi qualche secondo per il processing
sleep 10

# 3. Check status
curl http://localhost:8080/api/documents/$FILE_ID/status | jq

# 4. Search
curl "http://localhost:8082/api/search?q=test" | jq
```

### Test con Curl

```bash
# Upload vari formati
curl -F "file=@document.pdf" http://localhost:8080/api/documents/upload
curl -F "file=@report.docx" http://localhost:8080/api/documents/upload
curl -F "file=@data.xlsx" http://localhost:8080/api/documents/upload
curl -F "file=@page.html" http://localhost:8080/api/documents/upload
curl -F "file=@notes.txt" http://localhost:8080/api/documents/upload
```

## 🔐 Security (TODO per produzione)

- [ ] Aggiungere autenticazione JWT
- [ ] Abilitare HTTPS
- [ ] Configurare Elasticsearch security
- [ ] Implementare rate limiting
- [ ] Aggiungere virus scanning (ClamAV)
- [ ] Network policies per isolamento container

## 📈 Performance

### Scaling

```bash
# Scale Extraction Service (CPU-intensive)
docker-compose up --scale extraction-service=5

# Scale Indexing Service (I/O-intensive)
docker-compose up --scale indexing-service=2
```

### Tuning

- **Extraction**: Aumenta `spring.rabbitmq.listener.simple.max-concurrency`
- **Indexing**: Aumenta Elasticsearch heap in docker-compose.yml
- **RabbitMQ**: Configura prefetch per backpressure
- **MinIO**: Usa persistent volumes per produzione

## 🐛 Troubleshooting

### Extraction Fallisce

```bash
# Check logs
docker-compose logs extraction-service

# Check RabbitMQ DLQ
curl -u admin:admin http://localhost:15672/api/queues/%2F/extraction-requests-dlq
```

### Indexing Fallisce

```bash
# Check Elasticsearch
curl http://localhost:9200/_cluster/health

# Check logs
docker-compose logs indexing-service
```

### File non trovato su MinIO

```bash
# List objects in bucket
mc ls myminio/document-processing/files/{fileId}/

# Oppure via MinIO Console
open http://localhost:9001
```

## 📚 Related Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Architettura dettagliata del sistema
- [Apache Tika Documentation](https://tika.apache.org/)
- [Elasticsearch Guide](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)
- [MinIO Documentation](https://min.io/docs/minio/linux/index.html)

## 🎯 Supported File Formats

Grazie ad Apache Tika, il sistema supporta automaticamente:

- **Documents**: PDF, DOC, DOCX, RTF, ODT, TXT
- **Spreadsheets**: XLS, XLSX, ODS, CSV
- **Web**: HTML, HTM, XML
- **Data**: JSON
- **Markup**: Markdown (MD)
- **E molti altri formati...**

## 🤝 Contributing

Questo progetto riusa codice da `google-like-search`:
- **Tika extraction logic** da `DocumentService`
- **Elasticsearch indexing** da `SearchService`
- **Search API** da `SearchController`

## 📝 License

MIT License

---

**Made with ❤️ - Reusing battle-tested code from google-like-search**
