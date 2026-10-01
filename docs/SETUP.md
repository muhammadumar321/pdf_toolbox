# Developer Setup Guide

This guide provides step-by-step instructions to configure your local development environment for **PDF Toolbox**.

---

## 1. System Requirements

* **Operating System:** Windows 10/11, macOS (Intel or Apple Silicon), or Linux (Ubuntu 20.04+).
* **RAM:** Minimum 8 GB (16 GB recommended for running Android emulators).
* **Disk Space:** At least 10 GB free space for Android Studio, SDK platforms, and build caches.

---

## 2. Tooling & SDK Installation

### A. Java Development Kit (JDK 17)
The project requires **JDK 17**.
* Download from [Eclipse Adoptium (Temurin 17)](https://adoptium.net/) or install via package manager:
  ```powershell
  # Windows via winget
  winget install EclipseAdoptium.Temurin.17.JDK
  ```
* Verify your Java installation:
  ```bash
  java -version
  # Expected output: openjdk version "17.0.x" ...
  ```

### B. Android Studio & Android SDK
1. Download and install **Android Studio** (Hedgehog 2023.1.1 or newer).
2. Launch Android Studio and open the **SDK Manager** (Settings > Appearance & Behavior > System Settings > Android SDK).
3. Under **SDK Platforms**, install:
   - **Android 14.0 ("UpsideDownCake") - API Level 34**
4. Under **SDK Tools**, install:
   - **Android SDK Build-Tools 34.0.0**
   - **Android SDK Command-line Tools (latest)**
   - **Android SDK Platform-Tools**
   - **Android Emulator**

---

## 3. Project Configuration

1. **Clone the repository:**
   ```bash
   git clone https://github.com/muhammadumar321/pdf_toolbox.git
   cd pdf_toolbox
   ```
2. **Switch to active development branch:**
   ```bash
   git switch dev
   ```
3. **Configure `local.properties`:**
   Create a `local.properties` file in the project root containing your Android SDK directory:
   * **Windows:**
     ```properties
     sdk.dir=C:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
     ```
   * **macOS:**
     ```properties
     sdk.dir=/Users/<YourUsername>/Library/Android/sdk
     ```
   * **Linux:**
     ```properties
     sdk.dir=/home/<YourUsername>/Android/Sdk
     ```

---

## 4. Building & Verification

Verify that Gradle can build the application cleanly:

On Windows:
```cmd
.\gradlew.bat clean test check assembleDebug
```

On macOS / Linux:
```bash
./gradlew clean test check assembleDebug
```

---

## 5. Physical Device Setup (USB Debugging)

1. On your Android phone, enable Developer Options (Settings > About phone > tap "Build number" 7 times).
2. Go to **Settings > Developer options** and toggle:
   - **USB debugging: ON**
   - **Install via USB: ON** *(required on Xiaomi/Redmi HyperOS/MIUI devices)*.
3. Connect your phone via USB and run:
   ```bash
   adb devices
   ```
4. Install and run:
   ```bash
   .\gradlew.bat assembleDebug
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   adb shell am start -n com.pdftoolbox.app/.ui.MainActivity
   ```
