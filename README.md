# Headache Tracker 🧠

A privacy-focused Android app to track headaches and correlate them with contextual triggers (sleep, weather, hydration) using a configurable Wizard.

## Getting Started

### 1. Requirements
* Android Studio Ladybug or newer.
* JDK 17+.

### 2. Import Steps
1.  Open Android Studio.
2.  Select "Open" and select the folder containing `build.gradle.kts` (Project).
3.  Allow Gradle to sync.
4.  Run on an Emulator (API 34 recommended).

### 3. Architecture
* **MVVM:** ViewModels manage state and interact with UseCases/Repositories.
* **Clean Architecture:**
    * `domain`: Pure Kotlin logic (Analytics, Models).
    * `data`: Room DB, JSON parsing.
    * `ui`: Jetpack Compose screens.
* **DI:** Hilt is used for dependency injection.

### 4. Features
* **Wizard:** Defined in `assets/wizard.json`. To change questions, edit this file and reinstall the app.
* **Analytics:** Go to the "Stats" tab. The app calculates Pearson correlation for numeric inputs (Sleep vs Severity) and Severity Differences for Boolean inputs.
* **Seeding:** On the very first run, the app seeds dummy data so the charts aren't empty.

### 5. Testing
* Run Unit Tests: Right-click `com.ap0n.headache (test)` -> Run Tests.
* Focus: `AnalyticsTest.kt` verifies the math behind correlations.

## Developer Notes
* **Export:** Currently supports in-app viewing. CSV export logic is ready in `MainViewModel` but needs a UI button.
* **Sync:** Currently offline-only (Room).