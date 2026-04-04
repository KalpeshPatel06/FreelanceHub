#!/bin/bash
# ============================================================
# ec2-setup.sh — One-time setup script for your EC2 instance
#
# Run this ONCE after launching a fresh Ubuntu EC2 instance:
#   chmod +x ec2-setup.sh
#   ./ec2-setup.sh
#
# What it installs:
#   - Java 17 (to run the Spring Boot JAR)
#   - MySQL 8 (the database)
#   - Sets up the database and user
# ============================================================

set -e  # exit immediately if any command fails

echo "============================================"
echo "  FreelanceHub EC2 Setup"
echo "============================================"

# ---- Step 1: Update system packages ----
echo ""
echo "[1/5] Updating system packages..."
sudo apt-get update -y
sudo apt-get upgrade -y

# ---- Step 2: Install Java 17 ----
echo ""
echo "[2/5] Installing Java 17..."
sudo apt-get install -y openjdk-17-jdk

# Verify Java installation
java -version
echo "Java 17 installed successfully ✓"

# ---- Step 3: Install MySQL 8 ----
echo ""
echo "[3/5] Installing MySQL 8..."
sudo apt-get install -y mysql-server

# Start MySQL and enable it to start on boot
sudo systemctl start mysql
sudo systemctl enable mysql

echo "MySQL installed and started ✓"

# ---- Step 4: Set up database and user ----
echo ""
echo "[4/5] Setting up database..."

# Prompt for credentials
read -p "Enter a MySQL password for 'freelancehub' user: " DB_PASS

# Create the database and user
sudo mysql -e "
  CREATE DATABASE IF NOT EXISTS freelancehub
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

  CREATE USER IF NOT EXISTS 'freelancehub'@'localhost'
    IDENTIFIED BY '${DB_PASS}';

  GRANT ALL PRIVILEGES ON freelancehub.* TO 'freelancehub'@'localhost';

  FLUSH PRIVILEGES;
"

echo "Database 'freelancehub' created ✓"
echo "User 'freelancehub'@'localhost' created ✓"

# ---- Step 5: Create application directory ----
echo ""
echo "[5/5] Creating application directory..."
mkdir -p ~/freelancehub
echo "Directory ~/freelancehub created ✓"

# ---- Print environment variable instructions ----
echo ""
echo "============================================"
echo "  Setup Complete!"
echo "============================================"
echo ""
echo "Add these to your GitHub Secrets:"
echo ""
echo "  DB_URL      = jdbc:mysql://localhost:3306/freelancehub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
echo "  DB_USERNAME = freelancehub"
echo "  DB_PASSWORD = ${DB_PASS}"
echo ""
echo "Your EC2 security group must have port 8080 open."
echo "The CI/CD pipeline will deploy the app automatically on push to main."
echo ""
echo "To view app logs after deployment:"
echo "  tail -f ~/freelancehub/app.log"
echo ""
echo "To start the app manually:"
echo "  cd ~/freelancehub"
echo "  nohup java -jar freelancehub.jar > app.log 2>&1 &"
