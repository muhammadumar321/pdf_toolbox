package com.pdftoolbox.app.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.models.RecentFile
import com.pdftoolbox.app.data.repository.RecentFilesRepository
import kotlinx.coroutines.launch
import android.provider.OpenableColumns

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecentFilesRepository(application)

    private val _recentFiles = MutableLiveData<List<RecentFile>>()
    val recentFiles: LiveData<List<RecentFile>> = _recentFiles

    init {
        loadRecentFiles()
    }

    fun loadRecentFiles() {
        viewModelScope.launch {
            _recentFiles.value = repository.getRecentFiles()
        }
    }

    fun addRecentFile(uri: Uri) {
        viewModelScope.launch {
            val name = getFileName(uri) ?: "Unknown.pdf"
            repository.addRecentFile(uri.toString(), name)
            loadRecentFiles()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
