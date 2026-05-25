package com.pdftoolbox.app.ui.settings

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdftoolbox.app.data.repository.RecentFilesRepository
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecentFilesRepository(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)
    
    private val _clearRecentFilesEvent = MutableLiveData<Boolean>()
    val clearRecentFilesEvent: LiveData<Boolean> = _clearRecentFilesEvent

    private val _themeMode = MutableLiveData<Int>()
    val themeMode: LiveData<Int> = _themeMode

    init {
        _themeMode.value = prefs.getInt(KEY_THEME_MODE, DEFAULT_THEME_MODE)
    }

    fun clearRecentFiles() {
        viewModelScope.launch {
            repository.clearRecentFiles()
            _clearRecentFilesEvent.value = true
        }
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val DEFAULT_THEME_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
