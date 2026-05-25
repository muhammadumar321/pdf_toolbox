package com.pdftoolbox.app.ui.tools.watermark

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.repository.PdfToolsRepository
import kotlinx.coroutines.launch

class WatermarkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfToolsRepository(application)

    private val _selectedFile = MutableLiveData<Uri?>()
    val selectedFile: LiveData<Uri?> = _selectedFile

    private val _selectedImage = MutableLiveData<Uri?>()
    val selectedImage: LiveData<Uri?> = _selectedImage

    private val _watermarkState = MutableLiveData<Result<Boolean>>()
    val watermarkState: LiveData<Result<Boolean>> = _watermarkState

    fun selectFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun selectImage(uri: Uri) {
        _selectedImage.value = uri
    }

    fun applyWatermark(text: String, outputUri: Uri) {
        val file = _selectedFile.value ?: return
        
        viewModelScope.launch {
            try {
                val success = repository.addWatermark(file, text, outputUri)
                if (success) {
                    _watermarkState.value = Result.success(true)
                } else {
                    _watermarkState.value = Result.failure(Exception("Failed to apply watermark"))
                }
            } catch (e: Exception) {
                _watermarkState.value = Result.failure(e)
            }
        }
    }

    fun applyImageWatermark(outputUri: Uri) {
        val file = _selectedFile.value ?: return
        val image = _selectedImage.value ?: return

        viewModelScope.launch {
            try {
                val success = repository.addImageWatermark(file, image, outputUri)
                if (success) {
                    _watermarkState.value = Result.success(true)
                } else {
                    _watermarkState.value = Result.failure(Exception("Failed to apply watermark"))
                }
            } catch (e: Exception) {
                _watermarkState.value = Result.failure(e)
            }
        }
    }
}
