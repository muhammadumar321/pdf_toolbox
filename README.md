# PDF Toolbox (Foliodex)

A modern, offline, privacy-first Android application for PDF viewing, editing, annotation, and manipulation built with **Kotlin 2.0**, **Android Jetpack**, and **Apache PDFBox for Android**.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Key Features](#key-features)
- [Application Architecture](#application-architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Application Flow](#application-flow)
- [Navigation](#navigation)
- [State Management](#state-management)
- [Data & Storage](#data--storage)
- [API & Networking](#api--networking)
- [Authentication & Security](#authentication--security)
- [UI/UX & Design System](#uiux--design-system)
- [Configuration & Environment](#configuration--environment)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Running the Application](#running-the-application)
- [Testing Strategy](#testing-strategy)
- [Building & Compilation](#building--compilation)
- [Release & Deployment](#release--deployment)
- [Git Workflow & Branching Strategy](#git-workflow--branching-strategy)
- [Troubleshooting Guide](#troubleshooting-guide)
- [Known Limitations](#known-limitations)
- [Development Guidelines](#development-guidelines)
- [License](#license)

---

## Project Overview

* **Application Name:** PDF Toolbox (Package / Theme Name: `Theme.Foliodex`, Application ID: `com.pdftoolbox.app`)
* **Primary Purpose:** Provide an all-in-one, completely local mobile suite to inspect, manipulate, annotate, secure, and convert PDF documents on Android devices without relying on third-party servers.
* **Target Users:** Professionals, students, mobile workers, and privacy-conscious users handling sensitive personal, legal, or financial documents.
* **Core Philosophy:** **Zero Cloud Dependency.** All document rendering, transformations, text stripping, image extraction, and password operations execute strictly on-device inside the application process sandbox.
* **Current Status:** Production-ready v1.0.1. Fully stabilized, audited, unit-tested, and verified on Android 14 and Android 15 (Xiaomi HyperOS).

---

## Key Features

### 1. Document Viewing & Inspection
* **High-Performance PDF Viewer:** Multi-page rendering using Android's native `android.graphics.pdf.PdfRenderer`, serialized with `kotlinx.coroutines.sync.Mutex` to prevent concurrent access crashes during fast scrolling.
* **Memory-Managed LRU Cache:** In-memory `LruCache<Int, Bitmap>` dynamically sized to device RAM (up to 25 MB) with automated bitmap recycling to prevent `OutOfMemory` (OOM) errors.
* **Document Metadata Extraction:** Inspect document title, author, subject, keywords, page count, and PDF version via Apache PDFBox `PDDocumentInformation`.
* **Recent Documents Dashboard:** Instant access to recently opened documents with persistable URI grants across device reboots.

### 2. PDF Manipulation Tools (14 Tools)
1. **Merge PDF:** Combine multiple PDF documents into a single file with interactive drag-and-drop reordering (`ItemTouchHelper`).
2. **Split PDF:** Extract specific page ranges (e.g. `1-3, 5, 7-9`) into a new PDF document.
3. **Compress PDF:** Downsample and recompress embedded PDF image XObjects via JPEG compression to reduce document file size.
4. **Convert Images to PDF:** Assemble multiple JPEG/PNG images into a single multi-page PDF document respecting image aspect ratios.
5. **Rotate PDF:** Correct page orientation across the entire document by 90°, 180°, or 270°.
6. **Watermark PDF:** Apply customized text stamps (using Helvetica Bold) or image overlays across all document pages.
7. **Delete Pages:** Remove unwanted pages using flexible comma-separated single pages and page ranges.
8. **Extract Text:** Extract all textual content from documents using Apache PDFBox `PDFTextStripper` with one-tap clipboard copying.
9. **Extract Images:** Extract all embedded raster image XObjects from PDF pages and save them as standalone PNG files into a chosen folder via SAF.
10. **Edit & Annotate PDF:** Interactive canvas overlay (`PdfDrawingView`) allowing freehand sketching, drawing, and placing text annotations on any page, rasterizing the final result back onto the PDF.
11. **Protect PDF:** Encrypt documents using standard 128-bit encryption (`StandardProtectionPolicy`) with user-defined passwords.
12. **Unlock PDF:** Decrypt password-protected documents and strip security restrictions.
13. **Page Numbers:** Automatically stamp bottom-centered pagination (`Page X of Y`) on all document pages.
14. **Rearrange Pages:** Reorder pages using custom page index sequences (e.g. `3, 1, 2`).

### 3. System Integration & Deep Linking
* **External Intent Filter:** Registers for `android.intent.action.VIEW` for MIME type `application/pdf`, enabling PDF Toolbox as the default system PDF reader.
* **Theme Customization:** Comprehensive Material 3 DayNight support (Light, Dark, or System Default) persisted in `SharedPreferences`.

---

## Application Architecture

The application adheres to Google's recommended **Modern Android Architecture (MVVM)** with a single-activity architecture:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                               UI Layer                                 │
│  MainActivity ──> NavHostFragment ──> Tool & Viewer Fragments         │
│         ▲                                        │                     │
│         │ LiveData / StateFlow                   ▼                     │
│  ViewBinding ◄────────────────────────────── ViewModel                 │
└────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              Domain Layer                              │
│       FileUtils (parsing, resolution) | PermissionUtils (SAF)          │
└────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                               Data Layer                               │
│  ┌───────────────────────┐ ┌──────────────────────┐ ┌───────────────┐  │
│  │  PdfToolsRepository   │ │RecentFilesRepository │ │FileRepository │  │
│  └───────────────────────┘ └──────────────────────┘ └───────────────┘  │
│             │                         │                    │           │
│             ▼                         ▼                    ▼           │
│      PdfBox-Android / SAF     Local JSON Storage      Cache Dir        │
└────────────────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

1. **UI Layer (`com.pdftoolbox.app.ui`):**
   * **`MainActivity`:** Manages the main container, bottom navigation visibility, and incoming `ACTION_VIEW` intents.
   * **Fragments:** Purely presentation-focused. Handle user inputs, observe ViewModels, and render state changes via ViewBinding.
2. **ViewModel Layer:**
   * Holds UI state (`LiveData` and `StateFlow`), executes business logic within `viewModelScope`, and exposes immutable observables to Fragments.
3. **Domain & Utilities Layer (`com.pdftoolbox.app.utils`):**
   * **`FileUtils`:** Reusable utilities for ContentResolver display name resolution, page range parsing, and page order parsing.
   * **`PermissionUtils`:** SAF permission helpers.
4. **Data Layer (`com.pdftoolbox.app.data`):**
   * **`PdfToolsRepository`:** Coordinates Apache PDFBox operations, byte streams, transformation pipelines, and bitmap rasterization.
   * **`RecentFilesRepository`:** Manages recent documents in app-internal JSON storage (`recent_files.json`) capped at 30 items.
   * **`FileRepository`:** Handles cache copying when required for file descriptor processing.

---

## Technology Stack

| Component | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | `2.0.21` | Modern, null-safe Android development |
| **JDK** | Java / OpenJDK | `17` | Compilation target |
| **Build System** | Gradle | `8.14` | Project automation |
| **Android Plugin** | AGP | `8.9.0` | Android build tooling |
| **Compile SDK** | Android SDK | `34` (Android 14) | Platform target |
| **Min SDK** | Android SDK | `24` (Android 7.0) | Minimum supported Android OS |
| **UI Components** | Google Material 3 | `1.11.0` | Modern design system, DayNight themes |
| **Navigation** | Jetpack Navigation | `2.7.6` | Fragment routing, SafeArgs, backstack |
| **Lifecycle** | Jetpack Lifecycle | `2.7.0` | ViewModel, LiveData, runtime coroutines |
| **PDF Processing** | PdfBox-Android | `2.0.27.0` | Document manipulation, merge, encrypt |
| **PDF Rendering** | Native Android | `PdfRenderer` | Hardware-accelerated page rendering |
| **Image Loading** | Coil | `2.5.0` | Async image loading |
| **Coroutines** | Kotlinx Coroutines | `1.7.3` | Async background tasks & Mutex |
| **Storage Access** | AndroidX DocumentFile | `1.0.1` | Storage Access Framework (SAF) trees |
| **Unit Testing** | JUnit 4 | `4.13.2` | Core unit test runner |
| **Arch Testing** | AndroidX Core Testing | `2.2.0` | InstantTaskExecutorRule for LiveData |
| **Coroutines Test**| Kotlinx Coroutines Test| `1.7.3` | Test dispatchers & runTest |

---

## Project Structure

```text
pdf_toolbox/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/pdftoolbox/app/
│   │   │   │   ├── data/
│   │   │   │   │   ├── models/
│   │   │   │   │   │   ├── PdfMetadata.kt        # Document metadata data class
│   │   │   │   │   │   └── RecentFile.kt         # Recent file model with timestamp
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── FileRepository.kt     # Cache operations & name resolution
│   │   │   │   │       ├── PdfToolsRepository.kt # Core PDFBox manipulation engine
│   │   │   │   │       └── RecentFilesRepository.kt # JSON persistence (capped at 30)
│   │   │   │   ├── ui/
│   │   │   │   │   ├── base/                     # Reusable base UI classes
│   │   │   │   │   ├── home/                     # HomeFragment, HomeViewModel, RecentFilesAdapter
│   │   │   │   │   ├── settings/                 # SettingsFragment, SettingsViewModel
│   │   │   │   │   ├── tools/                    # ToolsFragment, ToolsAdapter, ToolItem
│   │   │   │   │   │   ├── compress/             # CompressFragment, CompressViewModel
│   │   │   │   │   │   ├── convert/              # ConvertFragment, ConvertViewModel
│   │   │   │   │   │   ├── delete/               # DeletePagesFragment, DeletePagesViewModel
│   │   │   │   │   │   ├── edit/                 # EditPdfFragment, EditPdfViewModel
│   │   │   │   │   │   ├── extract/              # ExtractImagesFragment, ExtractTextFragment
│   │   │   │   │   │   ├── merge/                # MergeFragment, MergeViewModel, SelectedFilesAdapter
│   │   │   │   │   │   ├── pagenumbers/          # PageNumbersFragment, PageNumbersViewModel
│   │   │   │   │   │   ├── protect/              # ProtectFragment, ProtectViewModel
│   │   │   │   │   │   ├── rearrange/            # RearrangeFragment, RearrangeViewModel
│   │   │   │   │   │   ├── rotate/               # RotateFragment, RotateViewModel
│   │   │   │   │   │   ├── split/                # SplitFragment, SplitViewModel
│   │   │   │   │   │   ├── unlock/               # UnlockFragment, UnlockViewModel
│   │   │   │   │   │   └── watermark/            # WatermarkFragment, WatermarkViewModel
│   │   │   │   │   ├── viewer/                   # ViewerFragment, PdfPageAdapter
│   │   │   │   │   └── widgets/                  # PdfDrawingView (drawing canvas)
│   │   │   │   ├── utils/
│   │   │   │   │   ├── FileUtils.kt              # Filename resolution & range parsing
│   │   │   │   │   └── PermissionUtils.kt        # SAF permission checks
│   │   │   │   ├── MainActivity.kt               # Single entry Activity & intent receiver
│   │   │   │   └── PdfToolboxApp.kt              # Application subclass & PDFBox loader
│   │   │   └── res/
│   │   │       ├── drawable/                     # PNG launcher icons, gradient backgrounds
│   │   │       ├── layout/                       # Material 3 XML layouts
│   │   │       ├── menu/                         # Bottom navigation & toolbar menus
│   │   │       ├── navigation/nav_graph.xml      # Jetpack Navigation graph
│   │   │       └── values/                       # Colors, themes, styles, strings
│   │   └── test/
│   │       └── java/com/pdftoolbox/app/
│   │           ├── FileUtilsTest.kt              # Unit tests for FileUtils
│   │           └── ModelsTest.kt                 # Unit tests for data models
│   ├── build.gradle.kts                          # App-level build script
│   └── proguard-rules.pro                        # R8/ProGuard shrinking rules
├── docs/
│   ├── ARCHITECTURE.md                           # Deep technical architecture
│   ├── SETUP.md                                  # Developer environment setup
│   ├── TROUBLESHOOTING.md                        # Common developer issues & fixes
│   └── RELEASE.md                                # Production release checklist
├── build.gradle.kts                              # Top-level build script
├── settings.gradle.kts                           # Gradle repository definitions
├── CHANGELOG.md                                  # Version release history
├── CONTRIBUTING.md                               # Contribution & Git guidelines
└── README.md                                     # Project documentation
```

---

## Application Flow

### Document Viewing Flow
```text
User selects PDF (or opens via Intent)
      ↓
HomeFragment / MainActivity
      ↓
takePersistableUriPermission (SAF)
      ↓
HomeViewModel.addRecentFile()
      ↓
Navigate to ViewerFragment(fileUri)
      ↓
PdfRenderer open (Thread-safe via Mutex)
      ↓
RecyclerView Page Adapter
      ↓
Check LRU Bitmap Cache
      ├─ Cache Hit  ──> Render cached Bitmap immediately
      └─ Cache Miss ──> Mutex.withLock { renderPage(index) } ──> Put in Cache ──> Display Bitmap
```

### Tool Transformation Flow
```text
User selects Tool (e.g. Merge / Split / Protect)
      ↓
Tool Fragment launched via NavController
      ↓
File Picker (ACTION_OPEN_DOCUMENT)
      ↓
ViewModel stores URI & resolves Display Name via FileUtils
      ↓
User defines parameters (passwords, page ranges, watermarks)
      ↓
File Creator (ACTION_CREATE_DOCUMENT) yields destination output URI
      ↓
ViewModel launches Coroutine on Dispatchers.IO
      ↓
PdfToolsRepository processes input stream(s) ──> writes to destination stream
      ↓
LiveData emits Result.success(Unit) or Result.failure(Exception)
      ↓
UI dismisses progress bar & displays Toast notification
```

---

## Navigation

* **Start Destination:** `R.id.navigation_home`
* **Navigation Graph:** `app/src/main/res/navigation/nav_graph.xml`
* **Bottom Navigation Bar:** Configured with `BottomNavigationView.setupWithNavController`:
  * Tab 1: **Home** (`R.id.navigation_home`)
  * Tab 2: **Tools** (`R.id.navigation_tools`)
  * Tab 3: **Settings** (`R.id.navigation_settings`)
* **Full-Screen Immersion:** `MainActivity` listens for destination changes and automatically hides `BottomNavigationView` on full-screen screens (`navigation_viewer` and `navigation_edit_pdf`).
* **Deep Linking / Intent Filter:**
  ```xml
  <intent-filter>
      <action android:name="android.intent.action.VIEW" />
      <category android:name="android.intent.category.DEFAULT" />
      <category android:name="android.intent.category.BROWSABLE" />
      <data android:scheme="file" />
      <data android:scheme="content" />
      <data android:mimeType="application/pdf" />
  </intent-filter>
  ```

---

## State Management

* **Architecture:** Google Jetpack `ViewModel` combined with `LiveData` and `StateFlow`.
* **State Ownership:** ViewModels retain all operational state (selected URIs, processing states, page counts) surviving Activity/Fragment recreation.
* **Loading Indicators:** Handled reactively. `ProgressBar` visibility is toggled by observing ViewModel states.
* **Error Handling:** Background exceptions are caught in coroutine `try-catch` blocks and returned as standard Kotlin `Result<T>` objects, preventing unhandled runtime exceptions.

---

## Data & Storage

* **Storage Access Framework (SAF):**
  * Reads documents via `Intent.ACTION_OPEN_DOCUMENT`.
  * Writes documents via `Intent.ACTION_CREATE_DOCUMENT`.
  * Selects output directories via `Intent.ACTION_OPEN_DOCUMENT_TREE`.
* **Persistable Permissions:**
  Persistable URI permissions are captured using:
  ```kotlin
  context.contentResolver.takePersistableUriPermission(
      uri,
      Intent.FLAG_GRANT_READ_URI_PERMISSION
  )
  ```
* **Recent Documents Store:**
  * Stored in `context.filesDir/recent_files.json`.
  * Structure: `[{"uri": "...", "name": "...", "timestamp": 123456789}]`.
  * History is capped at `MAX_RECENT_FILES = 30` to prevent memory and disk bloat.
* **Cache Management:**
  * Temporary files are isolated in `context.cacheDir`.

---

## API & Networking

* **100% Offline-First Architecture.**
* The app contains **no remote endpoints, no tracking SDKs, and no cloud analytics**.
* All PDF manipulations occur in-memory or on local disk streams.

---

## Authentication & Security

* **Document Encryption:**
  Implements standard PDF 128-bit encryption via Apache PDFBox `StandardProtectionPolicy` and `AccessPermission`.
* **Scoped Storage Compliance:**
  Complies with modern Android storage models (API 24 to 34+). Does not request or require broad `MANAGE_EXTERNAL_STORAGE` permissions.
* **ProGuard / R8 Obfuscation:**
  Minification is enabled in release builds with custom rules in `app/proguard-rules.pro` protecting PDFBox font resources and JNI bindings.

---

## UI/UX & Design System

* **Material Design 3:** Built using Google Material 3 (`Theme.Material3.DayNight.NoActionBar`).
* **Theme Modes:**
  * Light Mode
  * Dark Mode
  * System Default
* **Color Palette:**
  * Primary: `@color/md_theme_light_primary` (`#5A31E1` / Deep Purple)
  * Secondary: `@color/md_theme_light_secondary`
  * Error: `@color/md_theme_light_error` (`#BA1A1A`)
  * Surface: `@color/md_theme_light_surface`
* **Responsive Layouts:** Uses `ConstraintLayout` and `FrameLayout` viewports with dynamic aspect-ratio scaling for phones and tablets.

---

## Configuration & Environment

| Property | File | Value |
| :--- | :--- | :--- |
| **Android SDK Path** | `local.properties` | `sdk.dir=C:\Users\<user>\AppData\Local\Android\Sdk` |
| **JVM Target** | `app/build.gradle.kts` | `JavaVersion.VERSION_17` |
| **Min SDK** | `app/build.gradle.kts` | `24` (Android 7.0) |
| **Compile / Target SDK**| `app/build.gradle.kts` | `34` (Android 14) |
| **Version Code** | `app/build.gradle.kts` | `1` |
| **Version Name** | `app/build.gradle.kts` | `1.0.1` |

---

## Prerequisites

* **Operating System:** Windows, macOS, or Linux.
* **Java Development Kit:** JDK 17 (recommended: Eclipse Temurin or Azul Zulu 17).
* **Android Studio:** Android Studio Hedgehog (2023.1.1) or newer.
* **Android SDK:** Platforms 34, Android SDK Build-Tools 34.0.0, Android SDK Platform-Tools (with `adb`).

---

## Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/muhammadumar321/pdf_toolbox.git
   cd pdf_toolbox
   ```
2. **Switch to the active development branch:**
   ```bash
   git switch dev
   ```
3. **Verify Gradle Wrapper:**
   On Windows:
   ```cmd
   .\gradlew.bat --version
   ```
   On macOS/Linux:
   ```bash
   ./gradlew --version
   ```

---

## Running the Application

### Via Command Line (ADB)

1. Connect your Android device via USB and enable USB Debugging.
2. Verify connection:
   ```bash
   adb devices
   ```
3. Build and install the debug APK:
   ```bash
   .\gradlew.bat assembleDebug
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```
4. Launch the application:
   ```bash
   adb shell am start -n com.pdftoolbox.app/.ui.MainActivity
   ```

*(On Xiaomi HyperOS / MIUI devices, ensure **Install via USB** is toggled ON under Developer Options).*

---

## Testing Strategy

The project includes automated unit testing covering data parsing, boundaries, and models.

Run unit tests:
```bash
.\gradlew.bat test
```
HTML test reports are generated at:
```text
app/build/reports/tests/testDebugUnitTest/index.html
```

Run static analysis & Android Lint checks:
```bash
.\gradlew.bat check
```
Lint reports are generated at:
```text
app/build/reports/lint-results-debug.html
```

---

## Building & Compilation

### Debug Build
```bash
.\gradlew.bat assembleDebug
```
Output artifact: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
```bash
.\gradlew.bat assembleRelease
```
Output artifact: `app/build/outputs/apk/release/app-release-unsigned.apk`

### Android App Bundle (AAB for Google Play)
```bash
.\gradlew.bat bundleRelease
```
Output artifact: `app/build/outputs/bundle/release/app-release.aab`

---

## Release & Deployment

1. **Version Increment:** Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. **Keystore Configuration:** Configure release signing in `app/build.gradle.kts` using environment variables or a secure properties file.
3. **Run Full Verification:**
   ```bash
   .\gradlew.bat clean test check assembleRelease
   ```
4. **Refer to [docs/RELEASE.md](docs/RELEASE.md)** for detailed store deployment checklists.

---

## Git Workflow & Branching Strategy

The repository operates on a two-tier branching model:

```text
feature / bugfix branch
          ↓
        `dev` (active development & QA verification)
          ↓
        `main` (production / stable release)
```

* **`dev` (Active Branch):** All active development, bug fixes, refactoring, and automated testing are committed to `dev`.
* **`main` (Production Branch):** Reserved exclusively for verified releases. Direct commits to `main` are restricted.
* **Commit Conventions:** Follow [Conventional Commits](https://www.conventionalcommits.org/):
  * `feat: ...` for new capabilities
  * `fix: ...` for bug fixes
  * `docs: ...` for documentation
  * `test: ...` for test additions

---

## Troubleshooting Guide

For comprehensive troubleshooting steps, see [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

### Quick Solutions:
* **`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user` (Xiaomi/Redmi):**
  Open phone **Settings > Additional settings > Developer options** and toggle **Install via USB** to **ON**.
* **`AAPT: error: file failed to compile`:**
  Occurs when image files are renamed to `.png` without true PNG headers. Ensure all drawable assets have standard PNG byte headers (`89 50 4E 47`).
* **`IllegalStateException: Already has open page`:**
  Resolved in v1.0.1 via `renderMutex` in `ViewerFragment`. Ensure all calls to `pdfRenderer` are synchronized.
* **`SecurityException: Permission Denial` on Recent Files:**
  Resolved in v1.0.1 by invoking `takePersistableUriPermission` on file pick.

---

## Known Limitations

1. **Massive File Sizes (>500 MB):** Very large files may experience slight rendering delays on lower-end devices with constrained RAM.
2. **Complex AcroForms:** Certain proprietary interactive XFA form fields are rendered statically by PDFBox.
3. **Platform:** Native Android only (APIs 24 through 34+). iOS is not supported.

---

## Development Guidelines

* **Keep Views Dumb:** Keep Fragments clean of business logic; place state and transformations in ViewModels.
* **Dispatchers:** Always offload disk and PDF operations to `Dispatchers.IO`. Never perform PDFBox operations on the main thread.
* **Threading Guards:** Never bypass `renderMutex` when interacting with Android's native `PdfRenderer`.
* **Display Names:** Always resolve user-facing document names using `FileUtils.getDisplayName(context, uri)`.

---

## License

This project is licensed under the **Apache License 2.0** - see the LICENSE file for details.
