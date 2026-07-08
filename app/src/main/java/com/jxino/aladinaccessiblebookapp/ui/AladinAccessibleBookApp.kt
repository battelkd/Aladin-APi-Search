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
        when (screen) {
            AppScreen.VoiceSearch -> VoiceSearchScreen(
                uiState = uiState,
                hasAudioPermission = hasAudioPermission,
                shouldOpenAppSettingsForAudio = shouldOpenAppSettingsForAudio,
                onRequestPermission = onRequestPermission,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onResultClicked = onResultClicked,
            )
            is AppScreen.WebView -> WebViewScreen(
                url = screen.url,
                title = screen.title,
                onBackToSearch = onBackToSearch,
                onLoadingChanged = onWebViewLoadingChanged,
            )
        }
    }
}
