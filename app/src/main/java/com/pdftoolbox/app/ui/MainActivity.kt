package com.pdftoolbox.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.pdftoolbox.app.R
import com.pdftoolbox.app.databinding.ActivityMainBinding
import com.pdftoolbox.app.ui.settings.SettingsViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(
            SettingsViewModel.PREFS_NAME,
            MODE_PRIVATE
        )
        val mode = prefs.getInt(
            SettingsViewModel.KEY_THEME_MODE,
            SettingsViewModel.DEFAULT_THEME_MODE
        )
        AppCompatDelegate.setDefaultNightMode(mode)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        val toolDestinations = setOf(
            R.id.navigation_merge,
            R.id.navigation_split,
            R.id.navigation_compress,
            R.id.navigation_convert,
            R.id.navigation_rotate,
            R.id.navigation_watermark,
            R.id.navigation_delete_pages,
            R.id.navigation_extract_text,
            R.id.navigation_extract_images
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in toolDestinations) {
                binding.bottomNavigation.selectedItemId = R.id.navigation_tools
            }
        }

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data.toString()
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController

            val bundle = Bundle().apply {
                putString("fileUri", uri)
            }
            navController.navigate(R.id.navigation_viewer, bundle)
        }
    }
}