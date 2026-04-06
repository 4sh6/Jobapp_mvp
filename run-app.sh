#!/bin/bash

# Run JobApp with Gmail SMTP Configuration
# This script loads environment variables from .env and starts the application

set -a  # Export all variables

# Check if .env exists
if [ ! -f ".env" ]; then
    echo "❌ .env file not found!"
    echo ""
    echo "Please run setup first:"
    echo "  chmod +x setup-gmail.sh && ./setup-gmail.sh"
    exit 1
fi

# Load environment variables from .env
source .env

set +a

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     JobApp - Starting with Gmail SMTP Configuration       ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "📧 Email: $MAIL_USERNAME"
echo "🗄️  Database: $DB_URL"
echo ""

# Make mvnw executable if it isn't already
if [ ! -x "./mvnw" ]; then
    chmod +x ./mvnw
fi

# Clean and build in the background, then run
echo "🔨 Building application..."
./mvnw clean package -DskipTests > /dev/null 2>&1

echo "🚀 Starting application..."
echo ""

./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"

