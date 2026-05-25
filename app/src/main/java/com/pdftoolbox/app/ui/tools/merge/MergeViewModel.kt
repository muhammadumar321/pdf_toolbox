package com.pdftoolbox.app.ui.tools.merge

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.repository.PdfToolsRepository
import kotlinx.coroutines.launch

class MergeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfToolsRepository(application)

    private val _selectedFiles = MutableLiveData<List<Uri>>(emptyList())
    val selectedFiles: LiveData<List<Uri>> = _selectedFiles

    private val _mergeState = MutableLiveData<Result<Boolean>>()
    val mergeState: LiveData<Result<Boolean>> = _mergeState

    fun addFiles(uris: List<Uri>) {
        val current = _selectedFiles.value.orEmpty()
        val combined = java.util.LinkedHashSet<Uri>()
        combined.addAll(current)
        combined.addAll(uris)
        _selectedFiles.value = combined.toList()
    }

    fun setFiles(uris: List<Uri>) {
        _selectedFiles.value = uris
    }

    fun removeFile(uri: Uri) {
        val currentList = _selectedFiles.value.orEmpty().toMutableList()
        currentList.remove(uri)
        _selectedFiles.value = currentList
    }

    fun swapFiles(fromPosition: Int, toPosition: Int) {
        val currentList = _selectedFiles.value.orEmpty().toMutableList()
        if (fromPosition in currentList.indices && toPosition in currentList.indices) {
            java.util.Collections.swap(currentList, fromPosition, toPosition)
            _selectedFiles.value = currentList
        }
    }

    fun mergeFiles(outputUri: Uri) {
        val files = _selectedFiles.value
        if (files.isNullOrEmpty() || files.size < 2) {
            _mergeState.value = Result.failure(Exception("Select at least 2 files"))
            return
        }

        viewModelScope.launch {
            try {
                val success = repository.mergePdfs(files, outputUri)
                if (success) {
                    _mergeState.value = Result.success(true)
                } else {
                    _mergeState.value = Result.failure(Exception("Merge failed"))
                }
            } catch (e: Exception) {
                _mergeState.value = Result.failure(e)
            }
        }
    }
}
