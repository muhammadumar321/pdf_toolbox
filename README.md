# PDF Toolbox (Foliodex)

A modern, offline, privacy-first Android application for PDF manipulation and annotation built with Kotlin, Jetpack Architecture Components, and Apache PDFBox for Android.

---

## Overview

**PDF Toolbox** is a comprehensive utility that empowers users to view, edit, annotate, and transform PDF documents directly on-device without uploading sensitive files to cloud servers. All processing is executed locally utilizing Android's Storage Access Framework (SAF) and native PDF rendering pipelines.

---

## Features

### Document Viewing & Inspection
* **High-Performance PDF Viewer:** Multi-page rendering with memory-managed LRU page bitmap caching and synchronized mutex execution.
* **Document Metadata Extraction:** Inspect document title, author, subject, keywords, page count, and PDF specification version.
* **Recent Documents:** Fast access to recently opened documents with persistable URI permission management.

### Tool Suite
1. **Merge PDF:** Combine multiple PDF documents into a single document with interactive drag-and-drop reordering.
2. **Split PDF:** Extract specific page ranges (e.g. `1-3, 5, 7-9`) into a new PDF.
3. **Compress PDF:** Reduce document file size by downsampling and recompressing embedded image streams.
4. **Images to PDF:** Convert multiple images into a multi-page PDF document.
5. **Rotate PDF:** Adjust document page orientation (90°, 180°, 270°).
6. **Watermark PDF:** Add custom text or image watermarks across document pages.
7. **Delete Pages:** Strip unwanted pages using flexible comma-separated ranges.
8. **Extract Text:** Extract text content from documents with one-tap clipboard copy.
9. **Extract Images:** Extract and save all embedded images to a chosen output directory.
10. **Edit & Annotate PDF:** Draw, sketch, and overlay custom text annotations directly onto PDF pages.
11. **Protect PDF:** Secure documents with standard 128-bit password encryption.
12. **Unlock PDF:** Remove password restrictions from protected documents.
13. **Page Numbers:** Automatically stamp formatted page numbers (`Page X of Y`) on all pages.
14. **Rearrange Pages:** Reorder pages using custom sequence specifications (e.g. `3, 1, 2`).

### UI/UX & Theming
* **Material 3 Design:** Built with modern Material Design 3 guidelines, dynamic surface colors, and typography.
* **Dark & Light Modes:** Comprehensive theme support with system default, light, and dark options.
* **Immersive Full-Screen Modes:** Automatically hides navigation chrome on viewer and editing surfaces for distraction-free workflows.

---

## Architecture & Technology Stack

The application adheres to Android's recommended Modern App Architecture with a single-activity MVVM pattern:

```text
UI Layer (Views & ViewModels)
       ↓ LiveData / StateFlow
Domain / Business Layer (FileUtils, PermissionUtils)
       ↓ Coroutines / Mutex
Data Layer (PdfToolsRepository, RecentFilesRepository, FileRepository)
       ↓
Local Storage & Engines (PdfBox-Android, Android PdfRenderer, SAF)
```

* **Language:** Kotlin 2.0.21
* **Architecture:** MVVM (Model-View-ViewModel) + Single-Activity
* **UI Framework:** Android Material 3, ViewBinding, ConstraintLayout
* **Navigation:** Jetpack Navigation Component with SafeArgs
* **Async & Concurrency:** Kotlin Coroutines (`Dispatchers.IO`, `Dispatchers.Main`), Mutex locks
* **PDF Engine:** `com.tom-roush:pdfbox-android:2.0.27.0` & native `android.graphics.pdf.PdfRenderer`
* **Image Loading:** Coil (`io.coil-kt:coil:2.5.0`)
* **Storage Access:** Storage Access Framework (SAF) via `ActivityResultContracts`
* **Local Persistence:** JSON storage in app private `filesDir`
* **Target SDK:** 34 (Android 14) | **Min SDK:** 24 (Android 7.0) | **JVM Target:** 17

---

## Project Structure

```text
pdf_toolbox/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/pdftoolbox/app/
│   │   │   │   ├── data/
│   │   │   │   │   ├── models/           # Data models (RecentFile, PdfMetadata)
│   │   │   │   │   └── repository/       # PdfToolsRepository, RecentFilesRepository, FileRepository
│   │   │   │   ├── ui/
│   │   │   │   │   ├── base/             # Base UI classes
│   │   │   │   │   ├── home/             # Home dashboard & recent documents
│   │   │   │   │   ├── settings/         # Theme & app settings
│   │   │   │   │   ├── tools/            # 14 PDF manipulation tool fragments & ViewModels
│   │   │   │   │   ├── viewer/           # PDF viewer with thread-safe renderer & LRU cache
│   │   │   │   │   └── widgets/          # Custom drawing canvas (PdfDrawingView)
│   │   │   │   ├── utils/                # FileUtils, PermissionUtils
│   │   │   │   ├── MainActivity.kt       # Root activity managing navigation & intent filters
│   │   │   │   └── PdfToolboxApp.kt      # Application initialization & PDFBox loader
│   │   │   └── res/                      # Layouts, navigation graph, themes, drawables
│   │   └── test/                         # Unit tests (FileUtilsTest, ModelsTest)
│   └── build.gradle.kts                  # App module build configuration
├── build.gradle.kts                      # Root build configuration
├── settings.gradle.kts                   # Project repository settings
└── README.md
```

---

## Getting Started

### Prerequisites
* Android Studio Hedgehog (2023.1.1) or newer
* JDK 17 (recommended: Eclipse Temurin or Azul Zulu 17)
* Android SDK Platform 34 with Build-Tools 34.0.0

### Build & Run via Command Line

Clone the repository and switch to the development branch:
```bash
git clone https://github.com/muhammadumar321/pdf_toolbox.git
cd pdf_toolbox
git switch dev
```

Run unit tests:
```bash
./gradlew test
```

Run static analysis & lint checks:
```bash
./gradlew check
```

Build debug APK:
```bash
./gradlew assembleDebug
```
The resulting APK is located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Privacy & Security

* **Zero Cloud Transmission:** All PDF conversions, text extraction, password hashing, and renders happen purely in-process on the local Android device.
* **Scoped Storage & SAF:** Uses Android's Storage Access Framework with persistable read/write permissions. Does not require broad `MANAGE_EXTERNAL_STORAGE` permissions.
* **Safe Memory Management:** Bitmap allocations are tracked, scaled, and recycled via memory-aware LRU caches to prevent OutOfMemory (OOM) exceptions.

---

## Development & Branching Strategy

* **`main`:** Production/stable branch. Protected against direct pushes.
* **`dev`:** Active development branch. All feature work, bug fixes, testing, and refactoring occur here.

```text
feature / bugfix
       ↓
     `dev` (active development & verification)
       ↓
    testing / QA
       ↓
    `main` (stable production releases)
```

---

## License

This project is licensed under the Apache 2.0 License.
