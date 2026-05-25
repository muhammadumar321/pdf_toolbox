package com.pdftoolbox.app.ui.tools.compress

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.repository.PdfToolsRepository
import kotlinx.coroutines.launch

class CompressViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfToolsRepository(application)

    private val _selectedFile = MutableLiveData<Uri?>()
    val selectedFile: LiveData<Uri?> = _selectedFile

    private val _compressState = MutableLiveData<Result<Boolean>>()
    val compressState: LiveData<Result<Boolean>> = _compressState

    fun selectFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun compressPdf(outputUri: Uri) {
        val file = _selectedFile.value ?: return
        
        viewModelScope.launch {
            try {
                val success = repository.compressPdf(file, outputUri)
                if (success) {
                    _compressState.value = Result.success(true)
                } else {
                    _compressState.value = Result.failure(Exception("Failed to compress PDF"))
                }
            } catch (e: Exception) {
                _compressState.value = Result.failure(e)
            }
        }
    }
}
