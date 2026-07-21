package com.jxino.aladinaccessiblebookapp.ui

import androidx.compose.runtime.Composable
import com.jxino.aladinaccessiblebookapp.domain.BookSearchResult

@Composable
fun AladinAccessibleBookApp(
    uiState: BookSearchUiState,
    screen: AppScreen,
    hasAudioPermission: Boolean,
    shouldOpenAppSettingsForAudio: Boolean,
    onRequestPermission: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onResultClicked: (BookSearchResult) -> Unit,
    onBackToSearch: () -> Unit,
    onWebViewLoadingChanged: (Boolean) -> Unit,
) {
    AppTheme {
        VoiceSearchScreen(
            uiState = uiState,
            screen = screen,
            hasAudioPermission = hasAudioPermission,
            shouldOpenAppSettingsForAudio = shouldOpenAppSettingsForAudio,
            onRequestPermission = onRequestPermission,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            onResultClicked = onResultClicked,
            onBackToSearch = onBackToSearch,
            onWebViewLoadingChanged = onWebViewLoadingChanged,
        )
    }
}
