#!/bin/bash

echo "🛑 Stopping Document Processing System..."
echo ""

docker-compose down

echo ""
echo "✅ All services stopped"
echo ""
echo "💡 To remove all data (volumes), run:"
echo "   docker-compose down -v"
echo ""
