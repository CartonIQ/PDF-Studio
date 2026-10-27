package com.pdfstudio.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PDFStudioApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox for Android runtime fonts and resource assets
        PDFBoxResourceLoader.init(applicationContext)

        // Initialize Google Mobile Ads SDK asynchronously in background
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@PDFStudioApplication) {}
        }
    }
}
