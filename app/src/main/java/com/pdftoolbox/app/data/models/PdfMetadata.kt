package com.pdftoolbox.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PdfMetadata(
    val title: String,
    val author: String,
    val subject: String,
    val keywords: String,
    val pageCount: Int,
    val version: Float
) : Parcelable
