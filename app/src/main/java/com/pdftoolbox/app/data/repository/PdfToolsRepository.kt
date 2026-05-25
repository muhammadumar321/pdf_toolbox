package com.pdftoolbox.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix as androidMatrix
import android.net.Uri
import com.pdftoolbox.app.data.models.PdfMetadata
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class PdfToolsRepository(private val context: Context) {

    // ... existing methods ...
    suspend fun getPdfMetadata(uri: Uri): PdfMetadata? = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)
                document?.let { doc ->
                    val info = doc.documentInformation
                    return@withContext PdfMetadata(
                        title = info.title ?: "",
                        author = info.author ?: "",
                        subject = info.subject ?: "",
                        keywords = info.keywords ?: "",
                        pageCount = doc.numberOfPages,
                        version = doc.version
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document?.close()
        }
        return@withContext null
    }

    suspend fun mergePdfs(uris: List<Uri>, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val merger = PDFMergerUtility()
        val inputStreams = mutableListOf<InputStream>()
        try {
            uris.forEach { uri ->
                val stream = context.contentResolver.openInputStream(uri)
                if (stream != null) {
                    inputStreams.add(stream)
                    merger.addSource(stream)
                }
            }
            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                merger.destinationStream = output
                merger.mergeDocuments(null)
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            inputStreams.forEach { it.close() }
        }
    }

    suspend fun splitPdf(uri: Uri, pageRange: String, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        var newDocument: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)
                newDocument = PDDocument()
                val pages = parsePageRange(pageRange, document!!.numberOfPages)
                pages.forEach { pageIndex ->
                    if (pageIndex in 0 until document!!.numberOfPages) {
                        newDocument!!.addPage(document!!.getPage(pageIndex))
                    }
                }
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    newDocument!!.save(output)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document?.close()
            newDocument?.close()
        }
    }

    suspend fun addWatermark(uri: Uri, text: String, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)
                
                document!!.pages.forEach { page ->
                    val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
                    val font = PDType1Font.HELVETICA_BOLD
                    val fontSize = 30f
                    
                    contentStream.beginText()
                    contentStream.setFont(font, fontSize)
                    contentStream.setNonStrokingColor(200, 200, 200) // Light gray
                    
                    // Center the text
                    val mediaBox = page.mediaBox
                    val rotation = page.rotation
                    
                    val stringWidth = font.getStringWidth(text) / 1000f * fontSize
                    val x = (mediaBox.width - stringWidth) / 2
                    val y = mediaBox.height / 2
                    
                    if (rotation == 90 || rotation == 270) {
                        // Adjust for rotation if needed, but for simplicity we'll just center on mediaBox
                    }
                    
                    contentStream.newLineAtOffset(x, y) 
                    contentStream.showText(text)
                    contentStream.endText()
                    contentStream.close()
                }

                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    document!!.save(output)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document?.close()
        }
    }

    suspend fun addImageWatermark(uri: Uri, imageUri: Uri, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)

                val bitmap = context.contentResolver.openInputStream(imageUri)?.use { imageStream ->
                    BitmapFactory.decodeStream(imageStream)
                }

                if (bitmap != null) {
                    val imgWidth = bitmap.width.toFloat()
                    val imgHeight = bitmap.height.toFloat()
                    val imageXObject = LosslessFactory.createFromImage(document, bitmap)
                    bitmap.recycle()

                    val drawWidth = 200f
                    val drawHeight = if (imgWidth > 0f) (imgHeight / imgWidth) * drawWidth else drawWidth

                    document!!.pages.forEach { page ->
                         val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
                         contentStream.drawImage(imageXObject, 50f, 50f, drawWidth, drawHeight)
                         contentStream.close()
                    }
                }

                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    document!!.save(output)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document?.close()
        }
    }

    suspend fun addBitmapOverlayToPdf(uri: Uri, overlayBitmap: android.graphics.Bitmap, pageIndex: Int, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)
                
                if (pageIndex >= 0 && pageIndex < document!!.numberOfPages) {
                    val page = document!!.getPage(pageIndex)
                    val imageXObject = LosslessFactory.createFromImage(document, overlayBitmap)
                    
                    val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
                    
                    val pageWidth = page.mediaBox.width
                    val pageHeight = page.mediaBox.height
                    
                    contentStream.drawImage(imageXObject, 0f, 0f, pageWidth, pageHeight)
                    contentStream.close()
                }

                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    document!!.save(output)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document?.close()
        }
    }

    suspend fun rotatePdf(uri: Uri, angle: Int, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)
                document!!.pages.forEach { page ->
                    val currentRotation = page.rotation
                    page.rotation = (currentRotation + angle) % 360
                }
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    document!!.save(output)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document?.close()
        }
    }

    suspend fun imagesToPdf(imageUris: List<Uri>, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val document = PDDocument()
        try {
            imageUris.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val page = PDPage(PDRectangle(bitmap.width.toFloat(), bitmap.height.toFloat()))
                        document.addPage(page)

                        val contentStream = PDPageContentStream(document, page)
                        val image = LosslessFactory.createFromImage(document, bitmap)
                        contentStream.drawImage(image, 0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
                        contentStream.close()
                        bitmap.recycle()
                    }
                }
            }
            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                document.save(output)
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document.close()
        }
    }

    suspend fun compressPdf(uri: Uri, outputUri: Uri, quality: Int = 60): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)

                val doc = document!!
                for (pageIdx in 0 until doc.numberOfPages) {
                    val page = doc.getPage(pageIdx)
                    val resources = page.resources
                    val imageNames = resources.xObjectNames.filter { resources.isImageXObject(it) }
                    for (name in imageNames) {
                        try {
                            val image = resources.getXObject(name) as? com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
                            if (image != null) {
                                val originalBitmap = image.image
                                if (originalBitmap != null && originalBitmap.width > 0 && originalBitmap.height > 0) {
                                    val maxDim = 1200
                                    var newWidth = originalBitmap.width
                                    var newHeight = originalBitmap.height
                                    if (newWidth > maxDim || newHeight > maxDim) {
                                        val scale = minOf(maxDim.toFloat() / newWidth, maxDim.toFloat() / newHeight)
                                        newWidth = (newWidth * scale).toInt().coerceAtLeast(1)
                                        newHeight = (newHeight * scale).toInt().coerceAtLeast(1)
                                    }
                                    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                                    val compressedImage = JPEGFactory.createFromImage(doc, scaledBitmap, quality / 100f)
                                    resources.put(name, compressedImage)
                                    scaledBitmap.recycle()
                                    originalBitmap.recycle()
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }

                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    doc.save(output)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document?.close()
        }
    }

    suspend fun protectPdf(uri: Uri, password: String, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)
                
                val ap = AccessPermission()
                val spp = StandardProtectionPolicy(password, password, ap)
                spp.encryptionKeyLength = 128
                spp.permissions = ap
                
                document!!.protect(spp)
                
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    document!!.save(output)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document?.close()
        }
    }

    suspend fun unlockPdf(uri: Uri, password: String, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input, password)
                document!!.setAllSecurityToBeRemoved(true)
                
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    document!!.save(output)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document?.close()
        }
    }

    suspend fun addPageNumbers(uri: Uri, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)
                val totalPages = document!!.numberOfPages
                val font = PDType1Font.HELVETICA
                val fontSize = 12f

                for (i in 0 until totalPages) {
                    val page = document!!.getPage(i)
                    val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
                    
                    val text = "Page ${i + 1} of $totalPages"
                    val stringWidth = font.getStringWidth(text) / 1000f * fontSize
                    
                    contentStream.beginText()
                    contentStream.setFont(font, fontSize)
                    contentStream.setNonStrokingColor(100, 100, 100)
                    
                    val x = (page.mediaBox.width - stringWidth) / 2
                    val y = 20f // 20 units from bottom
                    
                    contentStream.newLineAtOffset(x, y)
                    contentStream.showText(text)
                    contentStream.endText()
                    contentStream.close()
                }

                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    document!!.save(output)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document?.close()
        }
    }

    suspend fun rearrangePages(uri: Uri, pageOrder: List<Int>, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        var newDocument: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)
                newDocument = PDDocument()
                
                pageOrder.forEach { index ->
                    if (index in 0 until document!!.numberOfPages) {
                        newDocument!!.addPage(document!!.getPage(index))
                    }
                }
                
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    newDocument!!.save(output)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document?.close()
            newDocument?.close()
        }
    }

    suspend fun deletePages(uri: Uri, pageRange: String, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        var newDocument: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)
                newDocument = PDDocument()
                val pagesToDelete = parsePageRange(pageRange, document!!.numberOfPages)
                
                for (i in 0 until document!!.numberOfPages) {
                    if (!pagesToDelete.contains(i)) {
                        newDocument!!.addPage(document!!.getPage(i))
                    }
                }
                
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    newDocument!!.save(output)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            document?.close()
            newDocument?.close()
        }
    }

    suspend fun extractText(uri: Uri): String = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)
                val stripper = PDFTextStripper()
                return@withContext stripper.getText(document)
            }
            return@withContext ""
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Error extracting text: ${e.message}"
        } finally {
            document?.close()
        }
    }

    suspend fun extractImages(uri: Uri, outputDirUri: Uri): Int = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        var count = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                document = PDDocument.load(input)
                val outputDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, outputDirUri)
                
                if (outputDir != null && outputDir.exists()) {
                    document!!.pages.forEachIndexed { pageIndex, page ->
                        val resources = page.resources
                        resources.xObjectNames.forEach { name ->
                            if (resources.isImageXObject(name)) {
                                val image = resources.getXObject(name) as com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
                                val bitmap = image.image

                                val fileName = "image_${pageIndex + 1}_$name.png"
                                val file = outputDir.createFile("image/png", fileName)
                                file?.let {
                                    context.contentResolver.openOutputStream(it.uri)?.use { out ->
                                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                        count++
                                    }
                                }
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
            return@withContext count
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext -1
        } finally {
            document?.close()
        }
    }

    private fun parsePageRange(range: String, totalPages: Int): List<Int> {
        val result = mutableListOf<Int>()
        try {
            val parts = range.split(",")
            for (part in parts) {
                if (part.contains("-")) {
                    val bounds = part.trim().split("-")
                    if (bounds.size == 2) {
                        val start = bounds[0].trim().toIntOrNull()
                        val end = bounds[1].trim().toIntOrNull()
                        if (start != null && end != null) {
                            for (i in start..end) {
                                result.add(i - 1)
                            }
                        }
                    }
                } else {
                    val page = part.trim().toIntOrNull()
                    if (page != null) {
                        result.add(page - 1)
                    }
                }
            }
        } catch (e: Exception) {
        }
        return result.sorted().distinct()
    }
}
