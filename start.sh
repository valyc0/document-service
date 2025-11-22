#!/bin/bash

echo "======================================"
echo "🚀 Starting Document Server"
echo "======================================"
echo ""

# Check if JARs exist
missing_jars=0

if [ ! -f "orchestrator-service/target/orchestrator-service-1.0.0.jar" ]; then
    echo "⚠️  orchestrator-service JAR not found"
    missing_jars=1
fi

if [ ! -f "extraction-service/target/extraction-service-1.0.0.jar" ]; then
    echo "⚠️  extraction-service JAR not found"
    missing_jars=1
fi

if [ ! -f "indexing-service/target/indexing-service-1.0.0.jar" ]; then
    echo "⚠️  indexing-service JAR not found"
    missing_jars=1
fi

if [ ! -f "ui-service/target/ui-service-1.0.0.jar" ]; then
    echo "⚠️  ui-service JAR not found"
    missing_jars=1
fi

if [ $missing_jars -eq 1 ]; then
    echo ""
    echo "📦 Building projects first..."
    echo ""
    ./build.sh || exit 1
fi

echo ""
echo "🐳 Starting Docker Compose services..."
echo ""

docker compose up -d

echo ""
echo "======================================"
echo "✅ Document Server Started"
echo "======================================"
echo ""
echo "Services available at:"
echo "  📄 Orchestrator API: http://localhost:8080"
echo "  🎨 Web UI (Vaadin): http://localhost:8090"
echo "  🗄️  H2 Console: http://localhost:8083"
echo "  📦 MinIO Console: http://localhost:9001"
echo "  🐰 RabbitMQ Management: http://localhost:15672"
echo "  🔍 Elasticsearch: http://localhost:9200"
echo ""
echo "To view logs: docker compose logs -f"
echo "To stop: docker compose down"
