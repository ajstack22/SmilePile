# SmilePile Migration Examples

This directory contains real-world examples from the SmilePile project, which used Atlas to migrate from Android-only to a dual-platform (Android + iOS) application.

## Purpose

These examples demonstrate:
- How Atlas was used in production
- Android feature analysis and iOS migration planning
- Story creation patterns for cross-platform development
- Real agent orchestration workflows

## Contents

### android_feature_burndown/
Detailed analysis of SmilePile's Android features, used to plan iOS implementation with exact feature parity.

**Key Files:**
- `_01_data_storage.md` - Room to Core Data migration
- `_05_photo_editor.md` - Complex UI component analysis
- `_06_security_pin.md` - Security feature implementation

### 09_STORIES/
Legacy story format and agent definitions used during SmilePile development.

### stories/
Actual user stories created during SmilePile's Atlas workflow implementation.

## Usage

These are **reference examples only**. They contain SmilePile-specific paths, package names, and architecture decisions that are not applicable to other projects.

**Use them to:**
- Understand how to structure your own feature analysis
- See real-world Atlas agent workflows in action
- Learn story creation patterns
- Understand cross-platform migration strategies

**Do NOT:**
- Copy paths or configuration directly
- Use SmilePile-specific package names
- Assume architectural decisions apply to your project

## Project Context

SmilePile is a secure photo management app with:
- Android: Kotlin + Jetpack Compose
- iOS: Swift + SwiftUI
- Features: Photo editing, categories, PIN security, backup/export
- Constraint: No file > 250 lines (enforced by Atlas)

The migration achieved 100% feature parity using Atlas's structured workflow.
