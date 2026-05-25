package com.pdftoolbox.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.pdftoolbox.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var updating = false

        binding.toggleTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || updating) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                binding.btnThemeLight.id -> AppCompatDelegate.MODE_NIGHT_NO
                binding.btnThemeDark.id -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            viewModel.setThemeMode(mode)
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        binding.btnClearRecent.setOnClickListener {
            viewModel.clearRecentFiles()
        }

        viewModel.clearRecentFilesEvent.observe(viewLifecycleOwner) { cleared ->
            if (cleared) {
                Toast.makeText(context, "Recent files cleared", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.themeMode.observe(viewLifecycleOwner) { mode ->
            val targetId = when (mode) {
                AppCompatDelegate.MODE_NIGHT_NO -> binding.btnThemeLight.id
                AppCompatDelegate.MODE_NIGHT_YES -> binding.btnThemeDark.id
                else -> binding.btnThemeSystem.id
            }
            if (binding.toggleTheme.checkedButtonId != targetId) {
                updating = true
                binding.toggleTheme.check(targetId)
                updating = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
