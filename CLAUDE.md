# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SubControl is a privacy-focused Android subscription manager app. The app helps users track paid and trial subscriptions locally on their device without any cloud services or analytics.

## Key Architecture Decisions

- **Clean Architecture** with MVVM pattern
- **Jetpack Compose** for UI with Material Design 3
- **Proto DataStore** for encrypted local storage
- **Hilt** for dependency injection
- **Kotlin Coroutines + Flow** for reactive programming
- **No cloud services** - all data stored locally

## Development Commands

```bash
# Build
./gradlew assembleDebug           # Build debug APK
./gradlew bundleRelease           # Build release bundle
./gradlew assembleRelease         # Build release APK

# Testing
./gradlew testDebugUnitTest       # Run unit tests
./gradlew connectedDebugAndroidTest # Run instrumented tests
./gradlew test                    # Run all tests

# Code Quality
./gradlew lint                    # Android lint
./gradlew ktlintCheck            # Kotlin lint
./gradlew detekt                 # Static analysis
./gradlew lint ktlintCheck detekt # Run all checks

# Format Code
./gradlew ktlintFormat           # Auto-format Kotlin code
```

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/subcontrol/
│   │   │   ├── data/           # Repository, DataStore, Proto schemas
│   │   │   ├── domain/         # Use cases, models, business logic
│   │   │   ├── ui/             # Compose UI, ViewModels, navigation
│   │   │   └── di/             # Hilt modules
│   │   └── proto/              # Protocol Buffer definitions
│   └── test/                   # Unit tests
└── build.gradle.kts
```

## Key Technical Constraints

1. **Minimum Android Version**: API 35 (Android 15)
2. **Privacy-First**: No analytics, ads, or cloud services
3. **Encryption**: ChaCha20-Poly1305 for backup/restore
4. **Testing**: Maintain 90% code coverage
5. **Accessibility**: WCAG 2.1 AA compliance required

## Data Model

Subscriptions are stored using Protocol Buffers with the following key fields:
- Basic info: name, cost, currency, billing period
- Categories and tags for organization
- Renewal dates and notification settings
- Trial period tracking
- Custom notes

## Development Workflow

1. Create feature branch from main
2. Implement with TDD approach
3. Ensure all quality checks pass
4. Submit PR with comprehensive tests
5. GitHub Actions will run CI pipeline

## Important Implementation Notes

- Use `EncryptedProtobufSerializer` for all Proto DataStore implementations
- Follow Material Design 3 guidelines for all UI components
- Implement proper error handling with user-friendly messages
- Support both light and dark themes
- Ensure all strings are externalized for localization

## 🔄 Project Awareness & Context

- **Always read this `CLAUDE.md`** at the start of a new conversation to understand the project's architecture, goals, and constraints
- **Check `TASK.md`** before starting a new task. If the task isn't listed, add it with a brief description and today's date
- **Use consistent naming conventions, file structure, and architecture patterns** as described in this document
- **Follow the existing package structure** when adding new features or modules

## 🧱 Code Structure & Modularity

- **Never create a file longer than 500 lines of code.** If a file approaches this limit, refactor by splitting it into smaller, focused classes
- **Organize code into clearly separated modules**, grouped by feature or layer:
  - `data/` - Repository implementations, DataStore, Proto schemas
  - `domain/` - Use cases, domain models, business logic interfaces
  - `ui/` - Compose UI components, ViewModels, navigation
  - `di/` - Hilt modules and dependency injection setup
- **Each feature should have**:
  - `FeatureViewModel.kt` - ViewModel with UI state management
  - `FeatureScreen.kt` - Compose UI implementation
  - `FeatureRepository.kt` - Data layer implementation
  - `FeatureUseCase.kt` - Business logic (when needed)
- **Use consistent package imports** (prefer explicit imports over wildcards)

## 🧪 Testing & Reliability

- **Always create unit tests for new features** (ViewModels, repositories, use cases)
- **After updating any logic**, check whether existing unit tests need to be updated
- **Test structure should mirror the main source structure**:
  ```
  app/src/test/java/com/subcontrol/
  ├── data/           # Repository tests
  ├── domain/         # Use case tests
  ├── ui/             # ViewModel tests
  └── utils/          # Utility function tests
  ```
- **Each test class should include**:
  - At least 1 test for expected behavior
  - Edge case tests (empty data, null values)
  - Error/failure case tests
  - Use `MockK` for mocking dependencies
  - Use `Turbine` for testing Flows
- **UI tests** should be in `androidTest/` using Compose testing APIs

## ✅ Task Management

- **Create or update `TASK.md`** at the start of development if it doesn't exist
- **Mark completed tasks in `TASK.md`** immediately after finishing them
- **Add new sub-tasks or TODOs** discovered during development under a "Discovered During Work" section
- **Include dates** when adding or completing tasks for tracking progress

## 📎 Style & Conventions

- **Use Kotlin** as the primary language with idiomatic patterns
- **Follow Kotlin coding conventions** and Android best practices
- **Use Ktlint** for code formatting (run `./gradlew ktlintFormat`)
- **Apply these patterns**:
  - Immutable data classes for models
  - Sealed classes for state representation
  - Extension functions for cleaner APIs
  - Coroutines for async operations
  - StateFlow for UI state
  - Flow for data streams
- **Write KDoc comments for public APIs**:
  ```kotlin
  /**
   * Calculates the total monthly cost of all active subscriptions.
   *
   * @param subscriptions List of user subscriptions
   * @param currency Target currency for conversion
   * @return Total monthly cost in the specified currency
   */
  fun calculateMonthlyCost(
      subscriptions: List<Subscription>,
      currency: Currency
  ): BigDecimal
  ```

## 📚 Documentation & Explainability

- **Update `README.md`** when new features are added or setup steps change
- **Comment complex business logic** with `// Explanation:` comments
- **Document architectural decisions** in code when deviating from standard patterns
- **Keep inline documentation concise** but helpful for future developers
- **Update this `CLAUDE.md`** if new patterns or practices are established

## 🧠 AI Behavior Rules

- **Never assume missing context** - Ask questions if requirements are unclear
- **Never hallucinate libraries or APIs** - Only use verified Android/Kotlin libraries
- **Always verify file paths and package names** exist before referencing them
- **Never delete or overwrite existing code** unless explicitly instructed or fixing bugs
- **Check for existing implementations** before creating new files or functions
- **Respect the privacy-first approach** - Never add analytics, ads, or cloud services
- **Follow TDD when possible** - Write tests first for new features