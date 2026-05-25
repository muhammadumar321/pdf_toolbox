package com.pdftoolbox.app.ui.tools.split

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.repository.PdfToolsRepository
import kotlinx.coroutines.launch
import java.io.File

class SplitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfToolsRepository(application)

    private val _selectedFile = MutableLiveData<Uri?>()
    val selectedFile: LiveData<Uri?> = _selectedFile

    private val _splitState = MutableLiveData<Result<Boolean>>()
    val splitState: LiveData<Result<Boolean>> = _splitState

    fun selectFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun splitPdf(range: String, outputUri: Uri) {
        val uri = _selectedFile.value ?: return
        if (range.isBlank()) {
            _splitState.value = Result.failure(Exception("Please enter page range"))
            return
        }

        viewModelScope.launch {
            try {
                val success = repository.splitPdf(uri, range, outputUri)
                if (success) {
                    _splitState.value = Result.success(true)
                } else {
                    _splitState.value = Result.failure(Exception("Split failed"))
                }
            } catch (e: Exception) {
                _splitState.value = Result.failure(e)
            }
        }
    }
}
