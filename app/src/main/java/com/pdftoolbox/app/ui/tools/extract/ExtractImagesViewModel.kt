package com.pdftoolbox.app.ui.tools.extract

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.repository.PdfToolsRepository
import kotlinx.coroutines.launch

class ExtractImagesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfToolsRepository(application)
    
    private val _extractionStatus = MutableLiveData<ExtractionStatus>()
    val extractionStatus: LiveData<ExtractionStatus> = _extractionStatus

    sealed class ExtractionStatus {
        object Idle : ExtractionStatus()
        object Loading : ExtractionStatus()
        data class Success(val count: Int) : ExtractionStatus()
        data class Error(val message: String) : ExtractionStatus()
    }

    fun extractImages(uri: Uri, outputDir: Uri) {
        _extractionStatus.value = ExtractionStatus.Loading
        viewModelScope.launch {
            val count = repository.extractImages(uri, outputDir)
            if (count >= 0) {
                _extractionStatus.value = ExtractionStatus.Success(count)
            } else {
                _extractionStatus.value = ExtractionStatus.Error("Failed to extract images")
            }
        }
    }
}
