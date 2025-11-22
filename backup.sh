#!/bin/bash

# Script di backup per document-server
# Crea un archivio tar.gz nella directory superiore escludendo file non necessari

PROJECT_DIR="/home/valyc-pc/lavoro/document-server"
BACKUP_DIR="/home/valyc-pc/lavoro"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_NAME="document-server_backup_${TIMESTAMP}.tar.gz"

echo "🔄 Avvio backup di document-server..."
echo "📁 Directory sorgente: $PROJECT_DIR"
echo "💾 Destinazione: $BACKUP_DIR/$BACKUP_NAME"

cd "$PROJECT_DIR/.." || exit 1

tar -czf "$BACKUP_DIR/$BACKUP_NAME" \
    --exclude='document-server/*/target' \
    --exclude='document-server/**/target' \
    --exclude='document-server/.git' \
    --exclude='document-server/**/.git' \
    --exclude='document-server/node_modules' \
    --exclude='document-server/**/node_modules' \
    --exclude='document-server/**/*.jar.original' \
    --exclude='document-server/**/.idea' \
    --exclude='document-server/**/.vscode' \
    --exclude='document-server/**/*.class' \
    --exclude='document-server/**/*.log' \
    --exclude='document-server/**/.DS_Store' \
    document-server/

if [ $? -eq 0 ]; then
    BACKUP_SIZE=$(du -h "$BACKUP_DIR/$BACKUP_NAME" | cut -f1)
    echo "✅ Backup completato con successo!"
    echo "📦 File: $BACKUP_NAME"
    echo "📊 Dimensione: $BACKUP_SIZE"
    echo ""
    echo "Per ripristinare:"
    echo "  cd /home/valyc-pc/lavoro"
    echo "  tar -xzf $BACKUP_NAME"
else
    echo "❌ Errore durante il backup!"
    exit 1
fi
