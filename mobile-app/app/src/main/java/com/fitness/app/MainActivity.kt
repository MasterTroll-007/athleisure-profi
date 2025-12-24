package com.fitness.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fitness.app.data.local.LocalePreferences
import com.fitness.app.data.local.PreferencesManager
import com.fitness.app.ui.navigation.FitnessNavHost
import com.fitness.app.ui.theme.FitnessTheme
import com.fitness.app.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun attachBaseContext(newBase: Context) {
        val selectedLocale = LocalePreferences.getSelectedLocale(newBase)
        val wrappedContext = LocaleHelper.wrapContext(newBase, selectedLocale)
        super.attachBaseContext(wrappedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitnessTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FitnessNavHost(preferencesManager = preferencesManager)
                }
            }
        }
    }
}
