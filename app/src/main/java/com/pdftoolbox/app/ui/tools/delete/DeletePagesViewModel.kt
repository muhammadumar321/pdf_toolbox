package com.pdftoolbox.app.ui.tools.delete

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.repository.PdfToolsRepository
import kotlinx.coroutines.launch

class DeletePagesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfToolsRepository(application)

    private val _selectedFile = MutableLiveData<Uri?>()
    val selectedFile: LiveData<Uri?> = _selectedFile

    private val _deleteState = MutableLiveData<Result<Boolean>>()
    val deleteState: LiveData<Result<Boolean>> = _deleteState

    fun selectFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun deletePages(pageRange: String, outputUri: Uri) {
        val file = _selectedFile.value ?: return
        
        viewModelScope.launch {
            try {
                val success = repository.deletePages(file, pageRange, outputUri)
                if (success) {
                    _deleteState.value = Result.success(true)
                } else {
                    _deleteState.value = Result.failure(Exception("Failed to delete pages"))
                }
            } catch (e: Exception) {
                _deleteState.value = Result.failure(e)
            }
        }
    }
}
