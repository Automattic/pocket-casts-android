# AGENTS.md

This file provides guidance to AI coding assistants (Claude Code, Cursor, Windsurf, etc.) when working with code in this repository.

## Common Commands

### Building
```bash
# Build debug APK (uses staging servers with .debug suffix)
./gradlew :app:assembleDebug

# Build debugProd APK (development build pointing to production servers)
./gradlew :app:assembleDebugProd

# Build prototype (minified pre-release build)
./gradlew :app:assemblePrototype

# Build release APK
./gradlew :app:assembleRelease

# Install debug build on connected device
./gradlew :app:installDebugProd
```

### Testing
```bash
# Run unit tests for the app module
./gradlew :app:testDebugUnitTest

# Run unit tests for a specific feature module
./gradlew :modules:features:search:testDebugUnitTest

# Run instrumentation tests on connected device
./gradlew :app:connectedDebugAndroidTest

# Run instrumentation tests for a specific module
./gradlew :modules:features:search:connectedDebugAndroidTest
```

### Code Quality
```bash
# Check code formatting (must pass before merge)
./gradlew spotlessCheck

# Auto-format code
./gradlew spotlessApply

# Install git hooks to run spotless on pre-commit
./gradlew installGitHooks

# Run lint on all application modules
./gradlew aggregatedLintRelease

# Run dependency analysis
./gradlew buildHealth
```

### Build Variants
- **debug**: Development with `.debug` suffix, uses staging servers (*.pocketcasts.net)
- **debugProd**: Development build pointing to production servers (*.pocketcasts.com)
- **prototype**: Minified pre-release build with ProGuard/R8
- **release**: Production release build with minification and shrinking

## Architecture Overview

### Multi-Module Structure

**Application Modules** (3):
- `app/` - Main mobile Android application
- `automotive/` - Android Automotive OS variant
- `wear/` - Wear OS variant

**Feature Modules** (`modules/features/`):
Self-contained features with UI, ViewModels, and feature-specific logic. Features depend on services but never on other features.

**Service Modules** (`modules/services/`):
Shared infrastructure and business logic. Core services include:
- `model` - Database entities and Room DAOs (122 database migrations!)
- `repositories` - Data layer abstracting servers and model (Repository pattern)
- `servers` - Network API clients (Retrofit + OkHttp)
- `compose` - Shared Compose components
- `ui` - Shared UI theming and components
- `analytics` - Analytics tracking
- `localization` - Strings and translations

**Dependency Flow**:
```
Applications (app, automotive, wear)
    ↓
Features (account, search, player, etc.)
    ↓
Services (repositories, ui, compose, analytics, etc.)
    ↓
Core Services (model, servers)
```

### Architecture Pattern: MVVM

- **ViewModels**: Annotated with `@HiltViewModel`, use `StateFlow` for state management
- **UI State Pattern**: Sealed interfaces/classes for representing UI states (e.g., `Idle`, `Loading`, `Success`, `Error`)
- **Unidirectional Data Flow**: ViewModels expose immutable state via `StateFlow`, accept actions via functions
- **Dependency Injection**: Hilt (Dagger 2) throughout the codebase
- **Reactive Programming**: Dual strategy with RxJava2 (legacy) and Coroutines/Flow (modern)

### Technology Stack

**UI**:
- **Jetpack Compose** (primary for new features) with Material (Material2)
  > Note: The codebase currently uses Material (Material2) for Compose components. Migration to Material3 is in progress. **For all new Compose UI, use Material3 components unless you are working in a module that has not yet migrated.** If unsure, check the module's dependencies or consult the team. Existing code may still use Material2 until migration is complete.
- **XML Views** (legacy)
- View Binding enabled for XML layouts

**Dependency Injection**: Hilt

**Database**: Room with extensive migration history

**Networking**: Retrofit + OkHttp + Moshi

**Media**: Media3 (ExoPlayer) + Cast Framework for Chromecast

**Image Loading**: Coil

**Async**: Coroutines + WorkManager

**Testing**:
- JUnit, Mockito, Turbine (for Flow testing)
- Compose UI Test, UIAutomator, MockWebServer

## Development Guidelines

### Technology Preferences

**Use Jetpack Compose for new UI**:
- All new screens/views must be written in Compose
- Migrating existing XML views to Compose is encouraged when updating them
- Use Material3 components from `modules/services/compose` and `modules/services/ui`

**Prefer Coroutines over RxJava**:
- New code should use Coroutines and Flow
- Convert RxJava code to Coroutines when you have a good opportunity
- Use `StateFlow` for state management in ViewModels

**Write in Kotlin**:
- All new features must be written in Kotlin
- Convert Java code to Kotlin at the first opportunity

### Code Style

- Line length: **120 characters**
- Must pass `spotlessCheck` before merge (auto-format with `spotlessApply`)
- Use **TODO** for temporary notes, never use **FIXME** (not allowed in repository)
- ktlint enforces Kotlin style with custom rules for Compose
- All warnings treated as errors in Kotlin compilation

### String Resources

- Add English strings only to `modules/services/localization`
- Translations are managed through GlotPress and pulled automatically
- Mark non-translatable strings with `translatable="false"`

### Image Resources

- Add images to `modules/services/images` only

**Prefer vector graphics (.svg) over rasterized formats when possible**:
- Scalable to any screen density without quality loss
- Smaller file size for simple graphics
- Use for icons, logos, and simple illustrations

**Prefer WebP format over PNG**:
- New images should be `.webp` format for better compression and smaller APK size
- PNG acceptable only for:
  - App icons and launcher icons (when required by Android)
  - Images requiring transparency that are very small
  - Third-party assets that cannot be modified

### Testing

- Write unit tests for ViewModels, managers, and business logic
- Use `MainCoroutineRule` for testing coroutines
- Use Turbine for testing Flows
- Shared test utilities available in `modules/services/sharedtest`
- Test files in `src/test/` for unit tests, `src/androidTest/` for instrumentation tests

### Feature Flags

The codebase uses a `FeatureFlag` system for A/B testing and gradual rollout. Check for feature flags before implementing changes to flagged features.

### Module Dependencies

- Features can depend on services, never on other features
- Service modules can depend on other services (e.g., `compose` → `ui`, `repositories` → `model`)
- Most feature modules depend on: `model`, `repositories`, `ui`, `compose`, `analytics`, `localization`
- `repositories` is the central data access layer
- Dependency analysis plugin enforces these rules (`./gradlew buildHealth`)

## Project-Specific Notes

### Database Migrations

The Room database has 122 migration versions. When modifying entities:
- Always provide a migration path
- Export schema is enabled (`modules/services/model/schemas/`)
- Test migrations thoroughly

### Build Variants and Server URLs

- `debug` uses staging servers (*.pocketcasts.net)
- `debugProd`, `prototype`, and `release` use production servers (*.pocketcasts.com)
- Server URLs are configured via `buildConfigField` in build.gradle.kts

### Analytics

Use `AnalyticsTracker` service for event tracking. Analytics are integrated with Automattic Tracks.
