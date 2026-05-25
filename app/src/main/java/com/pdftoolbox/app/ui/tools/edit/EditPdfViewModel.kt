package com.pdftoolbox.app.ui.tools.edit

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.repository.PdfToolsRepository
import kotlinx.coroutines.launch

class EditPdfViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PdfToolsRepository(application)

    private val _selectedFile = MutableLiveData<Uri?>()
    val selectedFile: LiveData<Uri?> = _selectedFile

    private val _editState = MutableLiveData<Result<Unit>>()
    val editState: LiveData<Result<Unit>> = _editState

    fun selectFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun applyEdits(overlayBitmap: Bitmap, outputUri: Uri) {
        val uri = _selectedFile.value ?: return
        viewModelScope.launch {
            val success = repository.addBitmapOverlayToPdf(uri, overlayBitmap, 0, outputUri)
            if (success) {
                _editState.value = Result.success(Unit)
            } else {
                _editState.value = Result.failure(Exception("Failed to save edited PDF"))
            }
        }
    }
}
