package com.secureehr.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.secureehr.app.data.local.TokenManager
import com.secureehr.app.navigation.AppNavigation
import com.secureehr.app.ui.theme.SecureEHRTheme
import kotlinx.coroutines.flow.first

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val tokenManager = remember { TokenManager(this) }
            var darkMode by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                tokenManager.darkMode.collect { darkMode = it }
            }

            // Restore persisted language on every app start as a fallback to AppCompat's auto-restore
            LaunchedEffect(Unit) {
                val savedLang = tokenManager.language.first()
                val localeTag = if (savedLang == "Hindi") "hi" else "en"
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
            }

            SecureEHRTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
