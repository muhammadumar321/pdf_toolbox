# Documentation Changelog

## v1.0.1 (Current)
- **Date**: 2026-10-05
- **Features documented or changed**: Documented 17 core features (Viewer, Merge, Split, Compress, Convert, Rotate, Watermark, Delete Pages, Extract Text, Extract Images, Edit & Annotate, Protect, Unlock, Page Numbers, Rearrange, Themes, Recent Files).
- **Test cases added or updated**: Created 8 core end-to-end P0-P3 scenarios.
- **Important corrections**: N/A (Initial creation).
- **Remaining documentation gaps**: None identified. Core offline workflow is stable.

### How to Regenerate the PDF Manual
To regenerate the `APPLICATION_FUNCTIONAL_SPECIFICATION_AND_QA_TEST_MANUAL.pdf` from its Markdown source, you can use Node.js and a markdown-to-pdf conversion script.

1. Ensure you have Node.js installed.
2. Initialize and install `marked`:
   ```bash
   npm init -y
   npm install marked
   ```
3. Create a short script (e.g., `gen_html.js`) that uses `marked` to convert `docs/qa/APPLICATION_FUNCTIONAL_SPECIFICATION_AND_QA_TEST_MANUAL.md` to HTML.
4. Convert the HTML to a PDF using a Chromium-based browser (like Edge or Chrome) in headless mode:
   ```powershell
   Start-Process -FilePath "msedge" -ArgumentList "--headless", "--print-to-pdf=""docs\qa\APPLICATION_FUNCTIONAL_SPECIFICATION_AND_QA_TEST_MANUAL.pdf""", """file:///$(Get-Location)/docs/qa/manual.html""" -Wait
   ```
5. Do not commit `node_modules` or `package.json` to the Android repository. Ensure you delete them after PDF generation.
