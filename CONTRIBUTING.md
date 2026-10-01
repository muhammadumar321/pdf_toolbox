# Contributing to PDF Toolbox

Thank you for your interest in contributing to **PDF Toolbox**! We welcome bug reports, feature proposals, and pull requests.

---

## Code of Conduct

Please be respectful, collaborative, and considerate of all contributors and users.

---

## Branching & Workflow Policy

We follow a strict branching model:

```text
feature / bugfix branch
          ↓
        `dev` (active development & QA)
          ↓
        `main` (production release)
```

1. **Active Development Branch:** All ongoing work, bug fixes, UI improvements, and feature developments must be submitted to and branched from **`dev`**.
2. **Never Commit Directly to `main`:** `main` is reserved exclusively for validated, stable production releases.
3. **Branch Naming Conventions:**
   - Features: `feature/<feature-name>`
   - Bug fixes: `fix/<bug-name>`
   - Documentation: `docs/<topic>`
   - Refactoring: `refactor/<component>`

---

## Coding Standards

* **Language:** Kotlin 2.0+ following the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
* **Architecture:** Adhere strictly to the MVVM layered architecture:
  - Keep Views (`Fragment`, `Activity`) dumb — handle UI and lifecycle only.
  - Put presentation state and user intents in `ViewModel`.
  - Perform all IO, PDF operations, and disk access in repositories with Coroutine dispatchers (`Dispatchers.IO`).
* **Resource Access:** Always access storage via Android's Storage Access Framework (SAF) and `ContentResolver`. Do not assume direct POSIX filesystem paths.
* **Thread Safety:** Ensure calls to Android's `PdfRenderer` are synchronized via mutexes or single-threaded execution.

---

## Pull Request Guidelines

Before submitting a Pull Request:
1. Ensure the code compiles cleanly:
   ```bash
   ./gradlew assembleDebug
   ```
2. Run all unit tests:
   ```bash
   ./gradlew test
   ```
3. Run lint and static analysis:
   ```bash
   ./gradlew check
   ```
4. Verify there are no merge conflicts with `dev`.
5. Write clear, conventional commit messages:
   - `feat: add PDF encryption support`
   - `fix: resolve page renderer concurrency crash`
   - `docs: update setup and architecture guide`

---

## Reporting Issues

When reporting an issue, please include:
* Android OS version and device model.
* Detailed steps to reproduce the problem.
* Expected vs. actual behavior.
* Stack traces or logcat output if a crash occurred.
