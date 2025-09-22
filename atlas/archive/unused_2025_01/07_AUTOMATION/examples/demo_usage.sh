#!/bin/bash

# Atlas Backlog Automation - Demo Usage Script
# This script demonstrates the key features of the Atlas automation system

echo "🚀 Atlas Backlog Automation Demo"
echo "=================================="
echo

# Change to automation directory
cd "$(dirname "$0")/.."

echo "📋 1. Setting up the system..."
echo "Installing git hooks..."
python atlas_cli.py git install-hooks
echo

echo "📊 2. Checking current backlog status..."
python atlas_cli.py backlog status
echo

echo "✨ 3. Creating sample stories..."

# Create a feature story
echo "Creating a feature story..."
python atlas_cli.py story create \
  --type feature \
  --title "Add user profile page" \
  --priority medium \
  --description "Users need a dedicated page to view and edit their profile information"

# Create a bug story
echo "Creating a bug story..."
python atlas_cli.py story create \
  --type bug \
  --title "Navigation menu doesn't work on mobile" \
  --priority high \
  --description "The hamburger menu fails to open on mobile devices"

# Create a tech debt story
echo "Creating a tech debt story..."
python atlas_cli.py story create \
  --type tech_debt \
  --title "Refactor database connection pooling" \
  --priority medium \
  --description "Current connection pooling implementation is inefficient"

echo

echo "🎯 4. Generating stories from templates..."

# Generate UI component story
python atlas_cli.py story generate \
  --type feature \
  --template user_interface \
  --param component=search_bar \
  --param page=homepage \
  --param user_type=visitor \
  --param action="search for content" \
  --param benefit="find relevant information quickly"

echo

echo "📈 5. Running smart prioritization..."
python atlas_cli.py backlog prioritize --auto
echo

echo "📊 6. Checking updated backlog status..."
python atlas_cli.py backlog status --detailed
echo

echo "💡 7. Getting story suggestions..."
python atlas_cli.py story suggest --context "user_experience" --count 3
echo

echo "📊 8. Generating analytics..."

# Velocity analysis
echo "Generating velocity report..."
python atlas_cli.py analytics velocity --weeks 4

# Quality check
echo "Running quality analysis..."
python atlas_cli.py quality check

echo

echo "🌊 9. Planning next wave..."
python atlas_cli.py wave plan --stories 5
echo

echo "📋 10. Running backlog scan..."
python atlas_cli.py backlog scan
echo

echo "🧹 11. Cleaning up backlog..."
python atlas_cli.py backlog clean
echo

echo "📊 12. Generating comprehensive dashboard..."
python atlas_cli.py dashboard export demo_dashboard.html
echo "Dashboard exported to: demo_dashboard.html"
echo

echo "🔍 13. Git integration report..."
python atlas_cli.py git report
echo

echo "🤖 14. Running full automation cycle (dry run)..."
python atlas_cli.py auto run --dry-run
echo

echo "✅ Demo completed!"
echo "=================="
echo
echo "Key files created:"
echo "- Stories in backlog/ directories"
echo "- Wave plans in backlog/waves/"
echo "- Dashboard: demo_dashboard.html"
echo "- Backlog metadata: .atlas/backlog_metadata.json"
echo
echo "Next steps:"
echo "1. Review the generated stories and dashboard"
echo "2. Customize config/backlog_config.yaml for your team"
echo "3. Set up GitHub Actions for automated workflows"
echo "4. Start using the CLI for daily backlog management"
echo
echo "Example daily commands:"
echo "- python atlas_cli.py story suggest"
echo "- python atlas_cli.py backlog status"
echo "- python atlas_cli.py dashboard export"
echo "- python atlas_cli.py auto run"
echo