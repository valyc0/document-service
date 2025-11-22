#!/bin/bash

docker compose down -v

# Remove all JAR files
find . -type f -name "*.jar" -delete
