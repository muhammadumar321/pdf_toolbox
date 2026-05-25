package com.pdftoolbox.app.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FileRepository(private val context: Context) {

    suspend fun copyFileToCache(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.let { input ->
                val fileName = getFileName(uri) ?: "temp_pdf_${System.currentTimeMillis()}.pdf"
                val file = File(context.cacheDir, fileName)
                val outputStream = FileOutputStream(file)
                
                input.use { i ->
                    outputStream.use { o ->
                        i.copyTo(o)
                    }
                }
                return@withContext file
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun getFileName(uri: Uri): String? {
        // Simplified for now, real implementation should query MediaStore/ContentResolver
        return uri.lastPathSegment
    }
}
