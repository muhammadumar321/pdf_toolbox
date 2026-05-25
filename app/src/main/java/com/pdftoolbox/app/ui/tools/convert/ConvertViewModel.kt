package com.pdftoolbox.app.ui.tools.convert

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.repository.PdfToolsRepository
import kotlinx.coroutines.launch

class ConvertViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfToolsRepository(application)

    private val _selectedImages = MutableLiveData<List<Uri>>(emptyList())
    val selectedImages: LiveData<List<Uri>> = _selectedImages

    private val _convertState = MutableLiveData<Result<Boolean>>()
    val convertState: LiveData<Result<Boolean>> = _convertState

    fun addImages(uris: List<Uri>) {
        val currentList = _selectedImages.value.orEmpty().toMutableList()
        currentList.addAll(uris)
        _selectedImages.value = currentList
    }

    fun convertToPdf(outputUri: Uri) {
        val images = _selectedImages.value
        if (images.isNullOrEmpty()) return

        viewModelScope.launch {
            try {
                val success = repository.imagesToPdf(images, outputUri)
                if (success) {
                    _convertState.value = Result.success(true)
                } else {
                    _convertState.value = Result.failure(Exception("Failed to convert images"))
                }
            } catch (e: Exception) {
                _convertState.value = Result.failure(e)
            }
        }
    }
}
