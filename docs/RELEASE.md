# Release & Deployment Guide

This guide details the end-to-end process for preparing, signing, building, and publishing production releases of **PDF Toolbox**.

---

## Table of Contents

- [1. Release Principles](#1-release-principles)
- [2. Versioning Strategy](#2-versioning-strategy)
- [3. Code Signing Configuration](#3-code-signing-configuration)
  - [Generate a Production Keystore](#generate-a-production-keystore)
  - [Configure Gradle Signing](#configure-gradle-signing)
- [4. R8 / ProGuard Optimization](#4-r8--proguard-optimization)
- [5. Pre-Release Quality Checklist](#5-pre-release-quality-checklist)
- [6. Building Release Artifacts](#6-building-release-artifacts)
  - [Build Android App Bundle (.aab)](#build-android-app-bundle-aab)
  - [Build Universal Release APK (.apk)](#build-universal-release-apk-apk)
- [7. Publishing to Google Play Console](#7-publishing-to-google-play-console)
- [8. Post-Release Git Procedures](#8-post-release-git-procedures)

---

## 1. Release Principles

* **Release from `main` Only:** Production releases are tagged and built exclusively from `main` after complete stabilization and testing on `dev`.
* **Zero Secrets in Version Control:** Never commit `.jks`, `.keystore`, keystore passwords, or alias secrets to Git.
* **Deterministic Builds:** Ensure clean compilation and test verification before generating production artifacts.

---

## 2. Versioning Strategy

PDF Toolbox follows [Semantic Versioning (SemVer)](https://semver.org/):

$$\text{Version Format: } \text{MAJOR}.\text{MINOR}.\text{PATCH}$$

* **MAJOR:** Significant architectural changes or major redesigns.
* **MINOR:** New PDF tools, new UI features, or substantial enhancements.
* **PATCH:** Bug fixes, performance optimizations, or security updates.

Update versions in `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        versionCode = 2       // Monotonically increasing integer for Google Play
        versionName = "1.0.2" // Human-readable release version
    }
}
```

---

## 3. Code Signing Configuration

### Generate a Production Keystore

If you do not already have a release keystore, generate one using the Java `keytool` utility:

```bash
keytool -genkey -v -keystore release-key.jks -alias pdftoolbox-key -keyalg RSA -keysize 2048 -validity 10000
```

> [!CAUTION]
> Back up your `release-key.jks` file securely in an encrypted vault. If you lose this keystore, you will **not** be able to update your application on Google Play.

### Configure Gradle Signing

Store signing secrets in an untracked environment file or `~/.gradle/gradle.properties`:

```properties
PDFTOOLBOX_KEYSTORE_FILE=/path/to/release-key.jks
PDFTOOLBOX_KEYSTORE_PASSWORD=your_store_password
PDFTOOLBOX_KEY_ALIAS=pdftoolbox-key
PDFTOOLBOX_KEY_PASSWORD=your_key_password
```

In `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("PDFTOOLBOX_KEYSTORE_FILE") ?: "release-key.jks")
            storePassword = project.findProperty("PDFTOOLBOX_KEYSTORE_PASSWORD") as String?
            keyAlias = project.findProperty("PDFTOOLBOX_KEY_ALIAS") as String?
            keyPassword = project.findProperty("PDFTOOLBOX_KEY_PASSWORD") as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## 4. R8 / ProGuard Optimization

Release builds enable R8 code shrinking, optimization, and resource shrinking:

1. **Verify `app/proguard-rules.pro` Rules:**
   Ensure PDFBox reflection, fonts, and Coroutines rules are preserved:
   ```proguard
   -keep class com.tom_roush.pdfbox.** { *; }
   -keepclassmembers class * {
       @android.webkit.JavascriptInterface <methods>;
   }
   -dontwarn com.tom_roush.pdfbox.**
   -dontwarn org.bouncycastle.**
   ```
2. **Test Release Minification Locally:**
   Always test an obfuscated build on a physical device before deployment:
   ```bash
   .\gradlew.bat assembleRelease
   adb install -r app\build\outputs\apk\release\app-release.apk
   ```

---

## 5. Pre-Release Quality Checklist

Before building final release artifacts, execute the full validation suite:

- [ ] **Static Code Analysis:** Run lint and static checks:
  ```bash
  .\gradlew.bat check
  ```
- [ ] **Unit Tests:** All unit test suites pass:
  ```bash
  .\gradlew.bat test
  ```
- [ ] **14 Tool Smoke Tests:** Verify key tools on a physical device:
  - [ ] Merge PDF (2+ documents)
  - [ ] Split PDF (extract page subset)
  - [ ] Compress PDF (verify reduced file size)
  - [ ] Protect PDF (encrypt with password)
  - [ ] Unlock PDF (decrypt protected document)
  - [ ] Edit & Annotate (canvas drawing and save)
- [ ] **SAF Persistable Grants:** Verify recent files persist across app relaunch.
- [ ] **Theme Switching:** Verify Material 3 DayNight mode transitions cleanly.
- [ ] **Backstack & Navigation:** Verify back button behavior from all tool fragments.

---

## 6. Building Release Artifacts

### Build Android App Bundle (.aab)

Google Play Store requires an **Android App Bundle (AAB)** for new submissions:

On Windows:
```cmd
.\gradlew.bat bundleRelease
```

On macOS / Linux:
```bash
./gradlew bundleRelease
```

* Output location: `app/build/outputs/bundle/release/app-release.aab`

### Build Universal Release APK (.apk)

For direct GitHub Releases, enterprise distribution, or local QA testing:

On Windows:
```cmd
.\gradlew.bat assembleRelease
```

On macOS / Linux:
```bash
./gradlew assembleRelease
```

* Output location: `app/build/outputs/apk/release/app-release.apk`

---

## 7. Publishing to Google Play Console

1. Navigate to the [Google Play Console](https://play.google.com/console).
2. Select **PDF Toolbox** application.
3. Under **Production** (or **Testing > Internal testing**), create a new release.
4. Upload `app-release.aab`.
5. Enter Release Notes in `en-US` and target locales.
6. Verify **Data Safety Form**:
   * *Data collected:* None.
   * *Data shared:* None.
   * *Storage rationale:* PDF processing executes entirely on-device via Storage Access Framework (SAF).
7. Review and rollout the release.

---

## 8. Post-Release Git Procedures

After completing and validating a release:

1. **Merge `dev` into `main`:**
   ```bash
   git switch main
   git merge dev --ff-only
   ```
2. **Tag the Release:**
   ```bash
   git tag -a v1.0.1 -m "Release version 1.0.1"
   ```
3. **Push to Remote:**
   ```bash
   git push origin main
   git push origin v1.0.1
   ```
4. **Switch back to `dev` for active development:**
   ```bash
   git switch dev
   ```
