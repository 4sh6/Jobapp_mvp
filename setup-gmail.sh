#!/bin/bash

# Gmail SMTP Configuration Setup Script
# This script helps you configure Gmail SMTP for OTP emails

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     JobApp - Gmail SMTP Configuration Setup               ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if .env file exists
if [ -f ".env" ]; then
    echo "⚠️  .env file already exists. Do you want to overwrite it? (y/n)"
    read -r response
    if [ "$response" != "y" ]; then
        echo "Cancelled. Using existing .env file."
        exit 0
    fi
fi

echo ""
echo "📧 Gmail SMTP Configuration"
echo "────────────────────────────────────────────────────────────"
echo ""
echo "You need a Gmail account with 2FA enabled."
echo "Generate an App Password at: https://myaccount.google.com/apppasswords"
echo ""

read -p "Enter your Gmail email (e.g., user@gmail.com): " MAIL_USERNAME
read -sp "Enter your 16-character App Password (won't be displayed): " MAIL_PASSWORD
echo ""

echo ""
echo "🗄️  Database Configuration (Optional)"
echo "────────────────────────────────────────────────────────────"
echo "Current defaults: H2 in-memory database"
echo ""

read -p "Database URL [jdbc:h2:mem:testdb]: " DB_URL
DB_URL=${DB_URL:-jdbc:h2:mem:testdb}

read -p "Database username [sa]: " DB_USERNAME
DB_USERNAME=${DB_USERNAME:-sa}

read -sp "Database password [empty]: " DB_PASSWORD
DB_PASSWORD=${DB_PASSWORD:-}
echo ""

# Create .env file
cat > .env << EOF
# Email Configuration
MAIL_USERNAME=$MAIL_USERNAME
MAIL_PASSWORD=$MAIL_PASSWORD

# Database Configuration
DB_URL=$DB_URL
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
EOF

echo ""
echo "✅ Configuration saved to .env file"
echo ""
echo "🔐 Security Notice:"
echo "   - .env file is in .gitignore (won't be committed to Git)"
echo "   - Environment variables are NOT logged or displayed"
echo ""
echo "🚀 To start the application with this configuration, run:"
echo "────────────────────────────────────────────────────────────"
echo "   export \$(cat .env | xargs) && ./mvnw spring-boot:run"
echo ""
echo "   OR"
echo ""
echo "   chmod +x run-app.sh && ./run-app.sh"
echo "────────────────────────────────────────────────────────────"
echo ""

