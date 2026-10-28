package com.abhinay.ISLEnglishGujaratiApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinay.ISLEnglishGujaratiApp.ui.AppViewModel
import com.abhinay.ISLEnglishGujaratiApp.ui.SanketSetuNav
import com.abhinay.ISLEnglishGujaratiApp.ui.theme.SanketSetuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // shown only while the process starts; no artificial delay
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            SanketSetuTheme(state.textScale, state.highContrast) { SanketSetuNav(vm) }
        }
    }
}
