package com.wault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wault.app.data.ThemeController
import com.wault.app.ui.nav.WaultNavHost
import com.wault.app.ui.theme.WaultTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeController: ThemeController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme by themeController.darkTheme.collectAsStateWithLifecycle()
            WaultTheme(darkTheme = darkTheme) {
                WaultNavHost()
            }
        }
    }
}
