package com.pdftoolbox.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PdfToolboxApp : Application() {
    companion object {
        lateinit var instance: PdfToolboxApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize PDFBox
        PDFBoxResourceLoader.init(applicationContext)
    }
}
