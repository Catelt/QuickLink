# QuickLink

QuickLink is an Android app built with Kotlin and Jetpack Compose that helps you **open, share, and manage links quickly**.
It supports deep links, QR codes (generate & scan), and simple file downloads from URLs.

[**Get it on Google Play**](https://play.google.com/store/apps/details?id=com.catelt.quicklink)

## Features

- **Deep link opener**
  - Paste or type a URL and open it with the appropriate app or browser.
  - Automatically validates the URL and shows a toast if it cannot be handled.
- **Copy & share**
  - Copy links to the clipboard with one tap.
  - Share text/links using the Android share sheet.
- **QR code generator**
  - Generate a QR code from a URL or text.
  - Designed to be easily scannable on other devices.
- **QR code scanner**
  - Scan QR codes using the camera.
  - Extract the embedded link/text and open, copy, or share it.
- **Download file from URL**
  - Simple screen to download a file from a user‑provided URL.
- **Modern UI**
  - Jetpack Compose UI with Material 3 components.
  - Navigation drawer to switch between Deep Link, QR Code, Scan QR, and Download File screens.

## Tech stack

- **Language**: Kotlin
- **UI**: Jetpack Compose, Material 3
- **Architecture**: ViewModel + state/events (`QuickLinkViewModel`, `QuickLinkState`, `QuickLinkEvent`)
- **Modules**:
  - `app`: main Android application
  - `feature:component`, `feature:downloadfile`, `feature:sendfile`: feature modules used by the app
- **Build system**: Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`)

## Getting started

### Prerequisites

- **Android Studio** (Giraffe or newer recommended)
- **JDK 17** (or the version bundled with your Android Studio)
- **Android SDK** and build tools installed

### Clone the project

```bash
git clone <your-repo-url>
cd QuickLink
```

### Open in Android Studio

1. Open Android Studio.
2. Choose **Open an existing project**.
3. Select the `QuickLink` project directory.
4. Let Gradle sync and index the project.

### Run the app

1. Connect an Android device or start an emulator (API 24+ recommended).
2. Select the `app` configuration.
3. Click **Run**.