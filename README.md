# SubControl - Privacy-First Subscription Manager

[![Android](https://img.shields.io/badge/Android-15%2B-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A privacy-focused Android subscription manager that helps you track paid and trial subscriptions locally on your device without any cloud services or analytics.

## 🎯 Features

### ✅ Implemented (Phase 1 & 2)
- **Subscription Management**: Add, edit, and delete subscriptions with full validation
- **Cost Analytics**: Monthly and yearly cost calculations with category breakdowns
- **Dashboard**: Real-time cost summaries and upcoming renewal notifications
- **Notification System**: Smart reminders using AlarmManager with WorkManager fallback
- **Material Design 3**: Modern UI with dynamic colors and accessibility support
- **Offline First**: All data stored locally with AES-256-GCM encryption
- **Clean Architecture**: Testable, maintainable code with 100% test coverage

### 🚧 Coming Soon (Phase 3)
- **Backup/Restore**: Encrypted export/import functionality
- **Localization**: Multi-language support (EN, IT, ES, DE, FR)
- **Accessibility**: WCAG 2.1 AA compliance
- **CI/CD Pipeline**: Automated testing and deployment

## 🏗️ Architecture

SubControl follows Clean Architecture principles with clear separation of concerns:

```
📱 UI Layer (Compose)
├── 🎨 Material Design 3 Components
├── 📊 Dashboard with Analytics
├── 🔔 Notification Management
└── 🧭 Type-safe Navigation

💼 Domain Layer
├── 🎯 Use Cases (Business Logic)
├── 📋 Repository Interfaces
└── 🏗️ Domain Models

💾 Data Layer
├── 🔒 Encrypted Proto DataStore
├── 📦 Repository Implementations
└── 🔐 AES-256-GCM Encryption
```

## 🛠️ Technology Stack

- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt (Dagger)
- **Storage**: Proto DataStore with AES-256-GCM encryption
- **Async**: Coroutines + Flow
- **Testing**: JUnit, MockK, Turbine
- **Build**: Gradle 8.7 with Kotlin DSL

## 📊 Current Progress

**Overall Progress**: 50% (18/36 tasks completed)

### Phase 1 ✅ Complete
- Foundation architecture and infrastructure
- Core UI components and navigation
- Subscription management functionality

### Phase 2 ✅ Complete
- Business logic and use cases
- Dashboard UI with cost analytics
- Notification scheduling system

### Phase 3 🚧 Upcoming
- Advanced features and localization
- Accessibility improvements
- CI/CD pipeline setup

## 🧪 Testing

SubControl maintains 100% test coverage for business logic with comprehensive testing:

- **77 Unit Tests** covering all use cases, ViewModels, and business logic
- **MockK** for dependency mocking
- **Turbine** for Flow testing
- **Coroutines Test** for async testing

```bash
./gradlew testDebugUnitTest
```

## 🔐 Security & Privacy

- **Local-First**: All data stored locally on device
- **AES-256-GCM Encryption**: Hardware-backed encryption via Android Keystore
- **No Analytics**: Zero tracking or data collection
- **No Network**: App operates completely offline
- **Input Validation**: Comprehensive validation for all user inputs

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or later
- Android SDK 35 (Android 15)
- Java 17 or later

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/subcontrol.git
   cd subcontrol
   ```

2. Open in Android Studio

3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```

## 📱 Screenshots

*Screenshots coming soon - UI implementation complete*

## 🧑‍💻 Development

### Code Quality
- **KtLint**: Code formatting
- **Detekt**: Static analysis
- **Android Lint**: Android-specific checks

```bash
./gradlew ktlintCheck detekt lint
```

### Architecture Guidelines
- Follow Clean Architecture principles
- Use dependency injection with Hilt
- Implement reactive programming with Flow
- Maintain comprehensive test coverage
- Follow Material Design 3 guidelines

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please read our contributing guidelines and submit pull requests for any improvements.

## 📞 Support

For support or questions, please open an issue on GitHub.

---

**SubControl** - Take control of your subscriptions, privately.