package com.mgradio.app

import android.app.UiModeManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mgradio.app.domain.model.PlaybackStatus
import com.mgradio.app.presentation.main.MainScreen
import com.mgradio.app.presentation.main.MainViewModel
import com.mgradio.app.presentation.theme.MgRadioTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            val isTv = isTelevisionDevice()
            val keepScreenOn = isTv && (
                state.playerState.status == PlaybackStatus.PLAYING ||
                    state.playerState.status == PlaybackStatus.BUFFERING ||
                    state.playerState.status == PlaybackStatus.RECONNECTING
                )

            LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            MgRadioTheme(
                themeMode = state.preferences.themeMode,
                useDynamicColor = state.preferences.useDynamicColor
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun isTelevisionDevice(): Boolean {
        val uiModeManager = getSystemService(UiModeManager::class.java)
        val isTvUiMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        return isTvUiMode || hasLeanback
    }
}
