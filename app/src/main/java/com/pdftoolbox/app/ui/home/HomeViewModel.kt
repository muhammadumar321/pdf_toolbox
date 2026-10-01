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
            val name = com.pdftoolbox.app.utils.FileUtils.getDisplayName(getApplication(), uri)
            repository.addRecentFile(uri.toString(), name)
            loadRecentFiles()
        }
    }
}
