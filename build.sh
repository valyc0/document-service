#!/bin/bash

echo "======================================"
echo "📦 Building Document Server Projects"
echo "======================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BUILD_DIR=$(pwd)

# Function to build a Maven project
build_maven_project() {
    local project_name=$1
    local project_dir=$2
    local extra_args=$3
    
    echo -e "${YELLOW}📦 Building $project_name...${NC}"
    cd "$BUILD_DIR/$project_dir"
    
    if mvn clean package -DskipTests $extra_args; then
        echo -e "${GREEN}✅ $project_name built successfully${NC}"
        echo ""
        return 0
    else
        echo -e "${RED}❌ Failed to build $project_name${NC}"
        echo ""
        return 1
    fi
}

# Build orchestrator-service
build_maven_project "Orchestrator Service" "orchestrator-service" || exit 1

# Build extraction-service
build_maven_project "Extraction Service" "extraction-service" || exit 1

# Build indexing-service
build_maven_project "Indexing Service" "indexing-service" || exit 1

# Build ui-service with Vaadin production profile
echo -e "${YELLOW}📦 Building UI Service (Vaadin production mode)...${NC}"
cd "$BUILD_DIR/ui-service"
if mvn clean package -Pproduction -DskipTests; then
    echo -e "${GREEN}✅ UI Service built successfully${NC}"
    echo ""
else
    echo -e "${RED}❌ Failed to build UI Service${NC}"
    echo ""
    exit 1
fi

cd "$BUILD_DIR"

echo "======================================"
echo -e "${GREEN}✅ All projects built successfully!${NC}"
echo "======================================"
echo ""
echo "JARs created:"
echo "  - orchestrator-service/target/orchestrator-service-1.0.0.jar"
echo "  - extraction-service/target/extraction-service-1.0.0.jar"
echo "  - indexing-service/target/indexing-service-1.0.0.jar"
echo "  - ui-service/target/ui-service-1.0.0.jar"
echo ""
echo "Ready to start with: docker compose up -d"
