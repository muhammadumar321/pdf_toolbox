package com.pdftoolbox.app

import com.pdftoolbox.app.data.models.PdfMetadata
import com.pdftoolbox.app.data.models.RecentFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ModelsTest {

    @Test
    fun recentFile_propertiesAndEquality() {
        val file1 = RecentFile("content://test/doc.pdf", "doc.pdf", 1000L)
        val file2 = RecentFile("content://test/doc.pdf", "doc.pdf", 1000L)
        val file3 = RecentFile("content://test/doc2.pdf", "doc2.pdf", 2000L)

        assertEquals("content://test/doc.pdf", file1.uri)
        assertEquals("doc.pdf", file1.name)
        assertEquals(1000L, file1.timestamp)

        assertEquals(file1, file2)
        assertNotEquals(file1, file3)
        assertEquals(file1.hashCode(), file2.hashCode())
    }

    @Test
    fun pdfMetadata_propertiesAndDefaults() {
        val metadata = PdfMetadata(
            title = "Annual Report",
            author = "Finance Dept",
            subject = "Financials",
            keywords = "annual, report, 2026",
            pageCount = 42,
            version = 1.7f
        )

        assertEquals("Annual Report", metadata.title)
        assertEquals("Finance Dept", metadata.author)
        assertEquals("Financials", metadata.subject)
        assertEquals("annual, report, 2026", metadata.keywords)
        assertEquals(42, metadata.pageCount)
        assertEquals(1.7f, metadata.version, 0.001f)
    }
}
