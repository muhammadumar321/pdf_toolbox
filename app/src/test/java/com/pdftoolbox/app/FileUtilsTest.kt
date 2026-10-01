package com.pdftoolbox.app

import com.pdftoolbox.app.utils.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUtilsTest {

    @Test
    fun parsePageRange_validCommaSeparatedAndRanges() {
        val totalPages = 10
        val result = FileUtils.parsePageRange("1-3, 5, 7-8", totalPages)
        // 0-indexed: 1->0, 2->1, 3->2, 5->4, 7->6, 8->7
        assertEquals(listOf(0, 1, 2, 4, 6, 7), result)
    }

    @Test
    fun parsePageRange_reversedRangeBounds() {
        val totalPages = 10
        val result = FileUtils.parsePageRange("4-2", totalPages)
        // Reversed range 4-2 should normalize to 2, 3, 4 -> 0-indexed: 1, 2, 3
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun parsePageRange_filtersOutOfBounds() {
        val totalPages = 5
        val result = FileUtils.parsePageRange("1, 3, 7, 10-12", totalPages)
        // 7, 10, 11, 12 exceed totalPages 5
        assertEquals(listOf(0, 2), result)
    }

    @Test
    fun parsePageRange_emptyOrInvalidInputReturnsEmpty() {
        assertTrue(FileUtils.parsePageRange("", 10).isEmpty())
        assertTrue(FileUtils.parsePageRange("   ", 10).isEmpty())
        assertTrue(FileUtils.parsePageRange("abc, foo-bar", 10).isEmpty())
        assertTrue(FileUtils.parsePageRange("1-3", 0).isEmpty())
    }

    @Test
    fun parsePageRange_deduplicatesPages() {
        val totalPages = 5
        val result = FileUtils.parsePageRange("1, 1, 1-2, 2", totalPages)
        assertEquals(listOf(0, 1), result)
    }

    @Test
    fun parsePageOrder_validOrder() {
        val totalPages = 5
        val result = FileUtils.parsePageOrder("3, 1, 2, 5, 4", totalPages)
        // 0-indexed: 3->2, 1->0, 2->1, 5->4, 4->3
        assertEquals(listOf(2, 0, 1, 4, 3), result)
    }

    @Test
    fun parsePageOrder_filtersOutOfBoundsAndInvalid() {
        val totalPages = 3
        val result = FileUtils.parsePageOrder("1, 5, 2, abc, 3", totalPages)
        assertEquals(listOf(0, 1, 2), result)
    }

    @Test
    fun parsePageOrder_emptyOrInvalid() {
        assertTrue(FileUtils.parsePageOrder("", 5).isEmpty())
        assertTrue(FileUtils.parsePageOrder("invalid", 5).isEmpty())
        assertTrue(FileUtils.parsePageOrder("1, 2", 0).isEmpty())
    }

    @Test
    fun getDisplayName_nullUriReturnsDefault() {
        val name = FileUtils.getDisplayName(null, null)
        assertEquals("No file selected", name)
    }
}
