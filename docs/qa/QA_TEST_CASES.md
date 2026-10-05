# QA Test Cases

| Test Case ID | Feature ID | Test Title | Priority | Preconditions | Steps | Expected Result |
|---|---|---|---|---|---|---|
| TC-001 | FEAT-001 | View multi-page PDF | P0 | A PDF file exists on device | 1. Open app 2. Tap FAB or select from Recents 3. Select PDF | PDF is rendered correctly, scrolling works |
| TC-002 | FEAT-003 | Merge two PDFs | P1 | Two PDF files exist | 1. Go to Tools > Merge 2. Select two PDFs 3. Tap Merge 4. Save file | A new PDF is generated containing pages from both |
| TC-003 | FEAT-004 | Split PDF by range | P1 | A PDF with >3 pages exists | 1. Go to Tools > Split 2. Select PDF 3. Enter range "1-2" 4. Tap Split | Output PDF has only pages 1 and 2 |
| TC-004 | FEAT-013 | Protect PDF with Password | P0 | A PDF exists | 1. Go to Tools > Protect 2. Select PDF 3. Enter password 4. Save | Output PDF requires the given password to open |
| TC-005 | FEAT-014 | Unlock PDF | P1 | An encrypted PDF exists | 1. Go to Tools > Unlock 2. Select PDF 3. Enter correct password 4. Save | Output PDF can be opened without password |
| TC-006 | FEAT-002 | Recent Files persist | P2 | Open a PDF | 1. Open PDF 2. Close app 3. Reopen app | PDF appears in Recent list and can be opened |
| TC-007 | FEAT-017 | Change Theme | P3 | App is running | 1. Go to Settings 2. Change Theme to Dark | App immediately applies Dark mode |
| TC-008 | FEAT-010 | Extract Text | P2 | A PDF with text exists | 1. Go to Tools > Extract Text 2. Select PDF | Text is extracted and can be copied |
