package com.pdftoolbox.app.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.Manifest

object PermissionUtils {
    
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses scoped storage, usually doesn't need READ_EXTERNAL_STORAGE for picking files via system picker
            // But if we want to manage files, we might need MANAGE_EXTERNAL_STORAGE (special permission)
            // For this app, we primarily rely on SAF (Storage Access Framework)
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
