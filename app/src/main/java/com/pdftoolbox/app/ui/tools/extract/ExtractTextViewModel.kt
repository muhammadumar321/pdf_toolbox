package com.pdftoolbox.app.ui.tools.extract

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.repository.PdfToolsRepository
import kotlinx.coroutines.launch

class ExtractTextViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfToolsRepository(application)

    private val _selectedFile = MutableLiveData<Uri?>()
    val selectedFile: LiveData<Uri?> = _selectedFile

    private val _extractedText = MutableLiveData<String>()
    val extractedText: LiveData<String> = _extractedText

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun selectFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun extractText() {
        val file = _selectedFile.value ?: return
        
        _loading.value = true
        viewModelScope.launch {
            try {
                val text = repository.extractText(file)
                _extractedText.value = text
            } catch (e: Exception) {
                _extractedText.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
