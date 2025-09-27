# CRITICAL INSTRUCTIONS - READ FIRST

## STRICT ADHERENCE TO REQUIREMENTS - NO UNREQUESTED FEATURES

**ABSOLUTELY NO UNREQUESTED FUNCTIONALITY**:
- DO NOT add search bars, search functionality, or filtering unless EXPLICITLY requested
- DO NOT add favorites, bookmarking, or starring features unless EXPLICITLY requested
- DO NOT add sorting, ordering, or arrangement features unless EXPLICITLY requested
- DO NOT add any "nice to have" or "helpful" features not in the requirements
- DO NOT anticipate future needs or add "foundation" for unrequested features
- DO NOT add UI elements, buttons, or controls for functionality not requested
- If something seems "missing" or "would be useful" - ASK FIRST, don't implement

**IMPLEMENTATION DISCIPLINE**:
- ONLY implement what is EXPLICITLY requested in the task
- Remove any code that provides unrequested functionality
- Every line of code must directly support a requested requirement
- No speculative coding or pre-emptive feature additions
- When in doubt, implement LESS rather than MORE

Do NOT leave things out. If I KNOW SOMETHING IS NOT GOING TO WORK, I SHOULD TELL THE USER! Be an open collaborator and do NOT try to reduce work by taking quick fixes, ALWAYS do full evaluations and utilize @atlas/core/atlas_workflow.py in order to have a guided experience in which the user only talks to Claude, and does not run scripts directly.

# CLAUDE.md - SmilePile iOS Development Guide

## Project Overview
SmilePile is an iOS app for photo management with Kids Mode functionality.

## Key Components
- ContentView.swift - Main app view with Kids Mode/Parent Mode switching
- KidsModeViewModel - Manages Kids Mode state and photo filtering
- PhotoGalleryView - Photo gallery display
- CategoryManagementView - Category management interface

## Common Issues & Solutions

### App Not Full Screen
- Check that views use .edgesIgnoringSafeArea(.all) or .ignoresSafeArea()
- Ensure NavigationStack/NavigationView isn't adding unwanted padding
- Verify no TabView or other containers are constraining the view

### Build Errors
- Duplicate type definitions (Category, Photo, etc.) - ensure types are only defined once
- Color extension conflicts - check for multiple Color.init(hex:) definitions
- Missing CoreData entities - ensure .xcdatamodeld is properly configured

## Running the App
```bash
xcodebuild -project SmilePile.xcodeproj -scheme SmilePile -configuration Debug -destination 'platform=iOS Simulator,id=EE3F2A09-2BA9-463D-8C07-323B0688FAE5' -derivedDataPath ./DerivedData build
xcrun simctl install "EE3F2A09-2BA9-463D-8C07-323B0688FAE5" DerivedData/Build/Products/Debug-iphonesimulator/SmilePile.app
xcrun simctl launch "EE3F2A09-2BA9-463D-8C07-323B0688FAE5" com.smilepile.SmilePile
```