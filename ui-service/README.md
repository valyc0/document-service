# UI Service - Vaadin 24

Interfaccia web per Document Server con Vaadin 24.

## Funzionalità

- **📤 Upload File**: Carica documenti (PDF, DOCX, TXT, ecc.)
- **📋 Grid Documenti**: Lista con stato di elaborazione
- **⬇️ Download**: Scarica file originali
- **🗑️ Cancellazione**: Elimina documenti (DB + MinIO + Elasticsearch)
- **🔍 Ricerca Google-like**: Ricerca full-text con highlighting

## Build

```bash
mvn clean package -Pproduction
```

## Run locale

```bash
mvn spring-boot:run
```

Apri: http://localhost:8090

## Docker

```bash
docker build -t ui-service .
docker run -p 8090:8090 -e orchestrator.url=http://localhost:8080 ui-service
```

## Variabili d'ambiente

- `orchestrator.url`: URL dell'orchestrator (default: http://orchestrator-service:8080)
