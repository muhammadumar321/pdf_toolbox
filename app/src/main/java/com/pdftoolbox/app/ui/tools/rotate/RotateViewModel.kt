package com.pdftoolbox.app.ui.tools.rotate

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.repository.PdfToolsRepository
import kotlinx.coroutines.launch

class RotateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfToolsRepository(application)

    private val _selectedFile = MutableLiveData<Uri?>()
    val selectedFile: LiveData<Uri?> = _selectedFile

    private val _rotateState = MutableLiveData<Result<Boolean>>()
    val rotateState: LiveData<Result<Boolean>> = _rotateState

    fun selectFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun rotatePdf(angle: Int, outputUri: Uri) {
        val file = _selectedFile.value ?: return
        
        viewModelScope.launch {
            try {
                val success = repository.rotatePdf(file, angle, outputUri)
                if (success) {
                    _rotateState.value = Result.success(true)
                } else {
                    _rotateState.value = Result.failure(Exception("Failed to rotate PDF"))
                }
            } catch (e: Exception) {
                _rotateState.value = Result.failure(e)
            }
        }
    }
}
