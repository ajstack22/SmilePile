# SmilePile 📸

A child-safe photo gallery app for Android that creates a private, curated space for children to enjoy family photos.

## 🎯 Overview

SmilePile is an Android application designed specifically for young children to safely view and interact with family photos. The app features a simple, intuitive interface with parental controls and zero internet connectivity for maximum privacy and safety.

## ✨ Features

### Child Mode
- **Simple Category Navigation**: Large, colorful tiles for easy category selection
- **Fullscreen Photo Viewing**: Swipe through photos with minimal UI distractions
- **Child-Safe Design**: No ads, no internet access, no external sharing

### Parent Mode
- **Secure Access**: Math-based authentication to prevent accidental access
- **Photo Import**: Import photos from device storage with category assignment
- **Category Management**: Create, edit, and delete photo categories
- **Privacy Controls**: EXIF metadata automatically stripped from all photos
- **Permission Management**: Granular control over app permissions

## 🔒 Privacy & Safety

- **100% Offline**: No internet permissions or connectivity
- **Local Storage Only**: All photos stored on device
- **Metadata Removal**: EXIF data automatically stripped
- **No Analytics**: No tracking or data collection
- **Child-Safe**: No ads, in-app purchases, or external links

## 📱 Compatibility

- **Minimum SDK**: API 26 (Android 8.0)
- **Target SDK**: API 35 (Android 16)
- **Tested On**: Pixel 9 Emulator, various physical devices
- **Screen Sizes**: Phones and tablets supported

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version)
- Android SDK 35
- Kotlin 1.9.0+
- Gradle 8.2+

### Building the App

1. Clone the repository:
```bash
git clone https://github.com/ajstack22/SmilePile.git
cd SmilePile
```

2. Open in Android Studio:
- File → Open → Select the `SmilePile/android` directory

3. Build and run:
- Click "Run" or use `./gradlew assembleDebug`

### Installation

1. Enable Developer Options on your Android device
2. Enable USB Debugging
3. Connect device via USB
4. Run: `adb install android/app/build/outputs/apk/debug/app-debug.apk`

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Material Design Components
- **Database**: Room Persistence Library
- **Async**: Coroutines
- **Image Loading**: Glide
- **Architecture**: MVVM Pattern

### Project Structure
```
SmilePile/
├── android/               # Android app source
│   ├── app/
│   │   ├── src/main/     # Main source code
│   │   │   ├── java/     # Kotlin source files
│   │   │   └── res/      # Resources (layouts, values, etc.)
│   │   └── build.gradle.kts
├── features/             # Feature specifications
├── bugs/                 # Bug tracking
└── .atlas/              # Development workflow tools
```

## 🔄 Current Status

### Completed Features ✅
- Basic photo gallery with categories
- Parent mode with secure access
- Photo import from device storage
- Category management (CRUD operations)
- Android 16 compatibility
- Storage permissions handling
- EXIF metadata removal

### In Development 🚧
- Photo reordering within categories
- Category cover image selection
- Enhanced animations
- Tablet-optimized layouts

### Planned Features 📋
- Backup/restore functionality
- Multiple parent profiles
- Voice narration for photos
- Drawing/annotation tools

## 🧪 Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## 🤝 Contributing

This is currently a personal project, but suggestions and feedback are welcome! Please open an issue for:
- Bug reports
- Feature requests
- General feedback

## 📄 License

This project is currently proprietary. All rights reserved.

## 👨‍💻 Development Workflow

The project uses the Atlas framework for development:
- Adversarial workflow for quality assurance
- Automated testing and validation
- Feature tracking and bug management

## 📧 Contact

For questions or support, please open an issue on GitHub.

---

**Built with ❤️ for families**