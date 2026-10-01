# Changelog

All notable changes to the **PDF Toolbox** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.1] - 2026-10-01

### Fixed
- **Thread Safety in PDF Viewer (`ViewerFragment`):** Implemented mutex-protected rendering to prevent `IllegalStateException: Already has open page` crashes caused by concurrent page rendering calls during fast list scrolling.
- **Unhandled Exceptions on Encrypted Documents (`ViewerFragment`):** Added exception guards for `SecurityException` and file access errors when opening password-protected or inaccessible PDFs.
- **Canvas Sizing in PDF Editor (`EditPdfFragment`):** Fixed a critical layout bug where setting zero dimensions collapsed the canvas into a 1x1 pixel dot. Rebuilt `fragment_edit_pdf.xml` with a dedicated viewport container and extracted page navigation out of the horizontal bottom toolbar.
- **Persistable URI Permission Grant (`HomeFragment`):** Added `takePersistableUriPermission` when opening documents from the home screen to prevent `SecurityException` permission denials after app process restarts.
- **Launcher Icon Asset Format:** Corrected `ic_launcher.png` and `ic_launcher_round.png` which were mislabeled JPEG files that crashed AAPT2 resource merging during release builds.
- **Incomplete Tool Destinations in `MainActivity`:** Added missing destinations (`edit_pdf`, `protect`, `unlock`, `page_numbers`, `rearrange`) to bottom navigation synchronization.
- **File Name Display:** Created centralized `FileUtils.getDisplayName` to resolve actual document titles from `OpenableColumns.DISPLAY_NAME` across all 14 tool screens instead of raw internal path segments.
- **Merge Tool Loading State (`MergeFragment`):** Added progress indicator visibility management during PDF merge execution.
- **PDFBox Deprecation Warnings:** Replaced deprecated integer-based `setNonStrokingColor` calls with standard normalized float values.

### Added
- **Unit Test Suite:** Added unit test suite covering `FileUtils` range parsing, page order validation, out-of-bound filtering, and data model integrity.
- **Recent Files Capping:** Capped `RecentFilesRepository` history to 30 items to prevent unbounded JSON file growth.
- **Full-Screen Immersion:** Automatically hide bottom navigation chrome when viewing or editing documents.
- **Comprehensive Documentation:** Added full project documentation, architectural overview, contribution guidelines, and changelog.

---

## [1.0.0] - 2026-09-30

### Added
- Initial project architecture with Kotlin, MVVM, and Jetpack Navigation.
- 14 core PDF manipulation tools:
  - Merge PDF
  - Split PDF
  - Compress PDF
  - Images to PDF conversion
  - Rotate PDF
  - Watermark PDF (Text and Image overlays)
  - Delete Pages
  - Extract Text
  - Extract Images
  - Edit & Annotate PDF (drawing and text)
  - Protect PDF (128-bit encryption)
  - Unlock PDF
  - Add Page Numbers
  - Rearrange Pages
- Material 3 theme system with light and dark mode toggling.
- Integration with Apache PDFBox for Android (`pdfbox-android`).
