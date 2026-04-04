#!/bin/bash
# ============================================================
# run-local.sh — Run FreelanceHub locally for development
#
# Prerequisites:
#   - Java 17 installed
#   - MySQL running locally on port 3306
#   - Database 'freelancehub' created
#
# Usage:
#   chmod +x run-local.sh
#   ./run-local.sh
# ============================================================

echo "============================================"
echo "  FreelanceHub - Local Development"
echo "============================================"

# Set environment variables for local development
# These override the values in application.properties
export DB_URL="jdbc:mysql://localhost:3306/freelancehub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="root"
export JWT_SECRET="LocalDevelopmentSecretKeyThatIsLongEnough1234567890"
export JWT_EXPIRATION="86400000"
export SERVER_PORT="8080"
# S3 is optional - leave these blank to skip S3 features
export AWS_REGION="us-east-1"
export AWS_S3_BUCKET="freelancehub-profiles"

echo ""
echo "Starting application on http://localhost:8080"
echo "Press Ctrl+C to stop."
echo ""

cd backend && mvn spring-boot:run
