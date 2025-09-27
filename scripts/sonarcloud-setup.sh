#!/bin/bash

# SonarCloud Setup Script for SmilePile
# Run this once to set up the project in SonarCloud

echo "🚀 SonarCloud Setup for SmilePile"
echo "================================="
echo ""
echo "Since SmilePile is a PUBLIC GitHub repository, you get unlimited free analysis!"
echo ""
echo "📋 SETUP STEPS:"
echo ""
echo "1. IMPORT PROJECT TO SONARCLOUD:"
echo "   ➜ Go to: https://sonarcloud.io/projects/create"
echo "   ➜ Choose: 'Import from GitHub'"
echo "   ➜ Select: ajstack22/SmilePile"
echo "   ➜ Choose: 'Free plan' (unlimited for public repos)"
echo ""
echo "2. GET YOUR TOKEN (if you don't have one):"
echo "   ➜ Go to: https://sonarcloud.io/account/security"
echo "   ➜ Generate a new token"
echo "   ➜ Save it as: export SONAR_TOKEN='your-token'"
echo ""
echo "3. RUN YOUR FIRST ANALYSIS:"
echo "   ➜ ./scripts/sonar-analysis.sh"
echo ""
echo "📊 After setup, view your dashboard at:"
echo "   https://sonarcloud.io/project/overview?id=ajstack22_SmilePile"
echo ""
echo "✅ This is a one-time setup. After this, you can run analysis anytime!"
echo ""

# Check if project exists
if [ -n "$SONAR_TOKEN" ] || [ -f "$HOME/.manylla-env" ]; then
    [ -f "$HOME/.manylla-env" ] && source "$HOME/.manylla-env"

    echo "🔍 Checking if project exists in SonarCloud..."

    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer $SONAR_TOKEN" \
        "https://sonarcloud.io/api/projects/search?organization=ajstack22&projects=ajstack22_SmilePile")

    if [ "$RESPONSE" = "200" ]; then
        echo "✅ Project already exists in SonarCloud!"
        echo ""
        echo "You can run analysis now:"
        echo "   ./scripts/sonar-analysis.sh"
    else
        echo "⚠️  Project not found in SonarCloud (HTTP $RESPONSE)"
        echo "Please complete step 1 above to import the project."
    fi
else
    echo "ℹ️  No token found. Please complete steps above."
fi

echo ""
echo "Need help? Check the docs:"
echo "   cat docs/SONARCLOUD.md"