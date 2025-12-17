#!/bin/bash

# SmilePile Website Production Deployment Script
# Deploys to https://smilepile.app via SSH + rsync (same method as Manylla)

set -e  # Exit on error
set -o pipefail  # Fail on pipe errors

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEBSITE_ROOT="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$WEBSITE_ROOT")"

# SSH Configuration (same as Manylla/StackMap)
SSH_HOST="stackmap-cpanel"
DEPLOY_PATH="~/smilepile"

# Backup configuration
BACKUP_NAME="smilepile-backup-$(date +%Y%m%d-%H%M%S)"

echo -e "${BLUE}🚀 SmilePile Website Deployment${NC}"
echo "=================================================="
echo ""

# ============================================================================
# STEP 1: Verify SSH Connection
# ============================================================================

echo -e "${YELLOW}Step 1: Verifying SSH connection...${NC}"
echo "─────────────────────────────────────"

if ! grep -q "Host $SSH_HOST" ~/.ssh/config 2>/dev/null; then
    echo -e "${RED}❌ SSH config not found for $SSH_HOST${NC}"
    echo ""
    echo "Please add to ~/.ssh/config:"
    echo ""
    echo "Host stackmap-cpanel"
    echo "  HostName YOUR_CPANEL_IP"
    echo "  Port 21098"
    echo "  User stachblx"
    echo "  IdentityFile ~/.ssh/id_rsa_cpanel"
    echo ""
    exit 1
fi

# Test SSH connection
if ! ssh $SSH_HOST "echo 'Connection successful'" >/dev/null 2>&1; then
    echo -e "${RED}❌ Cannot connect to $SSH_HOST${NC}"
    echo "   Check your SSH configuration and network connection"
    exit 1
fi

echo -e "${GREEN}✅ SSH connection verified${NC}"
echo ""

# ============================================================================
# STEP 2: Create Backup
# ============================================================================

echo -e "${YELLOW}Step 2: Creating backup...${NC}"
echo "─────────────────────────────────────"

# Create backups directory
mkdir -p "$WEBSITE_ROOT/backups"

# Backup existing website files
ssh $SSH_HOST "if [ -d $DEPLOY_PATH ]; then tar -czf ~/$BACKUP_NAME.tar.gz -C $DEPLOY_PATH . 2>/dev/null; fi" || true

# Download backup
scp $SSH_HOST:~/$BACKUP_NAME.tar.gz "$WEBSITE_ROOT/backups/" 2>/dev/null || echo "  Note: No existing website to backup"

echo -e "${GREEN}✅ Backup created: $BACKUP_NAME.tar.gz${NC}"
echo "   Rollback: ssh $SSH_HOST 'rm -rf $DEPLOY_PATH && mkdir -p $DEPLOY_PATH && tar -xzf ~/$BACKUP_NAME.tar.gz -C $DEPLOY_PATH'"
echo ""

# ============================================================================
# STEP 3: Build Website
# ============================================================================

echo -e "${YELLOW}Step 3: Building website...${NC}"
echo "─────────────────────────────────────"

cd "$WEBSITE_ROOT"

# Install dependencies
echo "📦 Installing dependencies..."
npm ci --production=false

# Run build
echo "🔨 Building Astro site..."
npm run build

# Verify build output
if [ ! -d "dist" ] || [ ! -f "dist/index.html" ]; then
    echo -e "${RED}❌ Build failed - dist directory not found or incomplete${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Website built successfully${NC}"
echo "   Output: $WEBSITE_ROOT/dist"
echo ""

# ============================================================================
# STEP 4: Deploy to cPanel
# ============================================================================

echo -e "${YELLOW}Step 4: Deploying to cPanel...${NC}"
echo "─────────────────────────────────────"

# Create deployment directory on server
echo "📁 Creating deployment directory..."
ssh $SSH_HOST "mkdir -p $DEPLOY_PATH"

# Deploy website files via rsync
echo "📤 Uploading files to $SSH_HOST:$DEPLOY_PATH..."
rsync -avz --delete \
    --exclude='.git' \
    --exclude='node_modules' \
    --exclude='.DS_Store' \
    dist/ $SSH_HOST:$DEPLOY_PATH/ || {
        echo -e "${RED}❌ Deployment failed${NC}"
        echo "   Rollback available: ssh $SSH_HOST 'rm -rf $DEPLOY_PATH && mkdir -p $DEPLOY_PATH && tar -xzf ~/$BACKUP_NAME.tar.gz -C $DEPLOY_PATH'"
        exit 1
}

echo -e "${GREEN}✅ Files uploaded successfully${NC}"
echo ""

# ============================================================================
# STEP 5: Verify Deployment
# ============================================================================

echo -e "${YELLOW}Step 5: Verifying deployment...${NC}"
echo "─────────────────────────────────────"

# Check key files exist
REQUIRED_FILES=(
    "index.html"
    "privacy/index.html"
    "terms/index.html"
    "support/index.html"
    "404.html"
    "robots.txt"
)

ALL_FILES_PRESENT=true
for file in "${REQUIRED_FILES[@]}"; do
    if ssh $SSH_HOST "[ -f $DEPLOY_PATH/$file ]"; then
        echo "  ✓ $file"
    else
        echo -e "  ${RED}✗ $file (missing)${NC}"
        ALL_FILES_PRESENT=false
    fi
done

if [ "$ALL_FILES_PRESENT" = false ]; then
    echo -e "${YELLOW}⚠️  Some files are missing${NC}"
    echo "   Deployment may be incomplete"
else
    echo -e "${GREEN}✅ All required files present${NC}"
fi

echo ""

# ============================================================================
# DEPLOYMENT SUMMARY
# ============================================================================

echo ""
echo -e "${GREEN}═══════════════════════════════════════════${NC}"
echo -e "${GREEN}    ✅ DEPLOYMENT SUCCESSFUL${NC}"
echo -e "${GREEN}═══════════════════════════════════════════${NC}"
echo ""
echo "Website URL:     https://smilepile.app"
echo "Deployment Path: $SSH_HOST:$DEPLOY_PATH"
echo "Backup:          $BACKUP_NAME.tar.gz"
echo "Deployed At:     $(date)"
echo ""
echo "Next Steps:"
echo "  1. Visit https://smilepile.app to verify"
echo "  2. Test all pages (privacy, terms, support)"
echo "  3. Check mobile responsiveness"
echo "  4. Verify query parameter redirects:"
echo "     - https://smilepile.app/?privacy → /privacy"
echo "     - https://smilepile.app/?tos → /terms"
echo ""
echo "Rollback (if needed):"
echo "  ssh $SSH_HOST 'rm -rf $DEPLOY_PATH && mkdir -p $DEPLOY_PATH && tar -xzf ~/$BACKUP_NAME.tar.gz -C $DEPLOY_PATH'"
echo ""
