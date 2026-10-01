# System Architecture & Technical Specifications

This document outlines the architecture, data flow, memory model, and security posture of the **PDF Toolbox** Android application.

---

## 1. Architectural Overview

PDF Toolbox implements the **Single-Activity Architecture** pattern using Android Jetpack Components:

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

---

## 2. Key Components

### UI Layer
* **MainActivity:** Single entry point managing window insets, navigation bar state, and external `ACTION_VIEW` intents.
* **Fragments:** 14 feature tool fragments, `HomeFragment`, `SettingsFragment`, and `ViewerFragment`.
* **ViewModels:** Own and maintain UI state, decoupling business operations from Android lifecycle events.

### Data Layer
* **`PdfToolsRepository`:**
  - Interfaces with Apache PDFBox (`com.tom_roush.pdfbox`) to execute complex document manipulations (merge, split, watermark, protect, compress).
  - Handles image stream extraction and font embedding.
  - All operations run off the main thread on `Dispatchers.IO`.
* **`RecentFilesRepository`:**
  - Manages recently accessed document references in app-internal JSON storage (`recent_files.json`).
  - Limits entries to `MAX_RECENT_FILES = 30` to maintain performance.
* **`FileRepository`:**
  - Facilitates safe copying of input streams to temporary cache files when required by third-party processors.

---

## 3. Concurrency & Rendering Pipeline

### Android `PdfRenderer` Thread-Safety
Android's native `android.graphics.pdf.PdfRenderer` is explicitly non-thread-safe:
* Multiple concurrent calls to `openPage()` or `render()` cause `IllegalStateException: Already has open page`.
* `ViewerFragment` introduces a dedicated `kotlinx.coroutines.sync.Mutex` (`renderMutex`).
* Every call to open, render, and close pages is serialized through `renderMutex.withLock`.

### Memory Management & LRU Caching
* PDF page bitmaps are cached using an in-memory `LruCache<Int, Bitmap>`.
* Cache size is calculated dynamically:
  $$\text{CacheSize} = \min\left(\frac{\text{Runtime Max Memory}}{32}, 25\,\text{MB}\right)$$
* Evicted entries explicitly invoke `Bitmap.recycle()` to avoid heap fragmentation and prevent OutOfMemory (OOM) errors.

---

## 4. Security & Storage Access Model

* **Scoped Storage & SAF:**
  The app does not require `MANAGE_EXTERNAL_STORAGE` or legacy read permissions on modern Android versions. It interacts with the filesystem exclusively via:
  - `Intent.ACTION_OPEN_DOCUMENT` (for single or multiple document picking)
  - `Intent.ACTION_CREATE_DOCUMENT` (for target output file creation)
  - `Intent.ACTION_OPEN_DOCUMENT_TREE` (for batch folder output, e.g., image extraction)
* **Persistable Permissions:**
  Persistable URI permissions (`takePersistableUriPermission`) are acquired so that documents saved to recent files can be opened across application lifecycles.
* **Encryption Standards:**
  Password protection utilizes PDF standard 128-bit security policies with access permissions configured via `StandardProtectionPolicy`.
