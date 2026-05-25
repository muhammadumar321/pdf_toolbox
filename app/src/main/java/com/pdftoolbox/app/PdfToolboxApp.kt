package com.pdftoolbox.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PdfToolboxApp : Application() {
    companion object {
        private var _instance: PdfToolboxApp? = null
        val instance: PdfToolboxApp
            get() = _instance ?: throw IllegalStateException(
                "PdfToolboxApp not initialized. Application.onCreate() may not have been called."
            )
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this
        PDFBoxResourceLoader.init(applicationContext)
    }
}
