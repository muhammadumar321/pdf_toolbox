package com.pdftoolbox.app.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object FileUtils {

    /**
     * Resolves the user-visible display name of a given Uri.
     * Queries [OpenableColumns.DISPLAY_NAME] for content URIs, falling back to path parsing.
     */
    fun getDisplayName(context: Context?, uri: Uri?): String {
        if (uri == null) return "No file selected"
        
        var result: String? = null
        if (uri.scheme == "content" && context != null) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore and fall back to path parsing
            }
        }

        if (result.isNullOrBlank()) {
            val path = uri.path ?: uri.toString()
            val cut = path.lastIndexOf('/')
            result = if (cut != -1) path.substring(cut + 1) else path
        }

        return result?.takeIf { it.isNotBlank() } ?: "file.pdf"
    }

    /**
     * Parses a page range string (e.g., "1-3, 5, 7-9") into 0-based page indices.
     */
    fun parsePageRange(range: String, totalPages: Int): List<Int> {
        val result = mutableListOf<Int>()
        if (range.isBlank() || totalPages <= 0) return result

        try {
            val parts = range.split(",")
            for (part in parts) {
                val trimmed = part.trim()
                if (trimmed.contains("-")) {
                    val bounds = trimmed.split("-")
                    if (bounds.size == 2) {
                        val start = bounds[0].trim().toIntOrNull()
                        val end = bounds[1].trim().toIntOrNull()
                        if (start != null && end != null) {
                            val minVal = minOf(start, end)
                            val maxVal = maxOf(start, end)
                            for (i in minVal..maxVal) {
                                if (i in 1..totalPages) {
                                    result.add(i - 1)
                                }
                            }
                        }
                    }
                } else {
                    val page = trimmed.toIntOrNull()
                    if (page != null && page in 1..totalPages) {
                        result.add(page - 1)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result.sorted().distinct()
    }

    /**
     * Parses a page order string (e.g., "3, 1, 2") into 0-based page indices.
     */
    fun parsePageOrder(order: String, totalPages: Int): List<Int> {
        if (order.isBlank() || totalPages <= 0) return emptyList()
        return try {
            order.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .map { it - 1 }
                .filter { it in 0 until totalPages }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
