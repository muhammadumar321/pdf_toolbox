# Troubleshooting Guide

This guide covers common development, build, runtime, and device installation issues encountered in **PDF Toolbox** and how to resolve them.

---

## Table of Contents

- [1. Device Installation Issues](#1-device-installation-issues)
  - [INSTALL_FAILED_USER_RESTRICTED (Xiaomi / Redmi / HyperOS / MIUI)](#install_failed_user_restricted-xiaomi--redmi--hyperos--miui)
  - [Device Unauthorized (`adb devices` shows `unauthorized`)](#device-unauthorized-adb-devices-shows-unauthorized)
- [2. Runtime & PDF Processing Issues](#2-runtime--pdf-processing-issues)
  - [IllegalStateException: Already has open page (PdfRenderer)](#illegalstateexception-already-has-open-page-pdfrenderer)
  - [SecurityException: Permission Denial on Recent Files](#securityexception-permission-denial-on-recent-files)
  - [OutOfMemoryError (OOM) on Large PDF Rendering](#outofmemoryerror-oom-on-large-pdf-rendering)
  - [Password-Protected PDF Fails to Open](#password-protected-pdf-fails-to-open)
- [3. Build & Resource Compilation Issues](#3-build--resource-compilation-issues)
  - [AAPT2 Error: Not a valid PNG file](#aapt2-error-not-a-valid-png-file)
  - [Java Version Mismatch (JVM Target Incompatibility)](#java-version-mismatch-jvm-target-incompatibility)
  - [Gradle Daemon Out of Memory](#gradle-daemon-out-of-memory)

---

## 1. Device Installation Issues

### INSTALL_FAILED_USER_RESTRICTED (Xiaomi / Redmi / HyperOS / MIUI)

**Symptom:**
Running `adb install` or launching from Android Studio fails with:
```text
Failure [INSTALL_FAILED_USER_RESTRICTED: Install canceled by user]
```

**Cause:**
Xiaomi HyperOS and MIUI enforce a security restriction requiring explicit user consent for sideloading apps via USB.

**Resolution:**
1. Open **Settings** on your Xiaomi / Redmi device.
2. Navigate to **Additional Settings** > **Developer options**.
3. Scroll down to the **Debugging** section.
4. Toggle **Install via USB** to **ON**.
   * *Note:* Xiaomi may require a SIM card to be inserted and an active Mi Account login to enable this setting.
5. Also toggle **USB debugging (Security settings)** to **ON** if prompted.
6. When deploying via ADB, keep your device screen unlocked and tap **Install** on the prompt dialog.

---

### Device Unauthorized (`adb devices` shows `unauthorized`)

**Symptom:**
```text
$ adb devices
List of devices attached
7a69ce42    unauthorized
```

**Resolution:**
1. Disconnect and reconnect the USB cable.
2. Check your phone screen for the prompt: *"Allow USB debugging?"*.
3. Check the box **"Always allow from this computer"** and tap **Allow**.
4. If the prompt does not appear, restart the ADB daemon:
   ```bash
   adb kill-server
   adb start-server
   adb devices
   ```

---

## 2. Runtime & PDF Processing Issues

### IllegalStateException: Already has open page (PdfRenderer)

**Symptom:**
Rapid scrolling in `ViewerFragment` crashes with:
```text
java.lang.IllegalStateException: Already has open page
    at android.graphics.pdf.PdfRenderer.openPage(PdfRenderer.java)
    at com.pdftoolbox.app.ui.viewer.ViewerFragment$PdfPageAdapter.onBindViewHolder(...)
```

**Cause:**
Android's native `android.graphics.pdf.PdfRenderer` is explicitly **non-thread-safe**. It allows only one `PdfRenderer.Page` instance to be open at any given moment across all coroutines or threads. Concurrent calls from fast RecyclerView recycling crash the process.

**Resolution:**
All page access, rendering, and closing must be serialized using a coroutine `Mutex`:
```kotlin
private val renderMutex = Mutex()

private suspend fun renderPage(pageIndex: Int): Bitmap? = renderMutex.withLock {
    var page: PdfRenderer.Page? = null
    try {
        page = renderer?.openPage(pageIndex)
        // render bitmap
    } finally {
        page?.close()
    }
}
```

---

### SecurityException: Permission Denial on Recent Files

**Symptom:**
Tapping an item in Recent Documents after app restart or phone reboot crashes with:
```text
java.lang.SecurityException: Permission Denial: opening provider com.android.providers.media.MediaDocumentsProvider ... requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs
```

**Cause:**
By default, URI access permissions granted via `ACTION_OPEN_DOCUMENT` are transient and expire when the application process terminates.

**Resolution:**
Persistable URI permissions must be explicitly requested immediately when the URI is received:
```kotlin
context.contentResolver.takePersistableUriPermission(
    uri,
    Intent.FLAG_GRANT_READ_URI_PERMISSION
)
```
Ensure your code wraps access in `try-catch (e: SecurityException)` and gracefully notifies the user if the document was deleted or moved by external apps.

---

### OutOfMemoryError (OOM) on Large PDF Rendering

**Symptom:**
Loading high-resolution documents or PDFs with hundreds of pages causes `java.lang.OutOfMemoryError: Failed to allocate a bitmap`.

**Cause:**
Holding uncompressed 32-bit ARGB bitmaps in memory indefinitely exhausts the Android Dalvik/ART heap.

**Resolution:**
1. Dynamic LRU Cache capped to a safe percentage of device RAM:
   ```kotlin
   val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
   val cacheSize = minOf(maxMemory / 32, 25 * 1024) // Cap at 25 MB
   val pageCache = object : LruCache<Int, Bitmap>(cacheSize) {
       override fun sizeOf(key: Int, bitmap: Bitmap): Int = bitmap.byteCount / 1024
       override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
           if (evicted) oldValue.recycle()
       }
   }
   ```
2. Clamp target render dimensions to screen density rather than raw PDF physical points if pages are excessively large.

---

### Password-Protected PDF Fails to Open

**Symptom:**
Opening an encrypted PDF throws:
```text
java.lang.SecurityException: PDF is encrypted
```

**Cause:**
Native Android `PdfRenderer` does not support password entry or decrypting encrypted PDFs directly.

**Resolution:**
1. Detect encryption before opening `PdfRenderer` using Apache PDFBox:
   ```kotlin
   val document = PDDocument.load(inputStream)
   if (document.isEncrypted) {
       // Prompt user for password or route to Unlock tool
   }
   ```
2. Guide the user to the **Unlock PDF** tool (`R.id.navigation_unlock`) to decrypt the document with their password.

---

## 3. Build & Resource Compilation Issues

### AAPT2 Error: Not a valid PNG file

**Symptom:**
Gradle build fails during `processDebugResources` with:
```text
AAPT: error: failed to read PNG file: Not a valid PNG file
```

**Cause:**
An image file (such as `ic_launcher.png`) was originally saved as a JPEG or WebP but renamed to `.png` without re-encoding. AAPT2 validates chunk headers and rejects mislabeled file formats.

**Resolution:**
Re-encode the file to valid PNG format using ImageMagick or Python:
```python
from PIL import Image
img = Image.open("res/drawable/ic_launcher.png")
img.save("res/drawable/ic_launcher.png", "PNG")
```

---

### Java Version Mismatch (JVM Target Incompatibility)

**Symptom:**
```text
Execution failed for task ':app:compileDebugKotlin'.
> 'compileDebugJavaWithJavac' task (current target is 17) and 'compileDebugKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version.
```

**Resolution:**
Ensure `compileOptions` and `kotlinOptions` in `app/build.gradle.kts` are both aligned to **Java 17**:
```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
```

---

### Gradle Daemon Out of Memory

**Symptom:**
Gradle build hangs or fails with `OutOfMemoryError: Metaspace` or `Java heap space`.

**Resolution:**
Adjust memory allocation in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC
org.gradle.parallel=true
org.gradle.caching=true
```
