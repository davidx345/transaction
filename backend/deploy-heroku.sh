#!/bin/bash

# Heroku Deployment Script for Fintech Reconciliation Engine
# This script automates the Heroku deployment process

set -e  # Exit on error

echo "🚀 Starting Heroku Deployment Setup..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Heroku CLI is installed
if ! command -v heroku &> /dev/null; then
    echo -e "${RED}❌ Heroku CLI is not installed!${NC}"
    echo "Please install it from: https://devcenter.heroku.com/articles/heroku-cli"
    exit 1
fi

echo -e "${GREEN}✅ Heroku CLI found${NC}"

# Check if logged in to Heroku
if ! heroku auth:whoami &> /dev/null; then
    echo -e "${YELLOW}⚠️  Not logged in to Heroku. Opening login...${NC}"
    heroku login
fi

echo -e "${GREEN}✅ Logged in to Heroku${NC}"
echo ""

# Prompt for app name
read -p "Enter your Heroku app name (e.g., fintech-recon-prod): " APP_NAME

if [ -z "$APP_NAME" ]; then
    echo -e "${RED}❌ App name cannot be empty${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}📦 Creating Heroku app: ${APP_NAME}${NC}"

# Create Heroku app
if heroku create "$APP_NAME" 2>/dev/null; then
    echo -e "${GREEN}✅ App created successfully${NC}"
else
    echo -e "${YELLOW}⚠️  App might already exist. Continuing...${NC}"
    heroku git:remote -a "$APP_NAME"
fi

echo ""
echo -e "${YELLOW}🗄️  Adding PostgreSQL database...${NC}"

# Add PostgreSQL
if heroku addons:create heroku-postgresql:essential-0 -a "$APP_NAME" 2>/dev/null; then
    echo -e "${GREEN}✅ PostgreSQL added${NC}"
else
    echo -e "${YELLOW}⚠️  PostgreSQL might already exist${NC}"
fi

echo ""
echo -e "${YELLOW}⚙️  Configuring environment variables...${NC}"

# Set environment variables
heroku config:set SPRING_PROFILES_ACTIVE=prod -a "$APP_NAME"
heroku config:set JAVA_OPTS="-Xmx512m -Xms256m" -a "$APP_NAME"

echo -e "${GREEN}✅ Environment variables configured${NC}"

echo ""
echo -e "${YELLOW}🐰 RabbitMQ Setup${NC}"
read -p "Do you want to add CloudAMQP (RabbitMQ)? (y/N): " ADD_RABBITMQ

if [[ "$ADD_RABBITMQ" =~ ^[Yy]$ ]]; then
    if heroku addons:create cloudamqp:lemur -a "$APP_NAME" 2>/dev/null; then
        echo -e "${GREEN}✅ CloudAMQP added${NC}"
    else
        echo -e "${YELLOW}⚠️  Failed to add CloudAMQP (might need billing)${NC}"
        heroku config:set RABBITMQ_HOST=localhost -a "$APP_NAME"
        heroku config:set RABBITMQ_PORT=5672 -a "$APP_NAME"
    fi
else
    echo "Skipping RabbitMQ. Setting dummy values..."
    heroku config:set RABBITMQ_HOST=localhost -a "$APP_NAME"
    heroku config:set RABBITMQ_PORT=5672 -a "$APP_NAME"
fi

echo ""
echo -e "${YELLOW}📊 Current Configuration:${NC}"
heroku config -a "$APP_NAME"

echo ""
echo -e "${YELLOW}🏗️  Building application...${NC}"

# Check if Maven is installed
if command -v mvn &> /dev/null; then
    echo "Building with Maven..."
    mvn clean package -DskipTests
    echo -e "${GREEN}✅ Build successful${NC}"
else
    echo -e "${YELLOW}⚠️  Maven not found locally. Heroku will build during deployment.${NC}"
fi

echo ""
echo -e "${YELLOW}📤 Deploying to Heroku...${NC}"

# Initialize git if needed
if [ ! -d .git ]; then
    git init
    git add .
    git commit -m "Initial commit for Heroku deployment"
fi

# Add Heroku remote if not exists
if ! git remote get-url heroku &> /dev/null; then
    heroku git:remote -a "$APP_NAME"
fi

# Deploy
echo "Pushing to Heroku..."
git push heroku main || git push heroku master

echo ""
echo -e "${GREEN}✅ Deployment complete!${NC}"

echo ""
echo -e "${YELLOW}🔍 Checking app status...${NC}"
sleep 5
heroku ps -a "$APP_NAME"

echo ""
echo -e "${YELLOW}📋 Recent logs:${NC}"
heroku logs --tail -n 50 -a "$APP_NAME"

echo ""
echo -e "${GREEN}🎉 Deployment Summary:${NC}"
echo -e "App Name: ${GREEN}${APP_NAME}${NC}"
echo -e "App URL: ${GREEN}https://${APP_NAME}.herokuapp.com${NC}"
echo ""
echo "Next steps:"
echo "1. Test your API: curl https://${APP_NAME}.herokuapp.com/api/disputes"
echo "2. View logs: heroku logs --tail -a ${APP_NAME}"
echo "3. Update frontend env: VITE_API_BASE_URL=https://${APP_NAME}.herokuapp.com"
echo ""
echo -e "${GREEN}✅ All done!${NC}"
