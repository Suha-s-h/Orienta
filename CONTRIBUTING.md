# Contributing to Orienta

Thanks for your interest in contributing to **Orienta**! As an open-source Android utility built with Kotlin and Jetpack Compose, community bug reports, OEM compatibility fixes, translations, and feature improvements are always welcome.

---

## Development Setup

### Prerequisites
- **JDK 17** (Temurin or OpenJDK 17)
- **Android Studio** (Meerkat / Ladybug or newer) or Android SDK Command-Line Tools
- **Android SDK Platform 36** (`compileSdk` 36.1 / `targetSdk` 36, `minSdk` 24)
- **Git**

### Getting Started
1. Fork this repository and clone your fork:
   ```bash
   git clone https://github.com/<your-username>/Orienta.git
   cd Orienta
   ```
2. Open the project in Android Studio (wait for Gradle sync) or build from the terminal:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## Project Conventions

- **Code Style:** Follow standard Kotlin & Jetpack Compose idioms. Keep functions concise, single-purpose, and battery-aware (avoid polling loops or unnecessary wakeups in background services).
- **Permissions:** Orienta prides itself on running **100% offline with zero root and zero `INTERNET` permission**. PRs introducing network permissions, analytics SDKs, or invasive tracking will not be accepted.
- **Comments & Docs:** Explain *why* non-obvious Android system flags, OEM workarounds, or `WindowManager` hacks are needed.
- **Keep Secrets Out of Git:** Never commit `.env`, `*.jks`, `*.keystore`, `local.properties`, or compiled `*.apk` files.

---

## Pull Request Process

1. Create a descriptive feature branch from `main` (e.g., `fix/xiaomi-floating-snap` or `feat/tile-vibration-toggle`).
2. Test your changes on a physical device or emulator—especially permission toggles, notification controls, and accessibility auto-switching.
3. Ensure `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass cleanly.
4. Open a Pull Request using the PR template and link any relevant issue.

---

## Reporting OEM Quirks

Android manufacturers (Samsung One UI, Xiaomi HyperOS/MIUI, Nothing OS, ColorOS, Pixel Stock) sometimes handle `SYSTEM_ALERT_WINDOW`, background foreground-service restrictions, or display cutouts differently. When reporting a bug, please include your device model, Android version, and OEM skin so we can reproduce and fix it quickly!
